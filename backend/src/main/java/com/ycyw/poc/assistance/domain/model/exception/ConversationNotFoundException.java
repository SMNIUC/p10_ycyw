package com.ycyw.poc.assistance.domain.model.exception;

import com.ycyw.poc.assistance.domain.model.ConversationId;

/** Conversation inexistante — ou volontairement indiscernable d'une conversation interdite. */
public class ConversationNotFoundException extends DomainRuleViolation {

    public ConversationNotFoundException(ConversationId id) {
        super("Conversation introuvable : " + id + ".");
    }
}
