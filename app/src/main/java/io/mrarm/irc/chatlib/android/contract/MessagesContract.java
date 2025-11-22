package io.mrarm.irc.chatlib.android.contract;

import android.database.DatabaseUtils;
import android.provider.BaseColumns;

public class MessagesContract {

    private MessagesContract() { }

    public static class MessageEntry implements BaseColumns {

        public static final String TABLE_NAME_PREFIX = "messages_";
        public static final String COLUMN_NAME_SENDER_DATA = "sender_data";
        public static final String COLUMN_NAME_SENDER_UUID = "sender_uuid";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_EXTRA_DATA = "extra";

        public static String getEscapedTableName(String channel) {
            return DatabaseUtils.sqlEscapeString(TABLE_NAME_PREFIX + channel);
        }


    }

}
