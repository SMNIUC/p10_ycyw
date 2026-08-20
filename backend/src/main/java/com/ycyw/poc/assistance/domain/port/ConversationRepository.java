package com.ycyw.poc.assistance.domain.port;

import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port secondaire de persistance de l'agrégat Conversation.
 *
 * <p>L'interface est déclarée par le domaine et implémentée par l'adaptateur : c'est l'inversion de
 * dépendance de DA-05. Elle est exprimée en objets du domaine, jamais en entités de persistance.
 */
public interface ConversationRepository {

    void save(Conversation conversation);

    Optional<Conversation> findById(ConversationId id);

    /** File d'attente des agents, par ordre d'arrivée (US-26). */
    List<Conversation> findWaiting();

    /** Conversations d'un utilisateur, les plus récentes d'abord (US-25). */
    List<Conversation> findForParticipant(UUID userId);
}
