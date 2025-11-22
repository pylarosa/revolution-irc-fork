package io.mrarm.irc.storage.message;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.room.Room;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import io.mrarm.irc.chatlib.ResponseCallback;
import io.mrarm.irc.chatlib.ResponseErrorCallback;
import io.mrarm.irc.chatlib.dto.MessageFilterOptions;
import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.dto.MessageList;
import io.mrarm.irc.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.irc.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.chatlib.util.SimpleRequestExecutor;

public class MessageStorageRepository {

    private static MessageStorageRepository sInstance;

    private final ChatLogDatabase database;
    private final SimpleRequestExecutor executor = new SimpleRequestExecutor();

    private MessageStorageRepository(Context context) {
        database = Room.databaseBuilder(context.getApplicationContext(), ChatLogDatabase.class, "chatlogs.db")
                .fallbackToDestructiveMigration()
                .build();
    }

    public static synchronized MessageStorageRepository getInstance(Context context) {
        if (sInstance == null)
            sInstance = new MessageStorageRepository(context);
        return sInstance;
    }

    public Future<Void> insert(UUID serverUuid, String channelName, MessageInfo messageInfo) {
        return executor.queue(() -> {
            MessageEntity entity = toEntity(serverUuid, channelName, messageInfo);
            database.messageDao().insert(entity);
            return null;
        }, null, null);
    }

    @Nullable
    public Future<MessageList> getMessages(UUID serverUuid, String channelName, int count, MessageFilterOptions options,
                                           MessageListAfterIdentifier after, ResponseCallback<MessageList> callback,
                                           ResponseErrorCallback errorCallback) {
        return executor.queue(() -> getMessagesInternal(serverUuid, channelName, count, options, after), callback, errorCallback);
    }

    @Nullable
    public Future<MessageList> getMessagesNear(UUID serverUuid, String channelName, MessageId messageId,
                                               MessageFilterOptions options, ResponseCallback<MessageList> callback,
                                               ResponseErrorCallback errorCallback) {
        if (!(messageId instanceof RoomMessageId))
            return null;
        RoomMessageId id = (RoomMessageId) messageId;
        return executor.queue(() -> getMessagesNearInternal(serverUuid, channelName, id, options), callback, errorCallback);
    }

    @Nullable
    public MessageList getMessagesInternal(UUID serverUuid, String channelName, int count, MessageFilterOptions options,
                                           MessageListAfterIdentifier after) {
        if (count <= 0)
            return new MessageList(Collections.emptyList(), Collections.emptyList(), null, null);
        MessageDao dao = database.messageDao();
        RoomMessageListAfterIdentifier roomAfter = (after instanceof RoomMessageListAfterIdentifier)
                ? (RoomMessageListAfterIdentifier) after : null;
        List<MessageEntity> entities;
        RoomMessageListAfterIdentifier.Direction direction = RoomMessageListAfterIdentifier.Direction.OLDER;
        if (roomAfter == null) {
            entities = dao.getLatest(serverUuid.toString(), channelName, count);
            Collections.reverse(entities);
        } else if (roomAfter.direction == RoomMessageListAfterIdentifier.Direction.NEWER) {
            direction = RoomMessageListAfterIdentifier.Direction.NEWER;
            entities = dao.getNewer(serverUuid.toString(), channelName, roomAfter.anchorId, count);
        } else {
            entities = dao.getOlder(serverUuid.toString(), channelName, roomAfter.anchorId, count);
            Collections.reverse(entities);
        }
        return toMessageList(entities, options, direction, count);
    }

    @Nullable
    public MessageList getMessagesNearInternal(UUID serverUuid, String channelName, RoomMessageId messageId,
                                               MessageFilterOptions options) {
        MessageDao dao = database.messageDao();
        List<MessageEntity> older = dao.getOlder(serverUuid.toString(), channelName, messageId.id + 1, 50);
        Collections.reverse(older);
        List<MessageEntity> newer = dao.getNewer(serverUuid.toString(), channelName, messageId.id, 50);
        List<MessageEntity> combined = new ArrayList<>(older.size() + newer.size());
        combined.addAll(older);
        combined.addAll(newer);
        MessageList list = toMessageList(combined, options, RoomMessageListAfterIdentifier.Direction.NEWER, older.size());
        if (older.size() == 50) {
            list = new MessageList(list.getMessages(), list.getMessageIds(), list.getNewer(),
                    new RoomMessageListAfterIdentifier(combined.get(0).id, RoomMessageListAfterIdentifier.Direction.OLDER));
        }
        return list;
    }

    public MessageId.Parser getMessageIdParser() {
        return RoomMessageIdParser.INSTANCE;
    }

    private MessageList toMessageList(List<MessageEntity> entities, MessageFilterOptions options,
                                      RoomMessageListAfterIdentifier.Direction direction, int requestedCount) {
        List<MessageInfo> messages = new ArrayList<>();
        List<MessageId> messageIds = new ArrayList<>();
        for (MessageEntity entity : entities) {
            MessageInfo info = toMessageInfo(entity);
            if (!matchesFilter(info, options))
                continue;
            messages.add(info);
            messageIds.add(new RoomMessageId(entity.id));
        }
        RoomMessageListAfterIdentifier older = null;
        RoomMessageListAfterIdentifier newer = null;
        if (!messages.isEmpty()) {
            if (direction == RoomMessageListAfterIdentifier.Direction.OLDER) {
                if (messages.size() == requestedCount)
                    older = new RoomMessageListAfterIdentifier(((RoomMessageId) messageIds.get(0)).id,
                            RoomMessageListAfterIdentifier.Direction.OLDER);
            } else {
                if (messages.size() == requestedCount)
                    newer = new RoomMessageListAfterIdentifier(((RoomMessageId) messageIds.get(messageIds.size() - 1)).id,
                            RoomMessageListAfterIdentifier.Direction.NEWER);
            }
        }
        return new MessageList(messages, messageIds, newer, older);
    }

    private boolean matchesFilter(MessageInfo info, @Nullable MessageFilterOptions options) {
        if (options == null)
            return true;
        if (options.restrictToMessageTypes != null && !options.restrictToMessageTypes.contains(info.getType()))
            return false;
        return options.excludeMessageTypes == null || !options.excludeMessageTypes.contains(info.getType());
    }

    private MessageEntity toEntity(UUID serverUuid, String channelName, MessageInfo messageInfo) {
        MessageEntity entity = new MessageEntity();
        entity.serverUuid = serverUuid.toString();
        entity.channelName = channelName;
        entity.date = messageInfo.getDate() != null ? messageInfo.getDate().getTime() : 0L;
        entity.text = messageInfo.getMessage();
        entity.type = messageInfo.getType() != null ? messageInfo.getType().asInt() : MessageInfo.MessageType.NORMAL.asInt();
        entity.extra = serializeExtraData(messageInfo);
        if (messageInfo.getSender() != null) {
            entity.senderData = serializeSenderInfo(messageInfo.getSender());
            if (messageInfo.getSender().getUserUUID() != null)
                entity.senderUuid = uuidToBytes(messageInfo.getSender().getUserUUID());
        }
        return entity;
    }

    private MessageInfo toMessageInfo(MessageEntity entity) {
        MessageSenderInfo sender = deserializeSenderInfo(entity.senderData, entity.senderUuid != null ? bytesToUUID(entity.senderUuid) : null);
        Date date = new Date(entity.date);
        return deserializeMessage(sender, date, entity.text, entity.type, entity.extra);
    }

    private static String serializeSenderInfo(MessageSenderInfo sender) {
        return (sender.getNickPrefixes() == null ? "" : sender.getNickPrefixes().toString()) + " " + sender.getNick() +
                (sender.getUser() != null ? "!" + sender.getUser() : "") +
                (sender.getHost() != null ? "@" + sender.getHost() : "");
    }

    private static MessageSenderInfo deserializeSenderInfo(String serialized, UUID uuid) {
        if (serialized == null || serialized.equals(""))
            return null;
        int piof = serialized.indexOf(' ');
        String prefixes = serialized.substring(0, piof);

        String nick, user = null, host = null;
        int iof = serialized.indexOf('!', piof);
        int iof2 = serialized.indexOf('@', (iof == -1 ? piof + 1 : iof + 1));
        if (iof != -1 || iof2 != -1)
            nick = serialized.substring(piof + 1, (iof != -1 ? iof : iof2));
        else
            nick = serialized.substring(piof + 1);
        if (iof != -1)
            user = serialized.substring(iof + 1, (iof2 == -1 ? serialized.length() : iof2));
        if (iof2 != -1)
            host = serialized.substring(iof2 + 1);
        return new MessageSenderInfo(nick, user, host, prefixes.length() > 0 ? new NickPrefixList(prefixes) : null, uuid);
    }

    private static String serializeExtraData(MessageInfo info) {
        MessageStorageSerializer serializer = new MessageStorageSerializer();
        return serializer.serializeExtraData(info);
    }

    private static MessageInfo deserializeMessage(MessageSenderInfo sender, Date date, String text,
                                                  int typeInt, String extraData) {
        MessageStorageSerializer serializer = new MessageStorageSerializer();
        return serializer.deserializeMessage(sender, date, text, typeInt, extraData);
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer b = ByteBuffer.wrap(new byte[16]);
        b.putLong(uuid.getMostSignificantBits());
        b.putLong(uuid.getLeastSignificantBits());
        return b.array();
    }

    private static UUID bytesToUUID(byte[] bytes) {
        ByteBuffer b = ByteBuffer.wrap(bytes);
        return new UUID(b.getLong(), b.getLong());
    }

    public static class RoomMessageId implements MessageId {

        public long id;

        RoomMessageId(long id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RoomMessageId)
                return ((RoomMessageId) obj).id == id;
            return false;
        }
    }

    public static class RoomMessageIdParser implements MessageId.Parser {
        public static final RoomMessageIdParser INSTANCE = new RoomMessageIdParser();

        @Override
        public MessageId parse(String str) {
            return new RoomMessageId(Long.parseLong(str));
        }
    }

    public static class RoomMessageListAfterIdentifier implements MessageListAfterIdentifier {
        public enum Direction {OLDER, NEWER}
        long anchorId;
        Direction direction;

        RoomMessageListAfterIdentifier(long anchorId, Direction direction) {
            this.anchorId = anchorId;
            this.direction = direction;
        }
    }
}
