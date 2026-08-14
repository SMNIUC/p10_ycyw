package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.MessageId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import java.time.Instant;

/** Le destinataire a recu le message sur son canal temps reel : etat « remis » d'US-24. */
public record MessageDelivered(
        ConversationId conversationId, MessageId messageId, ParticipantId recipient, Instant at)
        implements ChatEvent {}
