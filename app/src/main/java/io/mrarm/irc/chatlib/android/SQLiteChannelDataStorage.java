package io.mrarm.irc.chatlib.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.util.Date;
import java.util.concurrent.Future;

import io.mrarm.irc.chatlib.android.contract.ChannelDataContract;
import io.mrarm.irc.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.chatlib.irc.ChannelDataStorage;


public class SQLiteChannelDataStorage implements ChannelDataStorage {

    private SQLiteMiscStorage storage;
    private SQLiteStatement createChannelStatement;
    private SQLiteStatement updateTopicStatement;

    public SQLiteChannelDataStorage(SQLiteMiscStorage storage) {
        this.storage = storage;
    }

    @Override
    public Future<StoredData> getOrCreateChannelData(String channel) {
        return storage.getExecutor().queue(() -> {
            SQLiteDatabase db = storage.getDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT " + ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC + "," +
                            ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC_SET_BY + "," +
                            ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC_SET_ON +
                            " FROM " + ChannelDataContract.ChannelEntry.TABLE_NAME +
                            " WHERE " + ChannelDataContract.ChannelEntry.COLUMN_NAME_CHANNEL
                            + "=?1",
                    new String[]{channel});
            if (!cursor.moveToFirst()) {
                if (createChannelStatement == null)
                    createChannelStatement = db.compileStatement("INSERT INTO " +
                            ChannelDataContract.ChannelEntry.TABLE_NAME + " (" +
                            ChannelDataContract.ChannelEntry.COLUMN_NAME_CHANNEL + ")" +
                            "VALUES (?1)");
                createChannelStatement.bindString(1, channel);
                createChannelStatement.executeInsert();
                createChannelStatement.clearBindings();
                return null;
            }
            int topicColumn = cursor.getColumnIndex(
                    ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC);
            int topicSetByColumn = cursor.getColumnIndex(
                    ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC_SET_BY);
            int topicSetOnColumn = cursor.getColumnIndex(
                    ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC_SET_ON);
            String topic = cursor.getString(topicColumn);
            MessageSenderInfo topicSetBy = MessageStorageHelper.deserializeSenderInfo(
                    cursor.getString(topicSetByColumn), null);
            long topicSetOn = cursor.getLong(topicSetOnColumn);
            return new StoredData(topic, topicSetBy, new Date(topicSetOn * 1000L));
        }, null, null);
    }

    @Override
    public Future<Void> updateTopic(String channel, String topic, MessageSenderInfo setBy,
                                    Date setOn) {
        return storage.getExecutor().queue(() -> {
            SQLiteDatabase db = storage.getDatabase();
            if (updateTopicStatement == null)
                updateTopicStatement = db.compileStatement(
                        "UPDATE " + ChannelDataContract.ChannelEntry.TABLE_NAME +
                                " SET " + ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC + "=?2, " +
                                ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC_SET_BY + "=?3, " +
                                ChannelDataContract.ChannelEntry.COLUMN_NAME_TOPIC_SET_ON + "=?4" +
                                " WHERE " + ChannelDataContract.ChannelEntry.COLUMN_NAME_CHANNEL + "=?1");
            updateTopicStatement.bindString(1, channel);
            updateTopicStatement.bindString(2, topic);
            if (setBy != null)
                updateTopicStatement.bindString(3,
                        MessageStorageHelper.serializeSenderInfo(setBy));
            else
                updateTopicStatement.bindNull(3);
            if (setOn != null)
                updateTopicStatement.bindLong(4, setOn.getTime() / 1000L);
            else
                updateTopicStatement.bindNull(4);
            updateTopicStatement.executeUpdateDelete();
            updateTopicStatement.clearBindings();
            return null;
        }, null, null);
    }

}
