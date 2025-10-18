package io.mrarm.irc.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.storage.db.MessageLogEntity;

@Config(manifest = Config.NONE)
public class StorageRepositoryLogAccessTest {

    private Context context;
    private StorageRepository repository;
    private UUID serverId;
    private File logDir;
    private File logFile;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        repository = StorageRepository.getInstance(context);
        serverId = UUID.randomUUID();
        logDir = repository.getServerChatLogDir(serverId);
        if (!logDir.exists()) {
            assertTrue(logDir.mkdirs());
        }
        logFile = new File(logDir, "messages-2024-02-15.db");

        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        SupportSQLiteOpenHelper helper = factory.create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(logFile.getAbsolutePath())
                        .callback(new SupportSQLiteOpenHelper.Callback(1) {
                            @Override
                            public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE messages (" +
                                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "sender_data TEXT, " +
                                        "sender_uuid BLOB, " +
                                        "date INTEGER NOT NULL, " +
                                        "text TEXT, " +
                                        "type INTEGER NOT NULL, " +
                                        "extra TEXT)");
                            }

                            @Override
                            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                                // no-op
                            }
                        })
                        .build());
        SupportSQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("INSERT INTO messages (sender_data, sender_uuid, date, text, type, extra) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{"userA", null, 1000L, "hello", 0, null});
        db.execSQL("INSERT INTO messages (sender_data, sender_uuid, date, text, type, extra) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{"userB", null, 2000L, "hi", 0, null});
        db.execSQL("INSERT INTO messages (sender_data, sender_uuid, date, text, type, extra) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{"userC", null, 3000L, "hey", 0, null});
        db.close();
        helper.close();
    }

    @After
    public void tearDown() {
        repository.closeMessageLogs(serverId);
        if (logDir.exists()) {
            deleteRecursive(logDir);
        }
    }

    @Test
    public void getLatestMessagesReturnsNewestFirst() {
        List<MessageLogEntity> latest = repository.getLatestMessages(serverId, 2);
        assertEquals(2, latest.size());
        assertEquals(3000L, latest.get(0).date);
        assertEquals(2000L, latest.get(1).date);
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
