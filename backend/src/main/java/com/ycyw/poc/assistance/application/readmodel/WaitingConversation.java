package com.ycyw.poc.assistance.application.readmodel;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import java.time.Duration;
import java.time.Instant;

/**
 * Ligne de la file d'attente des agents (US-26 : « presentees par ordre d'arrivee avec leur temps
 * d'attente »).
 *
 * <p>Le temps d'attente est calcule au moment de la lecture, a partir du port d'horloge : c'est une
 * donnee derivee, jamais stockee.
 */
public record WaitingConversation(
        ConversationId id, String subject, Instant openedAt, Duration waitingFor) {}
