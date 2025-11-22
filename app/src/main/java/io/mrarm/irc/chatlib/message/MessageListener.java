package io.mrarm.irc.chatlib.message;

import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageInfo;

public interface MessageListener {

    void onMessage(String channel, MessageInfo message, MessageId messageId);

}
