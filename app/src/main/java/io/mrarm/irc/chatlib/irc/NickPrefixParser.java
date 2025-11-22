package io.mrarm.irc.chatlib.irc;

import io.mrarm.irc.chatlib.dto.NickWithPrefix;

public interface NickPrefixParser {

    NickWithPrefix parse(ServerConnectionData connection, String nick);

}
