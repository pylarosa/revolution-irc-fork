package io.mrarm.irc.storage.db;

import androidx.room.TypeConverter;

import java.util.UUID;

public class Converters {

    @TypeConverter
    public static UUID fromString(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    @TypeConverter
    public static String uuidToString(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }


    @TypeConverter
    public static String fromMessageKind(MessageKind kind) {
        return kind == null ? null : kind.name();
    }

    @TypeConverter
    public static MessageKind toMessageKind(String value) {
        return value == null ? null : MessageKind.valueOf(value);
    }
}
