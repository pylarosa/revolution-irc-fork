package io.mrarm.irc.message;

import java.util.UUID;

import io.mrarm.irc.storage.MessageStorageRepository;

public class MessagePipelineContext {
    public final UUID serverId;
    public final MessageStorageRepository repository;

    public MessagePipelineContext(UUID serverId,
                                  MessageStorageRepository repository) {
        this.serverId = serverId;
        this.repository = repository;
    }
}
