package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.Message;

/** Un message a été <b>persisté</b> puis doit être remis aux interlocuteurs connectés (US-24). */
public record MessagePosted(Message message) implements ChatEvent {

    @Override
    public ConversationId conversationId() {
        return message.conversationId();
    }
}
