package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.DeliveryState;
import com.ycyw.poc.assistance.domain.model.ParticipantRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Représentation persistante d'un message.
 *
 * <p>La conversation est référencée par son identifiant, sans association JPA : le message est
 * charge par son propre port, jamais par navigation depuis l'agrégat (voir {@code Message}).
 */
@Entity
@Table(name = "message", schema = "assistance")
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 20)
    private ParticipantRole authorRole;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private DeliveryState state;
}
