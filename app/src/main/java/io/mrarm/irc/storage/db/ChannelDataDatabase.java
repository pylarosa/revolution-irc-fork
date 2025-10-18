package io.mrarm.irc.storage.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ChannelDataEntity.class}, version = 2, exportSchema = false)
public abstract class ChannelDataDatabase extends RoomDatabase {
    public abstract ChannelDataDao channelDataDao();
}
