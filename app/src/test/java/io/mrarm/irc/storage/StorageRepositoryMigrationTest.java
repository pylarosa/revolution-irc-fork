package io.mrarm.irc.storage;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

import java.io.File;

import io.mrarm.irc.storage.db.NotificationCountDatabase;

@Config(manifest = Config.NONE)
public class StorageRepositoryMigrationTest {

    private Context context;
    private File dbFile;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbFile = new File(context.getFilesDir(), "notification-count.db");
        if (dbFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dbFile.delete();
        }
    }

    @Test
    public void migrationAddsFirstMessageIdColumn() {
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        SupportSQLiteOpenHelper helper = factory.create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(dbFile.getAbsolutePath())
                        .callback(new SupportSQLiteOpenHelper.Callback(1) {
                            @Override
                            public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE notification_count (server TEXT, channel TEXT, count INTEGER)");
                            }

                            @Override
                            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                                // no-op
                            }
                        })
                        .build());
        SupportSQLiteDatabase legacyDb = helper.getWritableDatabase();
        legacyDb.close();
        helper.close();

        StorageRepository repository = StorageRepository.getInstance(context);
        repository.ensureNotificationCountsMigrated();

        SupportSQLiteOpenHelper verifyHelper = factory.create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(dbFile.getAbsolutePath())
                        .callback(new SupportSQLiteOpenHelper.Callback(NotificationCountDatabase.VERSION) {
                            @Override
                            public void onCreate(SupportSQLiteDatabase db) {
                                // database already exists
                            }

                            @Override
                            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                                // no-op
                            }
                        })
                        .build());
        SupportSQLiteDatabase migratedDb = verifyHelper.getReadableDatabase();
        boolean hasColumn = false;
        try (android.database.Cursor cursor = migratedDb.query("PRAGMA table_info('notification_count')")) {
            while (cursor.moveToNext()) {
                String columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if ("firstMessageId".equals(columnName)) {
                    hasColumn = true;
                    break;
                }
            }
        }
        migratedDb.close();
        verifyHelper.close();
        assertTrue("Migration should add firstMessageId column", hasColumn);
    }
}
