package io.mrarm.irc.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;
import java.util.UUID;

import io.mrarm.irc.storage.MessageStatsRepository;

@Dao
public interface MessageDao {

    @Insert
    long insert(MessageEntity msg);

    /**
     * Load messages older than a certain ID (scroll up)
     */
    @Query("""
                SELECT * FROM messages_logs
                WHERE serverId = :serverId AND channel = :channel AND id < :beforeId
                ORDER BY id DESC
                LIMIT :limit
            """)
    List<MessageEntity> loadBefore(UUID serverId, String channel, long beforeId, int limit);

    /**
     * Load messages newer than a certain ID (scroll down)
     */
    @Query("""
                SELECT * FROM messages_logs
                WHERE serverId = :serverId AND channel = :channel AND id > :afterId
                ORDER BY id ASC
                LIMIT :limit
            """)
    List<MessageEntity> loadAfter(UUID serverId, String channel, long afterId, int limit);

    /**
     * Initial tail load
     */
    @Query("""
                SELECT * FROM messages_logs
                WHERE serverId = :serverId AND channel = :channel
                ORDER BY id DESC
                LIMIT :limit
            """)
    List<MessageEntity> loadRecent(UUID serverId, String channel, int limit);

    /**
     * Find specific message
     */
    @Query("SELECT * FROM messages_logs WHERE id = :id LIMIT 1")
    MessageEntity findById(long id);

    /**
     * Stats
     */
    @Query("SELECT SUM(aprox_row_size) FROM messages_logs")
    Long getGlobalUsage();

    @Query("SELECT SUM(aprox_row_size) FROM messages_logs WHERE serverId = :serverId")
    Long getUsageForServer(UUID serverId);

    @Query("SELECT COUNT(*) FROM messages_logs WHERE serverId = :serverId")
    Long getMessageCountForServer(UUID serverId);

    @Query("""
                SELECT serverId, SUM(aprox_row_size) AS size
                FROM messages_logs
                GROUP BY serverId
                ORDER BY size DESC
            """)
    List<MessageStatsRepository.ServerUsage> getUsageForAllServers();

    /**
     * Deletion
     */
    @Query("DELETE FROM messages_logs WHERE serverId = :serverId")
    void deleteByServer(UUID serverId);

    @Query("DELETE FROM messages_logs")
    void deleteAll();

    @Query("""
            UPDATE messages_logs
            SET
                text = NULL,
                sender = NULL,
                channel = 'null',
                timestamp = 0,
                extra_json = NULL
            WHERE serverId = :serverId;
            """)
    void replaceDataByServer(UUID serverId);

    @Query("""
            UPDATE messages_logs
            SET
                text = NULL,
                sender = NULL,
                channel = 'null',
                timestamp = 0,
                extra_json = NULL
            """)
    void replaceAll();
}
