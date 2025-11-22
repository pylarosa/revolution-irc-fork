package io.mrarm.irc.chatlib.irc.cap;


import java.util.Map;

import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.irc.CommandHandler;
import io.mrarm.irc.chatlib.irc.ServerConnectionData;

public abstract class Capability implements CommandHandler {

    public abstract String[] getNames();

    public boolean shouldEnableCapability(ServerConnectionData connection, CapabilityEntryPair capability) {
        return true;
    }

    public boolean isBlockingNegotationFinish() {
        return false;
    }

    public void onEnabled(ServerConnectionData connection) {
    }

    public void onDisabled(ServerConnectionData connection) {
    }

    public void processMessage(MessageInfo.Builder message, Map<String, String> tags) {
    }

}
