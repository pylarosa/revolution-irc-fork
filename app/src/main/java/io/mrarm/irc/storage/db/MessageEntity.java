package io.mrarm.irc.storage.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

import io.mrarm.irc.chatlib.dto.MessageInfo;

@Entity(tableName = "messages_logs")
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "serverId")
    public UUID serverId;

    @NonNull
    @ColumnInfo(name = "channel")
    public String channel;

    @NonNull
    @ColumnInfo(name = "kind")
    public MessageKind kind;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @NonNull
    @ColumnInfo(name = "type")
    public int type;

    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "sender")
    public String sender;

    @ColumnInfo(name = "extra_json")
    public String extraJson;

    public static MessageEntity from(UUID serverId, String channel, MessageInfo info) {
        MessageEntity e = new MessageEntity();
        e.serverId = serverId;
        e.channel = channel;
        e.timestamp = info.getDate() != null ? info.getDate().getTime() : System.currentTimeMillis();
        e.text = info.getMessage();
        e.type = (info.getType() != null ? info.getType().asInt() : MessageInfo.MessageType.NORMAL.asInt());
        e.sender = (info.getSender() != null ? info.getSender().getNick() : null);
        e.kind = MessageKind.CHANNEL;
        e.extraJson = null;
        return e;
    }
}
