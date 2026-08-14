package com.ycyw.poc.assistance.application;

import com.ycyw.poc.assistance.application.readmodel.ConversationSummary;
import com.ycyw.poc.assistance.application.readmodel.WaitingConversation;
import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.Message;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.model.exception.ConversationNotFoundException;
import com.ycyw.poc.assistance.domain.model.exception.NotAParticipantException;
import com.ycyw.poc.assistance.domain.port.ConversationRepository;
import com.ycyw.poc.assistance.domain.port.MessageRepository;
import com.ycyw.poc.assistance.domain.port.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Lectures du contexte Assistance.
 *
 * <p>Sert aussi de point de controle d'acces aux conversations : la meme regle — « seul un
 * participant lit une conversation » — est appliquee a la reprise d'historique apres coupure et a
 * l'abonnement temps reel, pour qu'un abonnement STOMP ne devienne jamais une porte derobee.
 */
@RequiredArgsConstructor
public class ConversationQueryService {

    private static final int PREVIEW_LENGTH = 80;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final TimeProvider time;

    /**
     * Historique complet d'une conversation (US-25), egalement utilise a la reconnexion : le client
     * recharge l'historique faisant foi plutot que de faire confiance a ce qu'il avait en memoire
     * (US-24).
     */
    public List<Message> history(ConversationId id, ParticipantId requester) {
        Conversation conversation = load(id);
        if (!conversation.hasParticipant(requester.userId())) {
            throw new NotAParticipantException(id, requester);
        }
        return messages.findByConversation(id);
    }

    /** Conversations d'un utilisateur, avec leur compteur de messages non lus. */
    public List<ConversationSummary> conversationsOf(ParticipantId user) {
        List<ConversationSummary> summaries = new ArrayList<>();
        for (Conversation conversation : conversations.findForParticipant(user.userId())) {
            List<Message> all = messages.findByConversation(conversation.id());
            Message last = all.isEmpty() ? null : all.get(all.size() - 1);
            summaries.add(
                    new ConversationSummary(
                            conversation.id(),
                            conversation.subject(),
                            conversation.status(),
                            conversation.openedAt(),
                            last == null ? null : last.sentAt(),
                            last == null ? null : preview(last),
                            messages.findUnreadFor(conversation.id(), user).size()));
        }
        return summaries;
    }

    /** File d'attente des agents, par ordre d'arrivee, avec le temps d'attente (US-26). */
    public List<WaitingConversation> waitingQueue() {
        Instant now = time.now();
        return conversations.findWaiting().stream()
                .map(
                        conversation ->
                                new WaitingConversation(
                                        conversation.id(),
                                        conversation.subject(),
                                        conversation.openedAt(),
                                        Duration.between(conversation.openedAt(), now)))
                .toList();
    }

    /** Vrai si l'utilisateur participe a la conversation. Utilise avant tout abonnement temps reel. */
    public boolean isParticipant(ConversationId id, UUID userId) {
        return conversations.findById(id).map(c -> c.hasParticipant(userId)).orElse(false);
    }

    private static String preview(Message message) {
        String text = message.body().text();
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + "…";
    }

    private Conversation load(ConversationId id) {
        return conversations.findById(id).orElseThrow(() -> new ConversationNotFoundException(id));
    }
}
