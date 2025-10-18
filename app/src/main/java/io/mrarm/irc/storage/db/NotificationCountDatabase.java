package io.mrarm.irc.storage.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {NotificationCountEntity.class}, version = NotificationCountDatabase.VERSION, exportSchema = false)
public abstract class NotificationCountDatabase extends RoomDatabase {

    public static final int VERSION = 2;

    public abstract NotificationCountDao notificationCountDao();
}
