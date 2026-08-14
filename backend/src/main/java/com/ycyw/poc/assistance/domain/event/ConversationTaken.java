package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import java.time.Instant;

/**
 * Un agent a pris la conversation en charge. Diffuse deux fois : au client, pour l'informer ; a la
 * file des agents, pour qu'elle disparaisse des autres postes (US-26).
 */
public record ConversationTaken(ConversationId conversationId, ParticipantId agent, Instant at)
        implements ChatEvent {}
