package com.ycyw.poc.assistance.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Presence d'un utilisateur dans une conversation, et date jusqu'a laquelle il a lu.
 *
 * <p>Entite interne a l'agregat Conversation : elle ne se modifie qu'a travers la racine. Le
 * marqueur de lecture porte le compteur de messages non lus attendu par US-24 et US-28 sans avoir
 * a stocker un etat par couple message/lecteur.
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

    /** Reconstruction depuis la persistance. Reserve a l'adaptateur secondaire. */
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
