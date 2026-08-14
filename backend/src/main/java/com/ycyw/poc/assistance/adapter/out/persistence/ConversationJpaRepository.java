package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.ConversationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acces Spring Data. Interne a l'adaptateur : le domaine ne connait que le port
 * {@code ConversationRepository}.
 */
interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {

    List<ConversationEntity> findByStatusOrderByOpenedAtAsc(ConversationStatus status);

    @Query(
            """
            select distinct c from ConversationEntity c
            join c.participants p
            where p.key.userId = :userId
            order by c.openedAt desc
            """)
    List<ConversationEntity> findForParticipant(@Param("userId") UUID userId);
}
