package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.ParticipantRole;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Présence d'un utilisateur dans une conversation, et son marqueur de lecture. */
@Entity
@Table(name = "participant", schema = "assistance")
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantEntity {

    /** La clé est fixée à la construction et ne change jamais : pas d'accesseur en écriture. */
    @EmbeddedId
    @Setter(AccessLevel.NONE)
    private ParticipantKey key;

    /** Le lien vers la racine est établi à la construction ; personne d'autre n'a à le manipuler. */
    @MapsId("conversationId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private ConversationEntity conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ParticipantRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    ParticipantEntity(ConversationEntity conversation, ParticipantKey key) {
        this.conversation = conversation;
        this.key = key;
    }
}
