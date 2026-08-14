package com.ycyw.poc.assistance.domain.port;

import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port secondaire de persistance de l'agregat Conversation.
 *
 * <p>L'interface est declaree par le domaine et implementee par l'adaptateur : c'est l'inversion de
 * dependance de DA-05. Elle est exprimee en objets du domaine, jamais en entites de persistance.
 */
public interface ConversationRepository {

    void save(Conversation conversation);

    Optional<Conversation> findById(ConversationId id);

    /** File d'attente des agents, par ordre d'arrivee (US-26). */
    List<Conversation> findWaiting();

    /** Conversations d'un utilisateur, les plus recentes d'abord (US-25). */
    List<Conversation> findForParticipant(UUID userId);
}
