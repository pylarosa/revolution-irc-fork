package io.mrarm.irc.storage.message;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MessageEntity.class}, version = 1, exportSchema = false)
public abstract class ChatLogDatabase extends RoomDatabase {

    public abstract MessageDao messageDao();
}
