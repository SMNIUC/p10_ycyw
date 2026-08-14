package com.ycyw.poc.assistance.domain.model.exception;

/**
 * Violation d'une regle du domaine.
 *
 * <p>Le domaine leve ses propres exceptions : il ignore qu'il existe des codes de statut HTTP. La
 * traduction en reponse (409, 403, 404) appartient a l'adaptateur primaire.
 */
public abstract class DomainRuleViolation extends RuntimeException {

    protected DomainRuleViolation(String message) {
        super(message);
    }
}
