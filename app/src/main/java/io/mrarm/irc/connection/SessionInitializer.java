package io.mrarm.irc.connection;

import android.content.Context;

import io.mrarm.irc.BuildConfig;
import io.mrarm.irc.DCCManager;
import io.mrarm.irc.R;
import io.mrarm.irc.chatlib.irc.IRCConnection;
import io.mrarm.irc.chatlib.irc.cap.SASLCapability;
import io.mrarm.irc.chatlib.irc.cap.SASLOptions;
import io.mrarm.irc.chatlib.irc.filters.ZNCPlaybackMessageFilter;
import io.mrarm.irc.chatlib.irc.handlers.MessageCommandHandler;
import io.mrarm.irc.chatlib.message.RoomMessageStorageApi;
import io.mrarm.irc.chatlib.message.WritableMessageStorageApi;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.storage.MessageStorageRepository;
import io.mrarm.irc.util.IgnoreListMessageFilter;

public class SessionInitializer {
    private final Context context;

    public SessionInitializer(Context context) {
        this.context = context;
    }

    public void attach(
            IRCConnection connection,
            ServerConnectionSession info,
            ServerConfigData config,
            SASLOptions saslOptions
    ) {
        MessageStorageRepository repo = MessageStorageRepository.getInstance(context);

        connection.getServerConnectionData().setMessageStorageRepository(repo);
        connection.getServerConnectionData().setServerUUID(config.uuid);

        WritableMessageStorageApi storageApi = new RoomMessageStorageApi(repo, config.uuid);
        connection.getServerConnectionData().setMessageStorageApi(storageApi);

        connection.getServerConnectionData()
                .getMessageFilterList()
                .addMessageFilter(new IgnoreListMessageFilter(config));

        if (saslOptions != null) {
            connection.getServerConnectionData()
                    .getCapabilityManager()
                    .registerCapability(new SASLCapability(saslOptions));
        }

        connection.getServerConnectionData()
                .getMessageFilterList()
                .addMessageFilter(
                        new ZNCPlaybackMessageFilter(
                                connection.getServerConnectionData()
                        )
                );

        MessageCommandHandler handler =
                connection.getServerConnectionData()
                        .getCommandHandlerList()
                        .getHandler(MessageCommandHandler.class);

        DCCManager dccManager = DCCManager.getInstance(context);
        handler.setDCCServerManager(dccManager.getServer());
        handler.setDCCClientManager(dccManager.createClient(info));
        handler.setCtcpVersionReply(
                context.getString(R.string.app_name),
                BuildConfig.VERSION_NAME,
                "Android"
        );

        connection.addDisconnectListener(
                (conn, reason) -> info.notifyDisconnected()
        );
    }
}
