package io.mrarm.irc.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;
import java.util.UUID;

@Dao
public interface MessageDao {

    @Insert
    long insert(MessageEntity msg);

    @Query("SELECT * FROM messages_logs WHERE serverId = :serverId AND channel = :channel AND id < :beforeId ORDER BY id DESC LIMIT :limit")
    List<MessageEntity> loadOlderById(UUID serverId, String channel, long beforeId, int limit);

    @Query("SELECT * FROM messages_logs WHERE serverId = :serverId AND channel = :channel AND id > :afterId ORDER BY id ASC LIMIT :limit")
    List<MessageEntity> loadNewerById(UUID serverId, String channel, long afterId, int limit);

    @Query("""
            SELECT * FROM messages_logs
                WHERE serverId = :serverId AND channel = :channel AND id < :centerId
                ORDER BY id DESC LIMIT :limit
            """)
    List<MessageEntity> loadBefore(UUID serverId, String channel, long centerId, int limit);

    @Query("""
            SELECT * FROM messages_logs
            WHERE serverId = :serverId AND channel = :channel AND id > :centerId
            ORDER BY id ASC LIMIT :limit""")
    List<MessageEntity> loadAfter(UUID serverId, String channel, long centerId, int limit
    );

    @Query("""
                    SELECT * FROM messages_logs
                    WHERE serverId = :serverId
                    AND channel = :channel
                    ORDER BY timestamp DESC
                    LIMIT :limit
            """)
    List<MessageEntity> loadRecent(
            UUID serverId,
            String channel,
            int limit
    );

    @Query("""
                        SELECT * from messages_logs
                        WHERE serverId = :serverId
                        AND channel = :channel
                        AND timestamp < :olderThan
                        ORDER BY timestamp DESC
                        LIMIT :limit
            """)
    List<MessageEntity> loadOlder(
            UUID serverId,
            String channel,
            long olderThan,
            int limit
    );

    @Query("SELECT * FROM messages_logs WHERE id = :id LIMIT 1")
    MessageEntity findById(long id);
}
