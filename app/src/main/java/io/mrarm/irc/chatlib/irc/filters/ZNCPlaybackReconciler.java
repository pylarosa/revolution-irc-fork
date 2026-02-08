package io.mrarm.irc.chatlib.irc.filters;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.dto.MessageList;
import io.mrarm.irc.chatlib.irc.MessageFilter;
import io.mrarm.irc.chatlib.irc.MessageFilterList;
import io.mrarm.irc.chatlib.irc.ServerConnectionData;
import io.mrarm.irc.infrastructure.threading.AppAsyncExecutor;
import io.mrarm.irc.message.MessageSink;
import io.mrarm.irc.storage.MessageStorageRepository;

public class ZNCPlaybackReconciler {

    void reconcile(ServerConnectionData connection,
                   Map<String, List<MessageInfo>> bufferedMessages,
                   MessageFilter skipFilter) {
        if (bufferedMessages.isEmpty()) return;

        AppAsyncExecutor.io(() -> processBuffered(connection, bufferedMessages, skipFilter));
    }

    private void processBuffered(ServerConnectionData connection,
                                 Map<String, List<MessageInfo>> bufferedMessages,
                                 MessageFilter skipFilter) {
        MessageStorageRepository repo = connection.getMessageStorageRepository();
        MessageSink sink = connection.getMessageSink();
        MessageFilterList filterList = connection.getMessageFilterList();
        UUID serverId = connection.getServerUUID();

        if (repo == null || sink == null || serverId == null) {
            return;
        }

        for (Map.Entry<String, List<MessageInfo>> entry : bufferedMessages.entrySet()) {

            String channel = entry.getKey();
            List<MessageInfo> messages = entry.getValue();
            if (messages.isEmpty()) {
                continue;
            }

            MessageList recentList = repo.loadRecent(serverId, channel, messages.size());
            List<MessageInfo> currentMessages = recentList != null ? recentList.getMessages() : null;
            int i;
            for (i = Math.min(messages.size(), currentMessages != null ? currentMessages.size() : 0);
                 i >= 1; i--) {
                boolean matched = true;
                for (int j = 0; j < i; j++) {
                    MessageInfo left = messages.get(j);
                    MessageInfo right = currentMessages.get(currentMessages.size() - i + j);
                    if (!left.getMessage().equals(right.getMessage()) ||
                            !left.getSender().getNick().equals(right.getSender().getNick())) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    break;
                }
            }

            for (; i < messages.size(); i++) {
                MessageInfo message = messages.get(i);
                if (!filterList.filterMessageExcept(connection, channel, message, skipFilter)) {
                    continue;
                }
                sink.accept(channel, message);
            }
        }
    }
}
