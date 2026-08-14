package com.ycyw.poc.assistance.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Cle composee de la table {@code assistance.participant}. */
@Embeddable
public class ParticipantKey implements Serializable {

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected ParticipantKey() {
        // requis par JPA
    }

    ParticipantKey(UUID conversationId, UUID userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    UUID getConversationId() {
        return conversationId;
    }

    UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ParticipantKey key)) {
            return false;
        }
        return Objects.equals(conversationId, key.conversationId)
                && Objects.equals(userId, key.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId);
    }
}
