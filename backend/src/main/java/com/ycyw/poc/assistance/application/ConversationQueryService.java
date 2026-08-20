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
 * <p>Sert aussi de point de contrôle d'accès aux conversations : la même règle — « seul un
 * participant lit une conversation » — est appliquée à la reprise d'historique après coupure et à
 * l'abonnement temps réel, pour qu'un abonnement STOMP ne devienne jamais une porte dérobée.
 */
@RequiredArgsConstructor
public class ConversationQueryService {

    private static final int PREVIEW_LENGTH = 80;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final TimeProvider time;

    /**
     * Historique complet d'une conversation (US-25), également utilisé à la reconnexion : le client
     * recharge l'historique faisant foi plutôt que de faire confiance à ce qu'il avait en mémoire
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
            Message last = all.isEmpty() ? null : all.getLast();
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

    /** File d'attente des agents, par ordre d'arrivée, avec le temps d'attente (US-26). */
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

    /** Vrai si l'utilisateur participe à la conversation. Utilisé avant tout abonnement temps réel. */
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
