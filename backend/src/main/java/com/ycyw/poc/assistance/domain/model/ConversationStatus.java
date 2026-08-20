package com.ycyw.poc.assistance.domain.model;

/**
 * Cycle de vie d'une conversation (US-23, US-26).
 *
 * <pre>
 *   WAITING --prise en charge par un agent--> TAKEN --clôture--> CLOSED
 * </pre>
 */
public enum ConversationStatus {
    /** Ouverte par le client, aucun agent ne l'a encore prise en charge. */
    WAITING,
    /** Prise en charge : elle n'est plus proposée aux autres agents (US-26). */
    TAKEN,
    /** Clôturée : consultable en historique (US-25), plus alimentable. */
    CLOSED
}
