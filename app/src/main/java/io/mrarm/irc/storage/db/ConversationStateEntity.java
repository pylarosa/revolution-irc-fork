package io.mrarm.irc.storage.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

import java.util.UUID;
@Entity(
        tableName = "conversation_state",
        primaryKeys = {"serverId", "channel"},
        indices = {
                @Index({"serverId"}),
                @Index({"serverId", "channel"})
        }
)
public class ConversationStateEntity {

    @NonNull
    public UUID serverId;

    @NonNull
    public String channel;

    /** Highest messages_logs.id the user has read */
    public long lastReadId;

    /** First unread messages_logs.id (0 = none) */
    public long firstUnreadId;

    /** Highest messages_logs.id already notified at Android level */
    public long lastNotifiedId;

    /** Optional future-proofing */
    public long mutedUntilMs;
}
