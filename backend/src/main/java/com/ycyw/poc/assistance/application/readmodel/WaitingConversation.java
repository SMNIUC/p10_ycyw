package com.ycyw.poc.assistance.application.readmodel;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import java.time.Duration;
import java.time.Instant;

/**
 * Ligne de la file d'attente des agents (US-26 : « présentées par ordre d'arrivée avec leur temps
 * d'attente »).
 *
 * <p>Le temps d'attente est calculé au moment de la lecture, à partir du port d'horloge : c'est une
 * donnée dérivée, jamais stockée.
 */
public record WaitingConversation(
        ConversationId id, String subject, Instant openedAt, Duration waitingFor) {}
