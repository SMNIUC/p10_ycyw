package com.ycyw.poc.assistance.support;

import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ConversationStatus;
import com.ycyw.poc.assistance.domain.port.ConversationRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt en mémoire.
 *
 * <p>Il tient en trente lignes parce que le port est exprimé en termes du domaine. C'est le
 * bénéfice concret de DA-06 : les cas d'usage se testent sans base de données, donc sans conteneur,
 * donc en quelques millisecondes.
 */
public class InMemoryConversationRepository implements ConversationRepository {

    private final Map<ConversationId, Conversation> store = new LinkedHashMap<>();

    @Override
    public void save(Conversation conversation) {
        store.put(conversation.id(), conversation);
    }

    @Override
    public Optional<Conversation> findById(ConversationId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Conversation> findWaiting() {
        return store.values().stream()
                .filter(conversation -> conversation.status() == ConversationStatus.WAITING)
                .sorted(Comparator.comparing(Conversation::openedAt))
                .toList();
    }

    @Override
    public List<Conversation> findForParticipant(UUID userId) {
        return store.values().stream()
                .filter(conversation -> conversation.hasParticipant(userId))
                .sorted(Comparator.comparing(Conversation::openedAt).reversed())
                .toList();
    }
}
