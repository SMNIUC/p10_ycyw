package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;

/** Une demande entre dans la file d'attente : les agents connectes doivent la voir (US-26). */
public record ConversationOpened(Conversation conversation) implements ChatEvent {

    @Override
    public ConversationId conversationId() {
        return conversation.id();
    }
}
