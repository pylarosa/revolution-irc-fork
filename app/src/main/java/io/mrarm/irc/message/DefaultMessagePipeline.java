package io.mrarm.irc.message;

import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.dto.RoomMessageId;
import io.mrarm.irc.storage.db.MessageEntity;

public class DefaultMessagePipeline implements MessagePipeline {

    private final MessagePipelineContext pipelineContext;

    // NOTE:
    // This bus is intentionally unused at the moment.
    // It will be used by accept(...) once persistence + emission
    // are moved here from RoomMessageStorageApi.
    private final MessageBus messageBus;


    public DefaultMessagePipeline(MessagePipelineContext context,
                                  MessageBus messageBus) {
        this.pipelineContext = context;
        this.messageBus = messageBus;
    }

    @Override
    public void accept(String channelName, MessageInfo message) {
        final MessageId messageId;

        try {
            messageId = persist(channelName, message);
        } catch (Throwable t) {
            // MUST NOT throw to caller
            // Swallow or log — policy decision later
            return;
        }

        messageBus.emit(channelName, message, messageId);
    }

    @Override
    public void shutdown() {
        // no-op for now
        // future: flush queues, stop executors, etc.
    }

    protected MessageId persist(String channelName, MessageInfo message) {
        MessageEntity entity = MessageEntity.from(pipelineContext.serverId, channelName, message);
        return new RoomMessageId(pipelineContext.repository.insertMessage(entity));
    }
}
