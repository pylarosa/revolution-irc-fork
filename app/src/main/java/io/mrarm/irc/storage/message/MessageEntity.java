package io.mrarm.irc.storage.message;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages", indices = {@Index(value = {"server_uuid", "channel_name", "id"})})
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "server_uuid")
    public String serverUuid;

    @Nullable
    @ColumnInfo(name = "channel_name")
    public String channelName;

    @Nullable
    @ColumnInfo(name = "sender_data")
    public String senderData;

    @Nullable
    @ColumnInfo(name = "sender_uuid")
    public byte[] senderUuid;

    @ColumnInfo(name = "date")
    public long date;

    @Nullable
    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "type")
    public int type;

    @Nullable
    @ColumnInfo(name = "extra")
    public String extra;
}
