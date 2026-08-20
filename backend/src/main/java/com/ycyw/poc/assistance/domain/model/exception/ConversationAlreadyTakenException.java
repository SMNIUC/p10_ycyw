package com.ycyw.poc.assistance.domain.model.exception;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;

/**
 * Deux agents consultent la même file et cliquent en même temps : le second doit être refusé
 * (US-26). La règle appartient au domaine ; l'adaptateur la traduit en réponse 409.
 */
public class ConversationAlreadyTakenException extends DomainRuleViolation {

    public ConversationAlreadyTakenException(ConversationId id, ParticipantId currentAgent) {
        super(
                "La conversation "
                        + id
                        + " est déjà prise en charge par l'agent "
                        + (currentAgent == null ? "inconnu" : currentAgent.userId())
                        + ".");
    }
}
