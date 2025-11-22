package io.mrarm.irc.chatlib.android;

import android.database.sqlite.SQLiteDatabase;

import java.io.File;

import io.mrarm.irc.chatlib.android.contract.ChannelDataContract;
import io.mrarm.irc.chatlib.util.SimpleRequestExecutor;

public class SQLiteMiscStorage {

    private static final int CURRENT_VERSION = 2;

    private SQLiteDatabase database;
    private final SimpleRequestExecutor executor = new SimpleRequestExecutor();

    public SQLiteMiscStorage(File path) {
        database = SQLiteDatabase.openOrCreateDatabase(path, null);
        if (database.getVersion() != CURRENT_VERSION) {
            dropTables();
            createTables();
        }
    }

    public void close() {
        database.close();
    }

    private void dropTables() {
        database.execSQL("DROP TABLE IF EXISTS " + ChannelDataContract.ChannelEntry.TABLE_NAME);
        database.setVersion(CURRENT_VERSION);
    }

    private void createTables() {
        database.execSQL(ChannelDataContract.ChannelEntry.CREATE_TABLE);
    }


    SQLiteDatabase getDatabase() {
        return database;
    }

    SimpleRequestExecutor getExecutor() {
        return executor;
    }

}
