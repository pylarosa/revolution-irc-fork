package io.mrarm.irc.util;

import java.util.List;
import java.util.concurrent.Future;

import io.mrarm.irc.chatlib.ResponseCallback;
import io.mrarm.irc.chatlib.ResponseErrorCallback;
import io.mrarm.irc.chatlib.dto.MessageFilterOptions;
import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.dto.MessageList;
import io.mrarm.irc.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.irc.chatlib.message.MessageListener;
import io.mrarm.irc.chatlib.message.WritableMessageStorageApi;
import io.mrarm.irc.chatlib.util.InstantFuture;

public class StubMessageStorageApi implements WritableMessageStorageApi {

    @Override
    public Future<Void> addMessage(String s, MessageInfo messageInfo, ResponseCallback<Void> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return new InstantFuture<>(null);
    }

    @Override
    public MessageId.Parser getMessageIdParser() {
        return null;
    }

    @Override
    public Future<MessageList> getMessages(String s, int i, MessageFilterOptions filterOptions, MessageListAfterIdentifier messageListAfterIdentifier, ResponseCallback<MessageList> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return null;
    }

    @Override
    public Future<MessageList> getMessagesNear(String s, MessageId messageId, MessageFilterOptions messageFilterOptions, ResponseCallback<MessageList> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return null;
    }

    @Override
    public Future<Void> deleteMessages(String s, List<MessageId> list, ResponseCallback<Void> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return null;
    }

    @Override
    public Future<Void> subscribeChannelMessages(String s, MessageListener messageListener, ResponseCallback<Void> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return new InstantFuture<>(null);
    }

    @Override
    public Future<Void> unsubscribeChannelMessages(String s, MessageListener messageListener, ResponseCallback<Void> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return new InstantFuture<>(null);
    }

}
