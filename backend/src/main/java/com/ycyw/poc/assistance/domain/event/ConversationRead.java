package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.MessageId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import java.time.Instant;
import java.util.List;

/** Le destinataire a ouvert la conversation : etat « lu » d'US-24 pour les messages concernes. */
public record ConversationRead(
        ConversationId conversationId,
        ParticipantId reader,
        List<MessageId> readMessages,
        Instant at)
        implements ChatEvent {

    public ConversationRead {
        readMessages = List.copyOf(readMessages);
    }
}
