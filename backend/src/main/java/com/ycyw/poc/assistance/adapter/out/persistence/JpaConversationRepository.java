package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ConversationStatus;
import com.ycyw.poc.assistance.domain.port.ConversationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptateur secondaire : implemente le port de persistance declare par le domaine. */
@Repository
public class JpaConversationRepository implements ConversationRepository {

    private final ConversationJpaRepository jpa;

    public JpaConversationRepository(ConversationJpaRepository jpa) {
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
