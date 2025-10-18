package io.mrarm.irc.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.mrarm.chatlib.android.storage.SQLiteChannelDataStorage;
import io.mrarm.chatlib.android.storage.SQLiteMessageStorageApi;
import io.mrarm.chatlib.android.storage.SQLiteMiscStorage;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.storage.db.NotificationCountDao;
import io.mrarm.irc.storage.db.NotificationCountDatabase;

public class StorageRepository {

    private static StorageRepository sInstance;

    private final Context mContext;
    private final ServerConfigManager mConfigManager;

    private final Map<UUID, SQLiteMessageStorageApi> mMessageStorageApis = new HashMap<>();
    private final Map<UUID, SQLiteMiscStorage> mMiscStorageMap = new HashMap<>();

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

    public synchronized SQLiteMessageStorageApi getMessageStorageApi(UUID serverId) {
        SQLiteMessageStorageApi api = mMessageStorageApis.get(serverId);
        if (api == null) {
            File dir = mConfigManager.getServerChatLogDir(serverId);
            if (!dir.exists())
                dir.mkdirs();
            api = new SQLiteMessageStorageApi(dir);
            mMessageStorageApis.put(serverId, api);
        }
        return api;
    }

    public synchronized void closeMessageStorage(UUID serverId) {
        SQLiteMessageStorageApi api = mMessageStorageApis.remove(serverId);
        if (api != null)
            api.close();
    }

    public synchronized SQLiteMiscStorage getMiscStorage(UUID serverId) {
        SQLiteMiscStorage storage = mMiscStorageMap.get(serverId);
        if (storage == null) {
            File file = mConfigManager.getServerMiscDataFile(serverId);
            storage = new SQLiteMiscStorage(file);
            mMiscStorageMap.put(serverId, storage);
        }
        return storage;
    }

    public synchronized void closeMiscStorage(UUID serverId) {
        SQLiteMiscStorage storage = mMiscStorageMap.remove(serverId);
        if (storage != null)
            storage.close();
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

    public SQLiteChannelDataStorage createChannelDataStorage(UUID serverId) {
        return new SQLiteChannelDataStorage(getMiscStorage(serverId));
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
}
