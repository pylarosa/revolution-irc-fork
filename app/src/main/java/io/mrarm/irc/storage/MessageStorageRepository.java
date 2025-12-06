package io.mrarm.irc.storage;

import static io.mrarm.irc.chatlib.dto.MessageInfo.typeFromInt;

import android.content.Context;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.dto.MessageList;
import io.mrarm.irc.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.chatlib.dto.RoomMessageId;
import io.mrarm.irc.storage.db.ChatLogDatabase;
import io.mrarm.irc.storage.db.MessageDao;
import io.mrarm.irc.storage.db.MessageEntity;
import io.mrarm.irc.util.Async;

public class MessageStorageRepository {
    private static volatile MessageStorageRepository INSTANCE;

    private final ChatLogDatabase db;
    private final MessageDao dao;

    private MessageStorageRepository(Context context) {
        db = ChatLogDatabase.getInstance(context);
        dao = db.messageDao();
    }

    public static MessageStorageRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MessageStorageRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MessageStorageRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public long insertMessage(MessageEntity msg) {
        return dao.insert(msg);
    }

    public List<MessageEntity> loadRecentMessages(UUID idServer, String channel, int limit) {
        return dao.loadRecent(idServer, channel, limit);
    }

    public List<MessageEntity> loadOlderMessagesById(UUID serverId, String channel, long beforeId, int limit) {
        return dao.loadOlderById(serverId, channel, beforeId, limit);
    }

    public List<MessageEntity> loadOlderMessages(UUID idServer, String channel, long olderThan, int limit) {
        return dao.loadOlder(idServer, channel, olderThan, limit);
    }

    public List<MessageEntity> loadNewerMessagesById(UUID serverId, String channel, long afterId, int limit) {
        return dao.loadNewerById(serverId, channel, afterId, limit);
    }

    // Async variants
    public void loadOlderAsync(UUID serverId, String channel, long beforeId, int limit,
                               Consumer<List<MessageEntity>> callback) {
        Async.io(() -> loadOlderMessagesById(serverId, channel, beforeId, limit),
                callback);
    }

    public void loadNewerAsync(UUID serverId, String channel, long afterId, int limit,
                               Consumer<List<MessageEntity>> callback) {
        Async.io(() -> loadNewerMessagesById(serverId, channel, afterId, limit),
                callback);
    }

    public void clearChannelLogs(UUID idServer, String channel) {
        db.runInTransaction(() -> {
            db.getOpenHelper()
                    .getWritableDatabase()
                    .execSQL(
                            "DELETE FROM messages_logs WHERE serverId = ? AND channel = ?",
                            new Object[]{idServer.toString(), channel}
                    );
        });
    }

    public MessageList toMessageListFromRoom(List<MessageEntity> entities) {
        List<Pair<MessageInfo, MessageId>> pairs = new ArrayList<>(entities.size());

        for (MessageEntity e : entities) {
            MessageSenderInfo sender = (e.sender != null
                    ? new MessageSenderInfo(e.sender, null, null, null, null)
                    : null);

            MessageInfo info = new MessageInfo(
                    sender,
                    new Date(e.timestamp),
                    e.text,
                    typeFromInt(e.type)
            );
            MessageId id = new RoomMessageId(e.id);
            pairs.add(new Pair<>(info, id));
        }

        pairs.sort(Comparator.comparing(p -> p.first.getDate()));

        List<MessageInfo> infos = new ArrayList<>(pairs.size());
        List<MessageId> ids = new ArrayList<>(pairs.size());
        for (Pair<MessageInfo, MessageId> p : pairs) {
            infos.add(p.first);
            ids.add(p.second);
        }

        return new MessageList(infos, ids, null, null);
    }


    public Future<?> loadRecentAsync(UUID serverId, String channel, int limit,
                                     Consumer<MessageList> uiCallback) {
        return Async.io(
                () -> toMessageListFromRoom(dao.loadRecent(serverId, channel, limit)),
                uiCallback
        );
    }

    public void loadNearAsync(UUID serverId, String channel, long centerId, int limit,
                              Consumer<List<MessageEntity>> callback) {

        Async.io(() -> {
            // older → chronological descending, so reverse later
            List<MessageEntity> older = dao.loadBefore(serverId, channel, centerId, limit);

            // newer → chronological ascending
            List<MessageEntity> newer = dao.loadAfter(serverId, channel, centerId, limit);

            List<MessageEntity> combined = new ArrayList<>(older.size() + newer.size());

            // Put older first (correct chronological order)
            older.sort((a, b) -> Long.compare(a.id, b.id));
            combined.addAll(older);

            // Add the center message itself
            MessageEntity center = dao.findById(centerId);
            if (center != null)
                combined.add(center);

            // Add newer
            combined.addAll(newer);

            return combined;

        }, callback);
    }

}
