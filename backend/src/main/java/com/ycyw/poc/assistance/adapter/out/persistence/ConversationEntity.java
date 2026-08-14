package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.ConversationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representation persistante de la conversation.
 *
 * <p>Deliberement distincte de l'agregat du domaine : c'est ce qui permet a Conversation de ne
 * porter aucune annotation de framework (DA-05), regle verifiee au build.
 *
 * <p>Le schema {@code assistance} materialise la frontiere de module : aucune table d'un autre
 * contexte n'est referencee ici, pas meme l'utilisateur — d'ou {@code customer_id} en UUID nu,
 * sans cle etrangere vers le schema {@code identity}.
 *
 * <p>Les accesseurs sont generes en portee <b>paquet</b> : ils n'existent que pour le mappeur, et
 * ne fuient pas hors de l'adaptateur de persistance.
 */
@Entity
@Table(name = "conversation", schema = "assistance")
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "subject", nullable = false, length = 160)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConversationStatus status;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "taken_at")
    private Instant takenAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Verrou optimiste. Deux agents peuvent cliquer sur la meme demande depuis deux instances : la
     * seconde ecriture echoue, et l'adaptateur primaire la traduit en refus explicite (US-26).
     *
     * <p>Sans accesseur : ce champ appartient au gestionnaire de persistance, aucun code applicatif
     * n'a de raison de le lire ni de l'ecrire.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private long version;

    /**
     * La collection se modifie, elle ne se remplace pas : un accesseur en ecriture substituerait
     * l'instance suivie par le gestionnaire de persistance, et la suppression des orphelins
     * cesserait de fonctionner.
     */
    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @Setter(AccessLevel.NONE)
    private List<ParticipantEntity> participants = new ArrayList<>();
}
