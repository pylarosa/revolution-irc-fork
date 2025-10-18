package io.mrarm.irc.storage.db;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "channel_data")
public class ChannelDataEntity {

    @PrimaryKey
    @ColumnInfo(name = "channel")
    @Nullable
    public String channel;

    @ColumnInfo(name = "topic")
    @Nullable
    public String topic;

    @ColumnInfo(name = "topic_set_by")
    @Nullable
    public String topicSetBy;

    @ColumnInfo(name = "topic_set_on")
    public Long topicSetOn;
}
