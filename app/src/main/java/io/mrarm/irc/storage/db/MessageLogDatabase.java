package io.mrarm.irc.storage.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MessageLogEntity.class}, version = 1, exportSchema = false)
public abstract class MessageLogDatabase extends RoomDatabase {
    public abstract MessageLogDao messageLogDao();
}
