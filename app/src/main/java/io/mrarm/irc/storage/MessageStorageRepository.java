package io.mrarm.irc.storage;

import static io.mrarm.irc.storage.MessageStorageHelper.deserializeMessage;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    private final Context context;

    // Global monitor lock for all maintenance & write operations
    private final Object maintenanceLock = new Object();

    private MessageStorageRepository(Context ctx) {
        context = ctx;
        db = ChatLogDatabase.getInstance(ctx);
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
        synchronized (maintenanceLock) {
            return dao.insert(msg);
        }
    }

    // Async variants
    public void loadOlderAsync(UUID serverId, String channel, long beforeId, int limit,
                               Consumer<List<MessageEntity>> callback) {
        Async.io(() -> dao.loadBefore(serverId, channel, beforeId, limit),
                callback);
    }

    public void loadNewerAsync(UUID serverId, String channel, long afterId, int limit,
                               Consumer<List<MessageEntity>> callback) {
        Async.io(() -> dao.loadAfter(serverId, channel, afterId, limit),
                callback);
    }

    public void loadRecentAsync(UUID serverId, String channel, int limit,
                                Consumer<MessageList> uiCallback) {
        Async.io(() -> toMessageListFromRoom(dao.loadRecent(serverId, channel, limit)),
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
            older.sort(Comparator.comparingLong(a -> a.id));
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

    public MessageList toMessageListFromRoom(List<MessageEntity> entities) {
        List<Pair<MessageInfo, MessageId>> pairs = new ArrayList<>(entities.size());

        for (MessageEntity e : entities) {
            MessageSenderInfo sender = (e.sender != null
                    ? new MessageSenderInfo(e.sender, null, null, null, null)
                    : null);

            MessageInfo info = deserializeMessage(sender, new Date(e.timestamp), e.text, e.type, e.extraJson);
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

    public void deleteLogsForServer(UUID serverId) {
        synchronized (maintenanceLock) {
            db.runInTransaction(() -> dao.replaceDataByServer(serverId));

            db.runInTransaction(() -> dao.deleteByServer(serverId));
        }
        compactUnlocked();
    }

    public void deleteAllLogs() {
        synchronized (maintenanceLock) {
            db.runInTransaction(dao::replaceAll);

            db.runInTransaction(dao::deleteAll);
        }
        compactUnlocked();
    }

    private void compactUnlocked() {
        SupportSQLiteDatabase raw = db.getOpenHelper().getWritableDatabase();

        try {
            raw.execSQL("PRAGMA wal_checkpoint(TRUNCATE);");
            raw.close();
        } catch (Exception ignored) {
        }

        try {
            raw.execSQL("VACUUM;");
        } catch (Exception ignored) {
        }

        wipeFile(new File(context.getDatabasePath("chatlogs.db") + "-wal"), true);
        wipeFile(new File(context.getDatabasePath("chatlogs.db") + "-shm"), false);
    }


    private void wipeFile(File f, boolean resetLength) {
        if (!f.exists()) return;
        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            long len = f.length();
            byte[] zero = new byte[4096];
            long pos = 0;
            while (pos < len) {
                int toWrite = (int) Math.min(zero.length, len - pos);
                raf.write(zero, 0, toWrite);
                pos += toWrite;
            }

            if (resetLength) {
                raf.setLength(0);
            }

        } catch (Exception ignored) {
            Log.d("Catched exception while removing data: ", ignored.getMessage());
        }
    }
}
