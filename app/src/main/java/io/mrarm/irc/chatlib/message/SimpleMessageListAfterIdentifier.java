package io.mrarm.irc.chatlib.message;


import io.mrarm.irc.chatlib.dto.MessageListAfterIdentifier;

public class SimpleMessageListAfterIdentifier implements MessageListAfterIdentifier {

    private final int index;

    public SimpleMessageListAfterIdentifier(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

}
