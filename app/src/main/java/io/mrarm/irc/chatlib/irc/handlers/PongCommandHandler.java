package io.mrarm.irc.chatlib.irc.handlers;


import java.util.List;
import java.util.Map;

import io.mrarm.irc.chatlib.irc.CommandHandler;
import io.mrarm.irc.chatlib.irc.ErrorCommandHandler;
import io.mrarm.irc.chatlib.irc.InvalidMessageException;
import io.mrarm.irc.chatlib.irc.MessagePrefix;
import io.mrarm.irc.chatlib.irc.RequestResponseCommandHandler;
import io.mrarm.irc.chatlib.irc.ServerConnectionData;

public class PongCommandHandler extends RequestResponseCommandHandler<String, Void> {

    public PongCommandHandler(ErrorCommandHandler handler) {
        super(handler, false);
    }

    @Override
    public Object[] getHandledCommands() {
        return new Object[]{"PONG"};
    }

    @Override
    public int[] getHandledErrors() {
        return new int[0];
    }

    @Override
    public void handle(ServerConnectionData connection, MessagePrefix sender, String command, List<String> params,
                       Map<String, String> tags)
            throws InvalidMessageException {
        onResponse(CommandHandler.getParamOrNull(params, 1), null);
    }

    @Override
    public boolean onError(int commandId, List<String> params) {
        return true;
    }

}
