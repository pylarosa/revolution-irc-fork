package io.mrarm.irc.storage.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(
        entities = {
                MessageEntity.class,
                ConversationStateEntity.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class ChatLogDatabase extends RoomDatabase {

    public abstract MessageDao messageDao();
    public abstract ConversationStateDao conversationStateDao();

    private static volatile ChatLogDatabase INSTANCE;

    public static ChatLogDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ChatLogDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ChatLogDatabase.class,
                                    "chatlogs.db"
                            )
                            .fallbackToDestructiveMigration()
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void closeInstance() {
        synchronized (ChatLogDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }
}
