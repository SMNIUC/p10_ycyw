package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import java.time.Instant;

/** Conversation clôturée — « quand je la clôture, alors le client en est informé » (US-26). */
public record ConversationClosed(ConversationId conversationId, ParticipantId closedBy, Instant at)
        implements ChatEvent {}
