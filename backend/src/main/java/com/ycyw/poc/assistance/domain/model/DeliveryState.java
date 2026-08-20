package com.ycyw.poc.assistance.domain.model;

/**
 * État d'acheminement d'un message, exige par US-24 : « son état — envoyé, remis, lu — m'est
 * indique ».
 *
 * <p>La progression est monotone : un message remis ne redevient jamais simplement envoyé. C'est
 * cette règle qui protege l'affichage d'un ordre d'arrivée réseau imprévisible.
 */
public enum DeliveryState {
    SENT(0),
    DELIVERED(1),
    READ(2);

    private final int rank;

    DeliveryState(int rank) {
        this.rank = rank;
    }

    /** Vrai si {@code target} représente une progression de l'acheminement. */
    public boolean precedes(DeliveryState target) {
        return this.rank < target.rank;
    }
}
