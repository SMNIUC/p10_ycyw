package com.ycyw.poc.assistance.domain.model.exception;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;

/**
 * Deux agents consultent la meme file et cliquent en meme temps : le second doit etre refuse
 * (US-26). La regle appartient au domaine ; l'adaptateur la traduit en reponse 409.
 */
public class ConversationAlreadyTakenException extends DomainRuleViolation {

    public ConversationAlreadyTakenException(ConversationId id, ParticipantId currentAgent) {
        super(
                "La conversation "
                        + id
                        + " est deja prise en charge par l'agent "
                        + (currentAgent == null ? "inconnu" : currentAgent.userId())
                        + ".");
    }
}
