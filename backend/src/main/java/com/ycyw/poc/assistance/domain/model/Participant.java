package com.ycyw.poc.assistance.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Présence d'un utilisateur dans une conversation, et date jusqu'à laquelle il a lu.
 *
 * <p>Entité interne à l'agrégat Conversation : elle ne se modifie qu'à travers la racine. Le
 * marqueur de lecture porte le compteur de messages non lus attendu par US-24 et US-28 sans avoir
 * à stocker un état par couple message/lecteur.
 */
public final class Participant {

    private final ParticipantId id;
    private final Instant joinedAt;
    private Instant lastReadAt;

    private Participant(ParticipantId id, Instant joinedAt, Instant lastReadAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt");
        this.lastReadAt = lastReadAt;
    }

    static Participant joining(ParticipantId id, Instant joinedAt) {
        return new Participant(id, joinedAt, null);
    }

    /** Reconstruction depuis la persistance. Réservé à l'adaptateur secondaire. */
    public static Participant rehydrate(ParticipantId id, Instant joinedAt, Instant lastReadAt) {
        return new Participant(id, joinedAt, lastReadAt);
    }

    /** Avance le marqueur de lecture. Il ne recule jamais : une lecture ne s'annule pas. */
    void readUpTo(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        if (lastReadAt == null || lastReadAt.isBefore(instant)) {
            lastReadAt = instant;
        }
    }

    public ParticipantId id() {
        return id;
    }

    public Instant joinedAt() {
        return joinedAt;
    }

    public Instant lastReadAt() {
        return lastReadAt;
    }
}
