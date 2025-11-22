package io.mrarm.irc.chatlib;


import io.mrarm.irc.chatlib.dto.StatusMessageInfo;

public interface StatusMessageListener {

    void onStatusMessage(StatusMessageInfo message);

}
