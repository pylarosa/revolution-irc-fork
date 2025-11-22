package io.mrarm.irc.chatlib.message;

import java.util.List;
import java.util.concurrent.Future;

import io.mrarm.irc.chatlib.ResponseCallback;
import io.mrarm.irc.chatlib.ResponseErrorCallback;
import io.mrarm.irc.chatlib.dto.MessageFilterOptions;
import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageList;
import io.mrarm.irc.chatlib.dto.MessageListAfterIdentifier;

public interface MessageStorageApi {

    MessageId.Parser getMessageIdParser();

    Future<MessageList> getMessages(String channelName, int count, MessageFilterOptions options,
                                    MessageListAfterIdentifier after, ResponseCallback<MessageList> callback,
                                    ResponseErrorCallback errorCallback);

    Future<MessageList> getMessagesNear(String channelName, MessageId messageId, MessageFilterOptions options,
                                        ResponseCallback<MessageList> callback,
                                        ResponseErrorCallback errorCallback);

    Future<Void> deleteMessages(String channelName, List<MessageId> messages, ResponseCallback<Void> callback,
                                ResponseErrorCallback errorCallback);

    Future<Void> subscribeChannelMessages(String channelName, MessageListener listener, ResponseCallback<Void> callback,
                                          ResponseErrorCallback errorCallback);

    Future<Void> unsubscribeChannelMessages(String channelName, MessageListener listener,
                                            ResponseCallback<Void> callback, ResponseErrorCallback errorCallback);

}
