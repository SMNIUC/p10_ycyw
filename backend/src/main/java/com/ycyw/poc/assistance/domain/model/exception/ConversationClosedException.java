package com.ycyw.poc.assistance.domain.model.exception;

import com.ycyw.poc.assistance.domain.model.ConversationId;

/** Une conversation cloturee reste consultable (US-25) mais n'accepte plus d'ecriture. */
public class ConversationClosedException extends DomainRuleViolation {

    public ConversationClosedException(ConversationId id) {
        super("La conversation " + id + " est cloturee.");
    }
}
