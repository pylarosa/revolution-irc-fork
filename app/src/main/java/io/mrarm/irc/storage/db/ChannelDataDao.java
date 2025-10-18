package io.mrarm.irc.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChannelDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ChannelDataEntity entity);

    @Query("SELECT * FROM channel_data WHERE channel = :channel LIMIT 1")
    ChannelDataEntity get(String channel);

    @Query("SELECT * FROM channel_data")
    List<ChannelDataEntity> getAll();

    @Query("DELETE FROM channel_data")
    void clear();
}
