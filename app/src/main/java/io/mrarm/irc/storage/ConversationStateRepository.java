package io.mrarm.irc.storage;

import android.content.Context;

import java.util.UUID;

import io.mrarm.irc.storage.db.ChatLogDatabase;
import io.mrarm.irc.storage.db.ConversationStateDao;
import io.mrarm.irc.storage.db.ConversationStateEntity;

public class ConversationStateRepository {

    private final ConversationStateDao dao;
    private final ChatLogDatabase db;

    public ConversationStateRepository(Context context) {
        this.db = ChatLogDatabase.getInstance(context);
        this.dao = ChatLogDatabase.getInstance(context).conversationStateDao();
    }

    public ConversationStateEntity getState(UUID serverId, String channel) {
        final ConversationStateEntity[] out = new ConversationStateEntity[1];

        db.runInTransaction(() -> {
            dao.ensureExists(serverId, channel);
            out[0] = dao.get(serverId, channel);
        });

        return out[0];
    }

    public void onUnreadMessageArrived(UUID serverId, String channel, long messageId) {
        dao.ensureExists(serverId, channel);
        dao.setFirstUnreadIfEmpty(serverId, channel, messageId);
    }

    public void markConversationRead(UUID serverId, String channel, long lastReadId) {
        dao.ensureExists(serverId, channel);
        dao.markRead(serverId, channel, lastReadId);
    }

    public void markNotified(UUID serverId, String channel, long messageId) {
        dao.ensureExists(serverId, channel);
        dao.setLastNotified(serverId, channel, messageId);
    }

    public long getUnreadCount(UUID serverId, String channel) {
        ConversationStateEntity state = getState(serverId, channel);
        if (state == null) {
            // State not ready yet; treat as unread-from-start
            return dao.getUnreadCount(serverId, channel, 0);
        }
        return dao.getUnreadCount(serverId, channel, state.lastReadId);
    }
}
