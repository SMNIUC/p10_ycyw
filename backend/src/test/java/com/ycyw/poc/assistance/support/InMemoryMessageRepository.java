package com.ycyw.poc.assistance.support;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.DeliveryState;
import com.ycyw.poc.assistance.domain.model.Message;
import com.ycyw.poc.assistance.domain.model.MessageId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.port.MessageRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Dépôt de messages en mémoire. */
public class InMemoryMessageRepository implements MessageRepository {

    private final Map<MessageId, Message> store = new LinkedHashMap<>();

    @Override
    public void save(Message message) {
        store.put(message.id(), message);
    }

    @Override
    public Optional<Message> findById(MessageId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Message> findByConversation(ConversationId conversationId) {
        return store.values().stream()
                .filter(message -> message.conversationId().equals(conversationId))
                .sorted(Comparator.comparing(Message::sentAt))
                .toList();
    }

    @Override
    public List<Message> findUnreadFor(ConversationId conversationId, ParticipantId recipient) {
        return store.values().stream()
                .filter(message -> message.conversationId().equals(conversationId))
                .filter(message -> message.isAddressedTo(recipient))
                .filter(message -> message.state() != DeliveryState.READ)
                .sorted(Comparator.comparing(Message::sentAt))
                .toList();
    }
}
