package io.mrarm.irc.storage.db;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "notification_count", indices = {
        @Index(value = {"server", "channel"}, unique = true)
})
public class NotificationCountEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @ColumnInfo(name = "server")
    public String server;

    @ColumnInfo(name = "channel")
    public String channel;

    @ColumnInfo(name = "count")
    public long count;

    @ColumnInfo(name = "firstMessageId")
    @Nullable
    public String firstMessageId;
}
