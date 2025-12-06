package io.mrarm.irc.chatlib.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import io.mrarm.irc.chatlib.ResponseCallback;
import io.mrarm.irc.chatlib.ResponseErrorCallback;
import io.mrarm.irc.chatlib.dto.MessageFilterOptions;
import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.dto.MessageList;
import io.mrarm.irc.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.irc.chatlib.dto.RoomMessageId;
import io.mrarm.irc.chatlib.util.SimpleRequestExecutor;
import io.mrarm.irc.storage.MessageStorageRepository;
import io.mrarm.irc.storage.db.MessageEntity;

public class RoomMessageStorageApi implements WritableMessageStorageApi {
    private final MessageStorageRepository repo;
    private final UUID serverId;


    private final Map<String, List<MessageListener>> channelListeners = new HashMap<>();
    private final List<MessageListener> globalListeners = new ArrayList<>();

    public RoomMessageStorageApi(MessageStorageRepository repo, UUID serverId) {
        this.repo = repo;
        this.serverId = serverId;
    }

    @Override
    public Future<Void> addMessage(String channelName,
                                   MessageInfo message,
                                   ResponseCallback<Void> callback,
                                   ResponseErrorCallback errorCallback) {
        return SimpleRequestExecutor.run(() -> {
            MessageEntity entity = MessageEntity.from(serverId, channelName, message);
            long roomId = repo.insertMessage(entity);

            MessageId msgId = new RoomMessageId(roomId);

            synchronized (channelListeners) {
                List<MessageListener> listeners = channelListeners.get(channelName);
                if (listeners != null) {
                    for (MessageListener listener : listeners) {
                        listener.onMessage(channelName, message, msgId);
                    }
                }
            }

            synchronized (globalListeners) {
                for (MessageListener listener : globalListeners) {
                    listener.onMessage(channelName, message, msgId);
                }
            }
            return null;
        }, callback, errorCallback);
    }

    private final MessageId.Parser parser = new RoomMessageId.Parser();

    @Override
    public MessageId.Parser getMessageIdParser() {
        return parser;
    }


    @Override
    public Future<MessageList> getMessages(String channelName,
                                           int count,
                                           MessageFilterOptions options,
                                           MessageListAfterIdentifier after,
                                           ResponseCallback<MessageList> callback,
                                           ResponseErrorCallback errorCallback) {
        return SimpleRequestExecutor.run(() -> {
            throw new UnsupportedOperationException("RoomMessageStorageApi.getMessages: use Room repository instead");
        }, callback, errorCallback);
    }

    @Override
    public Future<MessageList> getMessagesNear(String channelName,
                                               MessageId messageId,
                                               MessageFilterOptions options,
                                               ResponseCallback<MessageList> callback,
                                               ResponseErrorCallback errorCallback) {
        return SimpleRequestExecutor.run(() -> {
            throw new UnsupportedOperationException("RoomMessageStorageApi.getMessagesNear: use Room repository instead");
        }, callback, errorCallback);
    }

    @Override
    public Future<Void> deleteMessages(String channelName,
                                       List<MessageId> messages,
                                       ResponseCallback<Void> callback,
                                       ResponseErrorCallback errorCallback) {
        return SimpleRequestExecutor.run(() -> {
            throw new UnsupportedOperationException("RoomMessageStorageApi.deleteMessages: use Room repository instead");
        }, callback, errorCallback);
    }

    @Override
    public Future<Void> subscribeChannelMessages(String channelName,
                                                 MessageListener listener,
                                                 ResponseCallback<Void> callback,
                                                 ResponseErrorCallback errorCallback) {
        return SimpleRequestExecutor.run(() -> {
            if (channelName == null) {
                synchronized (globalListeners) {
                    globalListeners.add(listener);
                }
            } else {
                synchronized (channelListeners) {
                    channelListeners
                            .computeIfAbsent(channelName, k -> new ArrayList<>())
                            .add(listener);
                }
            }
            return null;
        }, callback, errorCallback);
    }

    @Override
    public Future<Void> unsubscribeChannelMessages(String channelName,
                                                   MessageListener listener,
                                                   ResponseCallback<Void> callback,
                                                   ResponseErrorCallback errorCallback) {
        return SimpleRequestExecutor.run(() -> {
            if (channelName == null) {
                synchronized (globalListeners) {
                    globalListeners.remove(listener);
                }
            } else {
                synchronized (channelListeners) {
                    List<MessageListener> listeners = channelListeners.get(channelName);
                    if (listeners != null) {
                        listeners.remove(listener);
                        if (listeners.isEmpty()) {
                            channelListeners.remove(channelName);
                        }
                    }
                }
            }
            return null;
        }, callback, errorCallback);
    }
}
