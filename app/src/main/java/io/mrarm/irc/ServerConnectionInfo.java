package io.mrarm.irc;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.chat.ChatUIData;
import io.mrarm.irc.chatlib.ChannelListListener;
import io.mrarm.irc.chatlib.ChatApi;
import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.irc.IRCConnection;
import io.mrarm.irc.chatlib.irc.IRCConnectionRequest;
import io.mrarm.irc.chatlib.irc.ServerConnectionApi;
import io.mrarm.irc.chatlib.irc.ServerConnectionData;
import io.mrarm.irc.chatlib.irc.cap.SASLCapability;
import io.mrarm.irc.chatlib.irc.cap.SASLOptions;
import io.mrarm.irc.chatlib.irc.filters.ZNCPlaybackMessageFilter;
import io.mrarm.irc.chatlib.irc.handlers.MessageCommandHandler;
import io.mrarm.irc.chatlib.message.RoomMessageStorageApi;
import io.mrarm.irc.chatlib.message.WritableMessageStorageApi;
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.infrastructure.threading.DelayScheduler;
import io.mrarm.irc.storage.MessageStorageRepository;
import io.mrarm.irc.util.IgnoreListMessageFilter;
import io.mrarm.irc.util.StubMessageStorageApi;
import io.mrarm.irc.util.UserAutoRunCommandHelper;

// TODO: Break up this class into multiple focused components.
// Current responsibilities mixed here:
// 1) Connection lifecycle control (connect / disconnect / reconnect / connectivity handling)
// 2) IRC protocol wiring (IRCConnection creation, handlers, capabilities, filters, DCC, SASL, ZNC)
// 3) Persistence wiring (message storage repository, storage API injection, cleanup on close)
// 4) UI state holder (ChatUIData, drawer expanded state, UI listeners)
// 5) Notification bridge (NotificationManager.ConnectionManager)
// 6) Domain state & coordination (channels list, connection flags, reconnect attempts, listeners)
//
// This class currently acts as a "connection session / aggregate root".
// Refactor target: split into lifecycle controller, protocol adapter, storage adapter,
// UI-facing state holder, and notification coordinator.

public class ServerConnectionInfo {

    private ServerConnectionManager mManager;
    private final ServerConfigData mServerConfig;
    private List<String> mChannels;
    private ChatApi mApi;
    private final IRCConnectionRequest mConnectionRequest;
    private final SASLOptions mSASLOptions;
    private boolean mExpandedInDrawer = true;
    private boolean mConnected = false;
    private boolean mConnecting = false;
    private boolean mDisconnecting = false;
    private boolean mUserDisconnectRequest = false;
    private long mReconnectQueueTime = -1L;
    private final DelayScheduler mReconnectScheduler;
    private final NotificationManager.ConnectionManager mNotificationData;
    private UserAutoRunCommandHelper mAutoRunHelper;
    private final List<InfoChangeListener> mInfoListeners = new ArrayList<>();
    private final List<ChannelListChangeListener> mChannelsListeners = new ArrayList<>();
    private int mCurrentReconnectAttempt = -1;
    private final ChatUIData mChatUIData = new ChatUIData();

    public ServerConnectionInfo(ServerConnectionManager manager, ServerConfigData config,
                                IRCConnectionRequest connectionRequest, SASLOptions saslOptions,
                                List<String> joinChannels, DelayScheduler reconnectScheduler) {
        mManager = manager;
        mServerConfig = config;
        mConnectionRequest = connectionRequest;
        mSASLOptions = saslOptions;
        mNotificationData = new NotificationManager.ConnectionManager(this);
        mChannels = joinChannels;
        mReconnectScheduler = reconnectScheduler;
        if (mChannels != null)
            mChannels.sort(String::compareToIgnoreCase);
    }

    private void setApi(ChatApi api) {
        synchronized (this) {
            mApi = api;
            api.getJoinedChannelList(this::setChannels, null);
            api.subscribeChannelList(new ChannelListListener() {
                @Override
                public void onChannelListChanged(List<String> list) {
                    setChannels(list);
                }

                @Override
                public void onChannelJoined(String s) {
                }

                @Override
                public void onChannelLeft(String s) {
                }
            }, null, null);
            mChatUIData.attachToConnection(api);
        }
    }

    public ServerConnectionManager getConnectionManager() {
        return mManager;
    }

    public void connect() {
        synchronized (this) {
            if (mDisconnecting)
                throw new RuntimeException("Trying to connect with mDisconnecting set");
            if (mConnected || mConnecting)
                return;
            mConnecting = true;
            mUserDisconnectRequest = false;
            mReconnectQueueTime = -1L;
        }
        Log.i("ServerConnectionInfo", "Connecting...");

        IRCConnection connection;
        boolean createdNewConnection = false;
        if (mApi == null || !(mApi instanceof IRCConnection)) {
            connection = new IRCConnection();
            ServerConfigManager.getInstance(mManager.getContext());
            MessageStorageRepository roomRepo = MessageStorageRepository.getInstance(mManager.getContext());

            connection.getServerConnectionData().setMessageStorageRepository(roomRepo);
            connection.getServerConnectionData().setServerUUID(getUUID());

            WritableMessageStorageApi api = new RoomMessageStorageApi(roomRepo, getUUID());
            connection.getServerConnectionData().setMessageStorageApi(api);


            connection.getServerConnectionData().getMessageFilterList().addMessageFilter(new IgnoreListMessageFilter(mServerConfig));
            if (mSASLOptions != null)
                connection.getServerConnectionData().getCapabilityManager().registerCapability(
                        new SASLCapability(mSASLOptions));
            connection.getServerConnectionData().getMessageFilterList().addMessageFilter(
                    new ZNCPlaybackMessageFilter(connection.getServerConnectionData()));
            MessageCommandHandler messageHandler = connection.getServerConnectionData()
                    .getCommandHandlerList().getHandler(MessageCommandHandler.class);
            DCCManager dccManager = DCCManager.getInstance(getConnectionManager().getContext());
            messageHandler.setDCCServerManager(dccManager.getServer());
            messageHandler.setDCCClientManager(dccManager.createClient(this));
            messageHandler.setCtcpVersionReply(mManager.getContext()
                    .getString(R.string.app_name), BuildConfig.VERSION_NAME, "Android");
            connection.addDisconnectListener((IRCConnection conn, Exception reason) -> notifyDisconnected());
            createdNewConnection = true;
        } else {
            connection = (IRCConnection) mApi;
        }

        IRCConnection fConnection = connection;

        List<String> rejoinChannels = getChannels();

        connection.connect(mConnectionRequest, (Void v) -> {
            synchronized (this) {
                mConnecting = false;
                setConnected(true);
                mCurrentReconnectAttempt = 0;

                if (mServerConfig.execCommandsConnected != null) {
                    if (mAutoRunHelper == null)
                        mAutoRunHelper = new UserAutoRunCommandHelper(this);
                    mAutoRunHelper.executeUserCommands(mServerConfig.execCommandsConnected);
                }
            }

            List<String> joinChannels = new ArrayList<>();
            if (mServerConfig.autojoinChannels != null)
                joinChannels.addAll(mServerConfig.autojoinChannels);
            if (rejoinChannels != null && mServerConfig.rejoinChannels)
                joinChannels.addAll(rejoinChannels);
            if (!joinChannels.isEmpty())
                fConnection.joinChannels(joinChannels, null, null);

        }, (Exception e) -> {
            if (e instanceof UserOverrideTrustManager.UserRejectedCertificateException ||
                    (e.getCause() != null && e.getCause() instanceof
                            UserOverrideTrustManager.UserRejectedCertificateException)) {
                Log.d("ServerConnectionInfo", "User rejected the certificate");
                synchronized (this) {
                    mUserDisconnectRequest = true;
                }
            }
            notifyDisconnected();
        });

        if (createdNewConnection) {
            setApi(connection);
        }
    }

    private void disconnect(boolean userExecutedQuit) {
        synchronized (this) {
            mUserDisconnectRequest = true;
            mReconnectScheduler.cancel(mReconnectRunnable);
            if (!isConnected() && isConnecting()) {
                mConnecting = false;
                mDisconnecting = true;
                Thread disconnectThread = new Thread(() -> ((IRCConnection) getApiInstance()).disconnect(true));
                disconnectThread.setName("Disconnect Thread");
                disconnectThread.start();
            } else if (isConnected()) {
                mDisconnecting = true;
                String message = AppSettings.getDefaultQuitMessage();
                if (userExecutedQuit) {
                    ((IRCConnection) mApi).disconnect(null, null);
                } else {
                    mApi.quit(message, null, (Exception e) -> ((IRCConnection) getApiInstance()).disconnect(true));
                }
            } else {
                notifyFullyDisconnected();
            }
        }
    }

    public void disconnect() {
        disconnect(false);
    }

    public void notifyUserExecutedQuit() {
        disconnect(true);
    }

    private void notifyDisconnected() {
        synchronized (this) {
            if (mAutoRunHelper != null)
                mAutoRunHelper.cancelUserCommandExecution();
        }
        if (isDisconnecting()) {
            notifyFullyDisconnected();
            return;
        }
        synchronized (this) {
            setConnected(false);
            mConnecting = false;
            if (mDisconnecting) {
                notifyFullyDisconnected();
                return;
            }
            if (mUserDisconnectRequest)
                return;
        }
        int reconnectDelay = mManager.getReconnectDelay(mCurrentReconnectAttempt++);
        if (reconnectDelay == -1)
            return;
        Log.i("ServerConnectionInfo", "Queuing reconnect in " + reconnectDelay + " ms");
        mReconnectQueueTime = System.nanoTime();
        mReconnectScheduler.schedule(reconnectDelay, mReconnectRunnable);
    }

    private void notifyFullyDisconnected() {
        synchronized (this) {
            setConnected(false);
            mConnecting = false;
            mDisconnecting = false;
        }
        mManager.notifyConnectionFullyDisconnected(this);
    }

    public synchronized void close() {
        Log.i("ServerConnectionInfo", "Closing");
        if (getApiInstance() != null) {
            ServerConnectionData connectionData = ((ServerConnectionApi) getApiInstance())
                    .getServerConnectionData();
            connectionData.setMessageStorageApi(new StubMessageStorageApi());
            connectionData.setChannelDataStorage(null);
        }
    }

    public void notifyConnectivityChanged(boolean hasAnyConnectivity, boolean hasWifi) {
        mReconnectScheduler.cancel(mReconnectRunnable);

        if (!hasAnyConnectivity || !AppSettings.isReconnectEnabled() ||
                (AppSettings.isReconnectWiFiOnly() && !hasWifi))
            return;
        if (AppSettings.isReconnectOnConnectivityChangeEnabled()) {
            connect(); // this will be ignored if we are already connected
        } else if (mReconnectQueueTime != -1L) {
            long reconnectDelay = mManager.getReconnectDelay(mCurrentReconnectAttempt++);
            if (reconnectDelay == -1)
                return;
            reconnectDelay = reconnectDelay - (System.nanoTime() - mReconnectQueueTime) / 1000000L;
            if (reconnectDelay <= 0L)
                connect();
            else
                mReconnectScheduler.schedule(reconnectDelay, mReconnectRunnable);
        }
    }

    public UUID getUUID() {
        return mServerConfig.uuid;
    }

    public String getName() {
        return mServerConfig.name;
    }

    public synchronized ChatApi getApiInstance() {
        return mApi;
    }


    public MessageId.Parser getMessageIdParser() {
        return ((ServerConnectionApi) getApiInstance())
                .getServerConnectionData()
                .getMessageStorageApi()
                .getMessageIdParser();
    }

    public boolean isConnected() {
        synchronized (this) {
            return mConnected;
        }
    }

    public void setConnected(boolean connected) {
        synchronized (this) {
            mConnected = connected;
        }
        notifyInfoChanged();
    }

    public boolean isConnecting() {
        synchronized (this) {
            return mConnecting;
        }
    }

    public boolean isDisconnecting() {
        synchronized (this) {
            return mDisconnecting;
        }
    }

    public boolean hasUserDisconnectRequest() {
        synchronized (this) {
            return mUserDisconnectRequest;
        }
    }

    public List<String> getChannels() {
        synchronized (this) {
            return mChannels;
        }
    }

    public boolean hasChannel(String channel) {
        synchronized (this) {
            for (String c : mChannels) {
                if (c.equalsIgnoreCase(channel))
                    return true;
            }
            return false;
        }
    }

    public void setChannels(List<String> channels) {
        channels.sort(String::compareToIgnoreCase);
        synchronized (this) {
            mChannels = channels;
        }
        synchronized (mChannelsListeners) {
            mManager.notifyChannelListChanged(this, channels);
            mManager.saveAutoconnectListAsync();
            List<ChannelListChangeListener> listeners = new ArrayList<>(mChannelsListeners);
            for (ChannelListChangeListener listener : listeners)
                listener.onChannelListChanged(this, channels);
        }
    }

    public boolean isExpandedInDrawer() {
        synchronized (this) {
            return mExpandedInDrawer;
        }
    }

    public void setExpandedInDrawer(boolean expanded) {
        synchronized (this) {
            mExpandedInDrawer = expanded;
        }
    }

    public NotificationManager.ConnectionManager getNotificationManager() {
        return mNotificationData;
    }

    public String getUserNick() {
        return ((ServerConnectionApi) getApiInstance()).getServerConnectionData().getUserNick();
    }

    public ChatUIData getChatUIData() {
        return mChatUIData;
    }

    public void addOnChannelInfoChangeListener(InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.add(listener);
        }
    }

    public void removeOnChannelInfoChangeListener(InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.remove(listener);
        }
    }

    public void addOnChannelListChangeListener(ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.add(listener);
        }
    }

    public void removeOnChannelListChangeListener(ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.remove(listener);
        }
    }

    private void notifyInfoChanged() {
        synchronized (mInfoListeners) {
            for (InfoChangeListener listener : mInfoListeners)
                listener.onConnectionInfoChanged(this);
            mManager.notifyConnectionInfoChanged(this);
        }
    }

    private final Runnable mReconnectRunnable = () -> {
        mReconnectQueueTime = -1L;
        if (!AppSettings.isReconnectEnabled() || (AppSettings.isReconnectWiFiOnly() &&
                !ServerConnectionManager.isWifiConnected(mManager.getContext())))
            return;
        this.connect();
    };

    public interface InfoChangeListener {
        void onConnectionInfoChanged(ServerConnectionInfo connection);
    }

    public interface ChannelListChangeListener {
        void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels);
    }

}
