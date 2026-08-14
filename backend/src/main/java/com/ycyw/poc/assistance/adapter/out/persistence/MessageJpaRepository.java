package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.DeliveryState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acces Spring Data aux messages. Interne a l'adaptateur. */
interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {

    List<MessageEntity> findByConversationIdOrderBySentAtAsc(UUID conversationId);

    List<MessageEntity> findByConversationIdAndAuthorIdNotAndStateNotOrderBySentAtAsc(
            UUID conversationId, UUID authorId, DeliveryState state);
}
