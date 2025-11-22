package io.mrarm.irc.chatlib.irc;


import io.mrarm.irc.chatlib.dto.MessageInfo;

public interface MessageFilter {

    boolean filter(ServerConnectionData connection, String channel, MessageInfo messageInfo);

}
