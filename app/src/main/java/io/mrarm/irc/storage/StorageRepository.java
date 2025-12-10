package io.mrarm.irc.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.storage.db.MessageLogDao;
import io.mrarm.irc.storage.db.MessageLogDatabase;
import io.mrarm.irc.storage.db.MessageLogEntity;
import io.mrarm.irc.storage.db.NotificationCountDao;
import io.mrarm.irc.storage.db.NotificationCountDatabase;

public class StorageRepository {

    private static StorageRepository sInstance;

    private final Context mContext;
    private final ServerConfigManager mConfigManager;

    private final Map<UUID, Map<String, MessageLogDatabase>> mMessageLogDatabases = new HashMap<>();

    private NotificationCountDatabase mNotificationCountDatabase;

    private StorageRepository(Context context) {
        mContext = context.getApplicationContext();
        mConfigManager = ServerConfigManager.getInstance(mContext);
    }

    public static synchronized StorageRepository getInstance(Context context) {
        if (sInstance == null)
            sInstance = new StorageRepository(context);
        return sInstance;
    }


    public synchronized List<MessageLogEntity> getLatestMessages(UUID serverId, int limit) {
        if (limit <= 0)
            return Collections.emptyList();
        MessageLogDao dao = getLatestMessageLogDao(serverId);
        if (dao == null)
            return Collections.emptyList();
        return dao.getLatestMessages(limit);
    }

    public synchronized MessageLogDao getLatestMessageLogDao(UUID serverId) {
        File latest = getLatestMessageLogFile(serverId);
        if (latest == null)
            return null;
        return getMessageLogDao(serverId, latest);
    }

    public synchronized MessageLogDao getMessageLogDao(UUID serverId, File logFile) {
        if (logFile == null)
            return null;
        Map<String, MessageLogDatabase> databases = mMessageLogDatabases.get(serverId);
        if (databases == null) {
            databases = new HashMap<>();
            mMessageLogDatabases.put(serverId, databases);
        }
        String key = logFile.getAbsolutePath();
        MessageLogDatabase database = databases.get(key);
        if (database == null) {
            if (!logFile.getParentFile().exists())
                logFile.getParentFile().mkdirs();
            database = buildMessageLogDatabase(logFile);
            databases.put(key, database);
        }
        return database.messageLogDao();
    }

    public synchronized void closeMessageLog(UUID serverId, File logFile) {
        if (logFile == null)
            return;
        Map<String, MessageLogDatabase> databases = mMessageLogDatabases.get(serverId);
        if (databases == null)
            return;
        MessageLogDatabase database = databases.remove(logFile.getAbsolutePath());
        if (database != null)
            database.close();
        if (databases.isEmpty())
            mMessageLogDatabases.remove(serverId);
    }

    public synchronized File getLatestMessageLogFile(UUID serverId) {
        File[] files = listMessageLogFiles(serverId);
        if (files == null || files.length == 0)
            return null;
        return files[0];
    }

    public synchronized File[] listMessageLogFiles(UUID serverId) {
        File dir = getServerChatLogDir(serverId);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.startsWith("messages-") && name.endsWith(".db");
            }
        });
        if (files == null || files.length == 0)
            return new File[0];
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o2.getName().compareTo(o1.getName());
            }
        });
        return files;
    }

    public synchronized NotificationCountDao getNotificationCountDao() {
        if (mNotificationCountDatabase == null) {
            File file = new File(mContext.getFilesDir(), "notification-count.db");
            mNotificationCountDatabase = Room.databaseBuilder(mContext,
                            NotificationCountDatabase.class,
                            file.getAbsolutePath())
                    .addMigrations(NOTIFICATION_MIGRATION_1_2)
                    .build();
        }
        return mNotificationCountDatabase.notificationCountDao();
    }

    public synchronized void closeNotificationCounts() {
        if (mNotificationCountDatabase != null) {
            mNotificationCountDatabase.close();
            mNotificationCountDatabase = null;
        }
    }

    public File getNotificationCountFile() {
        return new File(mContext.getFilesDir(), "notification-count.db");
    }

    public void ensureNotificationCountsMigrated() {
        getNotificationCountDao();
        closeNotificationCounts();
    }

    public File getServerChatLogDir(UUID serverId) {
        return mConfigManager.getServerChatLogDir(serverId);
    }

    public File getChatLogDir() {
        return mConfigManager.getChatLogDir();
    }

    private static final Migration NOTIFICATION_MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE notification_count ADD COLUMN firstMessageId TEXT");
        }
    };

    private MessageLogDatabase buildMessageLogDatabase(File file) {
        DatabaseContext context = new DatabaseContext(mContext, file);
        return Room.databaseBuilder(context, MessageLogDatabase.class, file.getName())
                .allowMainThreadQueries()
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build();
    }

    private static class DatabaseContext extends ContextWrapper {

        private final File mDatabasePath;

        DatabaseContext(Context base, File databasePath) {
            super(base);
            mDatabasePath = databasePath;
        }

        @Override
        public File getDatabasePath(String name) {
            if (!mDatabasePath.getParentFile().exists())
                mDatabasePath.getParentFile().mkdirs();
            return mDatabasePath;
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
            return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
            return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        }
    }
}
