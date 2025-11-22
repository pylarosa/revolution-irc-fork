package io.mrarm.irc.chatlib.dto;

import java.util.List;


public class MessageFilterOptions {

    public List<MessageInfo.MessageType> restrictToMessageTypes;
    public List<MessageInfo.MessageType> excludeMessageTypes;

}
