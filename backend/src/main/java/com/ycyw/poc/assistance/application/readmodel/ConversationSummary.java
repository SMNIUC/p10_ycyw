package com.ycyw.poc.assistance.application.readmodel;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ConversationStatus;
import java.time.Instant;

/**
 * Vue de lecture d'une conversation : ce qu'il faut pour afficher une liste (US-25) sans charger
 * l'historique.
 *
 * <p>Les lectures peuvent contourner l'agregat, les ecritures jamais (proposition d'architecture,
 * DA-12). Ce type appartient donc a la couche applicative et n'est pas un objet du domaine.
 */
public record ConversationSummary(
        ConversationId id,
        String subject,
        ConversationStatus status,
        Instant openedAt,
        Instant lastMessageAt,
        String lastMessagePreview,
        int unreadCount) {}
