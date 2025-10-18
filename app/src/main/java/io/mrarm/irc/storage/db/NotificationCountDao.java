package io.mrarm.irc.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface NotificationCountDao {

    @Query("SELECT count FROM notification_count WHERE server = :server AND channel = :channel")
    Long getCount(String server, String channel);

    @Query("SELECT firstMessageId FROM notification_count WHERE server = :server AND channel = :channel")
    String getFirstMessageId(String server, String channel);

    @Query("UPDATE notification_count SET count = count + :delta WHERE server = :server AND channel = :channel")
    int increment(String server, String channel, long delta);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(NotificationCountEntity entity);

    @Query("UPDATE notification_count SET count = :count WHERE server = :server AND channel = :channel")
    int setCount(String server, String channel, long count);

    @Query("UPDATE notification_count SET firstMessageId = :messageId WHERE server = :server AND channel = :channel AND firstMessageId IS NULL")
    int setFirstMessageId(String server, String channel, String messageId);

    @Query("UPDATE notification_count SET firstMessageId = NULL WHERE server = :server AND channel = :channel")
    void resetFirstMessageId(String server, String channel);

    @Query("DELETE FROM notification_count WHERE server = :server AND channel = :channel")
    void resetChannel(String server, String channel);

    @Query("DELETE FROM notification_count WHERE server = :server")
    void removeServer(String server);

    @Query("DELETE FROM notification_count")
    void clear();

    @Transaction
    default long upsert(String server, String channel, long count) {
        int updated = setCount(server, channel, count);
        if (updated == 0) {
            NotificationCountEntity entity = new NotificationCountEntity();
            entity.server = server;
            entity.channel = channel;
            entity.count = count;
            insert(entity);
        }
        return count;
    }

    @Query("SELECT * FROM notification_count")
    List<NotificationCountEntity> getAll();
}
