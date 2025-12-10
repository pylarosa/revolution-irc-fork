package io.mrarm.irc.storage;

import android.content.Context;

import java.util.List;
import java.util.UUID;

import io.mrarm.irc.storage.db.ChatLogDatabase;
import io.mrarm.irc.storage.db.MessageDao;

public class MessageStatsRepository {

    private final MessageDao dao;

    public MessageStatsRepository(Context context) {
        this.dao = ChatLogDatabase.getInstance(context).messageDao();
    }

    public long getGlobalUsage() {
        return dao.getGlobalUsage();
    }

    public long getUsageForServer(UUID serverId) {
        return dao.getUsageForServer(serverId);
    }

    public long getMessageCountForServer(UUID serverId) {
        return dao.getMessageCountForServer(serverId);
    }

    public List<ServerUsage> getUsageForAllServers() {
        return dao.getUsageForAllServers();
    }

    public static class ServerUsage {
        public UUID serverId;
        public Long size;
    }
}
