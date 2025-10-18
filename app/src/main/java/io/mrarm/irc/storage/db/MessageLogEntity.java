package io.mrarm.irc.storage.db;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageLogEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @ColumnInfo(name = "sender_data")
    @Nullable
    public String senderData;

    @ColumnInfo(name = "sender_uuid")
    @Nullable
    public byte[] senderUuid;

    @ColumnInfo(name = "date")
    public long date;

    @ColumnInfo(name = "text")
    @Nullable
    public String text;

    @ColumnInfo(name = "type")
    public int type;

    @ColumnInfo(name = "extra")
    @Nullable
    public String extra;
}
