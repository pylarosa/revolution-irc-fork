package io.mrarm.irc.message;

import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.message.MessageListener;

public interface MessageBus {
    /**
     * Emit a message to subscribers. <br>
     * Intended for pipeline-internal use.
     */
    void emit(String channelName, MessageInfo message, MessageId messageId);

    /**
     * Subscribe to messages for a specific room. <br>
     * roomKey == null means "all rooms".
     */
    void subscribe(String channelName, MessageListener listener);

    void unsubscribe(String channelName, MessageListener listener);
}
