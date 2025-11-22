package io.mrarm.irc.storage.message;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MessageEntity entity);

    @Query("SELECT * FROM messages WHERE server_uuid = :serverUuid AND channel_name = :channelName ORDER BY id DESC LIMIT :limit")
    List<MessageEntity> getLatest(String serverUuid, String channelName, int limit);

    @Query("SELECT * FROM messages WHERE server_uuid = :serverUuid AND channel_name = :channelName AND id < :beforeId ORDER BY id DESC LIMIT :limit")
    List<MessageEntity> getOlder(String serverUuid, String channelName, long beforeId, int limit);

    @Query("SELECT * FROM messages WHERE server_uuid = :serverUuid AND channel_name = :channelName AND id > :afterId ORDER BY id ASC LIMIT :limit")
    List<MessageEntity> getNewer(String serverUuid, String channelName, long afterId, int limit);

    @Query("SELECT * FROM messages WHERE server_uuid = :serverUuid AND channel_name = :channelName AND id >= :anchorId ORDER BY id ASC LIMIT :limit")
    List<MessageEntity> getAround(String serverUuid, String channelName, long anchorId, int limit);
}
