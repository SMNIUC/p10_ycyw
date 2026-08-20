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
 * Représentation persistante de la conversation.
 *
 * <p>Délibérément distincte de l'agrégat du domaine : c'est ce qui permet à Conversation de ne
 * porter aucune annotation de framework (DA-05), règle vérifiée au build.
 *
 * <p>Le schéma {@code assistance} matérialise la frontière de module : aucune table d'un autre
 * contexte n'est référencée ici, pas même l'utilisateur — d'où {@code customer_id} en UUID nu,
 * sans clé étrangère vers le schéma {@code identity}.
 *
 * <p>Les accesseurs sont générés en portée <b>paquet</b> : ils n'existent que pour le mappeur, et
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
     * Verrou optimiste. Deux agents peuvent cliquer sur la même demande depuis deux instances : la
     * seconde écriture échoué, et l'adaptateur primaire la traduit en refus explicite (US-26).
     *
     * <p>Sans accesseur : ce champ appartient au gestionnaire de persistance, aucun code applicatif
     * n'a de raison de le lire ni de l'écrire.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private long version;

    /**
     * La collection se modifie, elle ne se remplace pas : un accesseur en écriture substituerait
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
