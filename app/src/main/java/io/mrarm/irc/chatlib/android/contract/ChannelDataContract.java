package io.mrarm.irc.chatlib.android.contract;

public class ChannelDataContract {

    private ChannelDataContract() { }

    public static class ChannelEntry {

        public static final String TABLE_NAME = "channel_data";
        public static final String COLUMN_NAME_CHANNEL = "channel";
        public static final String COLUMN_NAME_TOPIC = "topic";
        public static final String COLUMN_NAME_TOPIC_SET_BY = "topic_set_by";
        public static final String COLUMN_NAME_TOPIC_SET_ON = "topic_set_on";

        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_NAME_CHANNEL + " TEXT PRIMARY KEY," +
                COLUMN_NAME_TOPIC + " TEXT," +
                COLUMN_NAME_TOPIC_SET_BY + " TEXT," +
                COLUMN_NAME_TOPIC_SET_ON + " INTEGER)";

    }
}
