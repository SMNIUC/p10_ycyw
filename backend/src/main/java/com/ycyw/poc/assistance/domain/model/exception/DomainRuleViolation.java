package com.ycyw.poc.assistance.domain.model.exception;

/**
 * Violation d'une règle du domaine.
 *
 * <p>Le domaine lève ses propres exceptions : il ignore qu'il existe des codes de statut HTTP. La
 * traduction en réponse (409, 403, 404) appartient à l'adaptateur primaire.
 */
public abstract class DomainRuleViolation extends RuntimeException {

    protected DomainRuleViolation(String message) {
        super(message);
    }
}
