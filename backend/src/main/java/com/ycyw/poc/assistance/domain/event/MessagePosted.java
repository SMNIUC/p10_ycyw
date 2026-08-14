package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.Message;

/** Un message a ete <b>persiste</b> puis doit etre remis aux interlocuteurs connectes (US-24). */
public record MessagePosted(Message message) implements ChatEvent {

    @Override
    public ConversationId conversationId() {
        return message.conversationId();
    }
}
