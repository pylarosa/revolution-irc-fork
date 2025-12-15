package io.mrarm.irc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.connection.ServerConnectionFactory;
import io.mrarm.irc.connection.ServerConnectionRegistry;
import io.mrarm.irc.infrastructure.threading.DelayScheduler;
import io.mrarm.irc.infrastructure.threading.ManagedCoroutineScope;
import io.mrarm.irc.infrastructure.threading.SchedulerProvider;
import io.mrarm.irc.infrastructure.threading.SchedulerProviderHolder;
import io.mrarm.irc.setting.ReconnectIntervalSetting;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;

// TODO(architecture):
// ServerConnectionManager currently acts as:
// - connection registry
// - connection factory
// - reconnect policy holder
// - service lifecycle coordinator
// These responsibilities should be separated gradually.
public class ServerConnectionManager {

    private static ServerConnectionManager instance;

    private final Context mContext;
    private final HashMap<UUID, ServerConnectionInfo> mConnectionsMap = new HashMap<>();
    private final ArrayList<ServerConnectionInfo> mConnections = new ArrayList<>();
    private final HashMap<UUID, ServerConnectionInfo> mDisconnectingConnections = new HashMap<>();
    private final List<ConnectionsListener> mListeners = new ArrayList<>();
    private final List<ServerConnectionInfo.ChannelListChangeListener> mChannelsListeners = new ArrayList<>();
    private final List<ServerConnectionInfo.InfoChangeListener> mInfoListeners = new ArrayList<>();
    private boolean mDestroying = false;
    private final ManagedCoroutineScope mIoScopeWrapper;
    private final CoroutineScope mIoScope;
    private final ServerConnectionFactory connectionFactory;
    public final ServerConnectionRegistry mConnectionRegistry;


    public static boolean hasInstance() {
        return instance != null;
    }

    public static synchronized ServerConnectionManager getInstance(Context context) {
        if (instance == null && context != null)
            instance = new ServerConnectionManager(context.getApplicationContext());
        return instance;
    }

    public static synchronized void destroyInstance() {
        if (instance == null)
            return;
        instance.mDestroying = true;
        while (!instance.mConnections.isEmpty()) {
            ServerConnectionInfo connection = instance.mConnections.get(instance.mConnections.size() - 1);
            connection.disconnect();
            instance.removeConnection(connection, false);
            instance.killDisconnectingConnection(connection.getUUID());
        }
        instance.mIoScopeWrapper.cancel();
        instance = null;
    }

    public ServerConnectionManager(Context context) {
        this(context, null);
    }

    public ServerConnectionManager(Context context, SchedulerProvider schedulerProvider) {
        mContext = context;
        SchedulerProvider resolvedProvider = schedulerProvider != null ? schedulerProvider : SchedulerProviderHolder.get();
        mIoScopeWrapper = new ManagedCoroutineScope(resolvedProvider.getIoDispatcher());
        mIoScope = mIoScopeWrapper.getScope();
        DelayScheduler mReconnectScheduler = resolvedProvider.getReconnectionScheduler();
        connectionFactory = new ServerConnectionFactory(mContext, mReconnectScheduler);
        mConnectionRegistry = new ServerConnectionRegistry(mContext);

        List<ServerConnectionRegistry.ConnectedServerEntry> entries = mConnectionRegistry.loadConnectedServers();
        ServerConfigManager configManager = ServerConfigManager.getInstance(context);

        for (ServerConnectionRegistry.ConnectedServerEntry entry : entries) {
            ServerConfigData configData = configManager.findServer(entry.uuid);
            if (configData != null) {
                try {
                    createConnection(configData, entry.channels, false);
                } catch (NickNotSetException ignored) {
                }
            }
        }
        Log.i("[REGISTRY]", "Restored " + entries.size() + " connections");
    }

    void saveAutoconnectListAsync() {
        BuildersKt.launch(mIoScope, EmptyCoroutineContext.INSTANCE, CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            saveRegistry();
            return Unit.INSTANCE;
        });
    }

    private void saveRegistry() {
        List<ServerConnectionRegistry.ConnectedServerEntry> entries = new ArrayList<>();
        for (ServerConnectionInfo connection : getConnections()) {
            ServerConnectionRegistry.ConnectedServerEntry e = new ServerConnectionRegistry.ConnectedServerEntry();
            e.uuid = connection.getUUID();
            e.channels = connection.getChannels();
            entries.add(e);
        }
        mConnectionRegistry.saveConnectedServers(entries);
    }

    public Context getContext() {
        return mContext;
    }

    public List<ServerConnectionInfo> getConnections() {
        synchronized (this) {
            return new ArrayList<>(mConnections);
        }
    }

    public void addConnection(ServerConnectionInfo connection, boolean saveAutoconnect) {
        synchronized (this) {
            if (mConnectionsMap.containsKey(connection.getUUID()))
                throw new RuntimeException("A connection with this UUID already exists");
            mConnectionsMap.put(connection.getUUID(), connection);
            mConnections.add(connection);
            if (saveAutoconnect)
                saveAutoconnectListAsync();
        }
        synchronized (mListeners) {
            for (ConnectionsListener listener : mListeners)
                listener.onConnectionAdded(connection);
        }
        IRCService.start(mContext);
    }

    public void addConnection(ServerConnectionInfo connection) {
        addConnection(connection, true);
    }

    private ServerConnectionInfo createConnection(ServerConfigData data,
                                                  List<String> joinChannels,
                                                  boolean saveAutoconnect) {
        killDisconnectingConnection(data.uuid);

        ServerConnectionInfo connectionInfo = connectionFactory.create(this, data, joinChannels);

        connectionInfo.connect();
        addConnection(connectionInfo, saveAutoconnect);
        return connectionInfo;
    }

    public ServerConnectionInfo createConnection(ServerConfigData data) {
        return createConnection(data, null, true);
    }

    public void tryCreateConnection(ServerConfigData data, Context activity) {
        if (ServerConnectionManager.getInstance(getContext()).hasConnection(data.uuid))
            return;
        try {
            createConnection(data);
        } catch (NickNotSetException e) {
            Toast.makeText(activity, R.string.connection_error_no_nick, Toast.LENGTH_SHORT).show();
        }
    }

    public void removeConnection(ServerConnectionInfo connection, boolean saveAutoconnect) {
        NotificationManager.getInstance().clearAllNotifications(mContext, connection);
        synchronized (this) {
            synchronized (connection) {
                if (connection.isDisconnecting()) {
                    synchronized (mDisconnectingConnections) {
                        if (mDisconnectingConnections.containsKey(connection.getUUID()))
                            throw new RuntimeException("mDisconnectingConnections already contains a disconnecting connection with this UUID");
                        mDisconnectingConnections.put(connection.getUUID(), connection);
                    }
                } else if (connection.isConnecting() || connection.isConnected() || !connection.hasUserDisconnectRequest()) {
                    throw new RuntimeException("Trying to remove a non-disconnected connection");
                } else {
                    connection.close();
                }
            }
            mConnections.remove(connection);
            mConnectionsMap.remove(connection.getUUID());
            if (saveAutoconnect)
                saveAutoconnectListAsync();
            if (mConnections.isEmpty())
                IRCService.stop(mContext);
            else if (!mDestroying)
                IRCService.start(mContext); // update connection count
        }
        synchronized (mListeners) {
            for (ConnectionsListener listener : mListeners)
                listener.onConnectionRemoved(connection);
        }
    }

    public void removeConnection(ServerConnectionInfo connection) {
        removeConnection(connection, true);
    }

    /**
     * Stop keeping track of a disconnected connection. A call to this function is required if you
     * want to do something with this server's logs to make sure they are properly released.
     */
    public void killDisconnectingConnection(UUID uuid) {
        synchronized (mDisconnectingConnections) {
            ServerConnectionInfo connection = mDisconnectingConnections.get(uuid);
            if (connection == null)
                return;
            connection.close();
            mDisconnectingConnections.remove(uuid);
        }
    }

    public void disconnectAndRemoveAllConnections(boolean kill) {
        synchronized (this) {
            while (!mConnections.isEmpty()) {
                ServerConnectionInfo connection = mConnections.get(mConnections.size() - 1);
                connection.disconnect();
                removeConnection(connection, false);
                if (kill)
                    killDisconnectingConnection(connection.getUUID());
            }
            saveAutoconnectListAsync();
        }
    }

    public ServerConnectionInfo getConnection(UUID uuid) {
        synchronized (this) {
            return mConnectionsMap.get(uuid);
        }
    }

    public boolean hasConnection(UUID uuid) {
        synchronized (this) {
            return mConnectionsMap.containsKey(uuid);
        }
    }

    int getReconnectDelay(int attemptNumber) {
        if (!AppSettings.isReconnectEnabled())
            return -1;

        List<ReconnectIntervalSetting.Rule> rules = AppSettings.getReconnectIntervalRules();

        if (rules.isEmpty())
            return -1;

        int att = 0;
        for (ReconnectIntervalSetting.Rule rule : rules) {
            att += rule.repeatCount;
            if (attemptNumber < att)
                return rule.reconnectDelay;
        }

        return rules.get(rules.size() - 1).reconnectDelay;
    }

    public void addListener(ConnectionsListener listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    public void removeListener(ConnectionsListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    public void addGlobalConnectionInfoListener(ServerConnectionInfo.InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.add(listener);
        }
    }

    public void removeGlobalConnectionInfoListener(ServerConnectionInfo.InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.remove(listener);
        }
    }

    public void addGlobalChannelListListener(ServerConnectionInfo.ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.add(listener);
        }
    }

    public void removeGlobalChannelListListener(ServerConnectionInfo.ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.remove(listener);
        }
    }

    void notifyConnectionInfoChanged(ServerConnectionInfo connection) {
        if (!hasConnection(connection.getUUID()))
            return;
        synchronized (mInfoListeners) {
            for (ServerConnectionInfo.InfoChangeListener listener : mInfoListeners)
                listener.onConnectionInfoChanged(connection);
            if (!mDestroying)
                IRCService.start(mContext);
        }
    }

    void notifyChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        if (!hasConnection(connection.getUUID()))
            return;
        synchronized (mChannelsListeners) {
            for (ServerConnectionInfo.ChannelListChangeListener listener : mChannelsListeners)
                listener.onChannelListChanged(connection, newChannels);
        }
    }

    void notifyConnectionFullyDisconnected(ServerConnectionInfo connection) {
        ServerConnectionInfo removed;
        synchronized (mDisconnectingConnections) {
            removed = mDisconnectingConnections.remove(connection.getUUID());
        }
        if (removed != null)
            connection.close();
    }

    public void notifyConnectivityChanged(boolean hasAnyConnectivity) {
        boolean hasWifi = isWifiConnected(mContext);
        synchronized (this) {
            for (ServerConnectionInfo server : mConnectionsMap.values())
                server.notifyConnectivityChanged(hasAnyConnectivity, hasWifi);
        }
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (mgr == null)
            return false;
        Network network = mgr.getActiveNetwork();
        if (network == null)
            return false;
        NetworkCapabilities capabilities = mgr.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    public interface ConnectionsListener {

        void onConnectionAdded(ServerConnectionInfo connection);

        void onConnectionRemoved(ServerConnectionInfo connection);

    }
    public static class NickNotSetException extends RuntimeException {
    }
}
