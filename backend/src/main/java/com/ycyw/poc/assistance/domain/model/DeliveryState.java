package com.ycyw.poc.assistance.domain.model;

/**
 * Etat d'acheminement d'un message, exige par US-24 : « son etat — envoye, remis, lu — m'est
 * indique ».
 *
 * <p>La progression est monotone : un message remis ne redevient jamais simplement envoye. C'est
 * cette regle qui protege l'affichage d'un ordre d'arrivee reseau imprevisible.
 */
public enum DeliveryState {
    SENT(0),
    DELIVERED(1),
    READ(2);

    private final int rank;

    DeliveryState(int rank) {
        this.rank = rank;
    }

    /** Vrai si {@code target} represente une progression de l'acheminement. */
    public boolean precedes(DeliveryState target) {
        return this.rank < target.rank;
    }
}
