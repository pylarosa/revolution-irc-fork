package io.mrarm.irc.chatlib.dto;

public interface MessageId {

    // NOTE: must be serializable using toString

    interface Parser {

        MessageId parse(String str);

    }

}
