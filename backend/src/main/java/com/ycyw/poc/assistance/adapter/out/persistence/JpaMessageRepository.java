package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.DeliveryState;
import com.ycyw.poc.assistance.domain.model.Message;
import com.ycyw.poc.assistance.domain.model.MessageId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.port.MessageRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptateur secondaire : persistance des messages. */
@Repository
public class JpaMessageRepository implements MessageRepository {

    private final MessageJpaRepository jpa;

    public JpaMessageRepository(MessageJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Message message) {
        MessageEntity entity = jpa.findById(message.id().value()).orElseGet(MessageEntity::new);
        AssistanceJpaMapper.apply(message, entity);
        jpa.save(entity);
    }

    @Override
    public Optional<Message> findById(MessageId id) {
        return jpa.findById(id.value()).map(AssistanceJpaMapper::toDomain);
    }

    @Override
    public List<Message> findByConversation(ConversationId conversationId) {
        return jpa.findByConversationIdOrderBySentAtAsc(conversationId.value()).stream()
                .map(AssistanceJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Message> findUnreadFor(ConversationId conversationId, ParticipantId recipient) {
        return jpa
                .findByConversationIdAndAuthorIdNotAndStateNotOrderBySentAtAsc(
                        conversationId.value(), recipient.userId(), DeliveryState.READ)
                .stream()
                .map(AssistanceJpaMapper::toDomain)
                .toList();
    }
}
