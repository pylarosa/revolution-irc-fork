package io.mrarm.irc.chatlib.message;


import java.util.concurrent.Future;

import io.mrarm.irc.chatlib.ResponseCallback;
import io.mrarm.irc.chatlib.ResponseErrorCallback;
import io.mrarm.irc.chatlib.dto.MessageInfo;

public interface WritableMessageStorageApi extends MessageStorageApi {

    Future<Void> addMessage(String channelName, MessageInfo message, ResponseCallback<Void> callback,
                            ResponseErrorCallback errorCallback);


}
