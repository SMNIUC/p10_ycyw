package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ConversationStatus;
import com.ycyw.poc.assistance.domain.port.ConversationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptateur secondaire : implémente le port de persistance déclaré par le domaine.
 *
 * <p><b>Visibilité paquet, comme le dépôt Spring Data qu'il enveloppe.</b> Seul le port
 * {@code ConversationRepository} est un contrat ; cet adaptateur en est une mise en œuvre, que rien
 * n'a de raison de nommer depuis l'extérieur. Spring le découvre par balayage de composants et
 * l'injecte sous le type du port.
 */
@Repository
class JpaConversationRepository implements ConversationRepository {

    private final ConversationJpaRepository jpa;

    // Visibilité paquet, et non publique : {@code ConversationJpaRepository} est interne à
    // l'adaptateur, un constructeur public l'exposerait donc dans une signature que personne, hors
    // du paquet, ne pourrait satisfaire. Rien n'instancie cet adaptateur à la main : le domaine ne
    // connaît que le port, et Spring câble le reste.
    JpaConversationRepository(ConversationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Conversation conversation) {
        ConversationEntity entity =
                jpa.findById(conversation.id().value()).orElseGet(ConversationEntity::new);
        AssistanceJpaMapper.apply(conversation, entity);
        jpa.save(entity);
    }

    @Override
    public Optional<Conversation> findById(ConversationId id) {
        return jpa.findById(id.value()).map(AssistanceJpaMapper::toDomain);
    }

    @Override
    public List<Conversation> findWaiting() {
        return jpa.findByStatusOrderByOpenedAtAsc(ConversationStatus.WAITING).stream()
                .map(AssistanceJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Conversation> findForParticipant(UUID userId) {
        return jpa.findForParticipant(userId).stream().map(AssistanceJpaMapper::toDomain).toList();
    }
}
