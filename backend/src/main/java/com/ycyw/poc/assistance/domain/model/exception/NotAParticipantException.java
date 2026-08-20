package com.ycyw.poc.assistance.domain.model.exception;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;

/** Cloisonnement des conversations : nul n'écrit ni ne lit une conversation où il n'est pas. */
public class NotAParticipantException extends DomainRuleViolation {

    public NotAParticipantException(ConversationId id, ParticipantId who) {
        super("L'utilisateur " + who.userId() + " ne participe pas à la conversation " + id + ".");
    }
}
