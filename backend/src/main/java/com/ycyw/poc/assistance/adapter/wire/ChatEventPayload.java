package com.ycyw.poc.assistance.adapter.wire;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ycyw.poc.assistance.domain.event.ChatEvent;
import com.ycyw.poc.assistance.domain.event.ConversationClosed;
import com.ycyw.poc.assistance.domain.event.ConversationOpened;
import com.ycyw.poc.assistance.domain.event.ConversationRead;
import com.ycyw.poc.assistance.domain.event.ConversationTaken;
import com.ycyw.poc.assistance.domain.event.MessageDelivered;
import com.ycyw.poc.assistance.domain.event.MessagePosted;
import com.ycyw.poc.assistance.domain.model.MessageId;
import java.time.Instant;
import java.util.List;

/**
 * Trame diffusée sur le canal temps réel.
 *
 * <p>C'est la traduction de l'événement de domaine en format de transport : le domaine ignore
 * qu'elle existe. Le champ {@code type} permet au client de discriminer sans deviner d'après la
 * forme du contenu.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatEventPayload(
        String type,
        String conversationId,
        Instant occurredAt,
        MessagePayload message,
        String messageId,
        List<String> messageIds,
        String actorId,
        String actorRole,
        String status,
        String subject) {

    /**
     * <b>L'horodatage vient de l'événement, jamais de l'adaptateur.</b> Chaque branche lit l'instant
     * porté par le domaine — lui-même obtenu par le port d'horloge (DA-06). Prendre ici l'heure
     * courante ferait diverger la trame temps réel de ce qui est persisté : le client verrait deux
     * dates différentes pour un même message selon qu'il le reçoit en direct ou qu'il recharge son
     * historique.
     */
    public static ChatEventPayload from(ChatEvent event) {
        return switch (event) {
            case MessagePosted posted ->
                    new ChatEventPayload(
                            "MESSAGE_POSTED",
                            posted.conversationId().toString(),
                            posted.message().sentAt(),
                            MessagePayload.of(posted.message()),
                            null,
                            null,
                            posted.message().author().userId().toString(),
                            posted.message().author().role().name(),
                            null,
                            null);
            case MessageDelivered delivered ->
                    new ChatEventPayload(
                            "MESSAGE_DELIVERED",
                            delivered.conversationId().toString(),
                            delivered.at(),
                            null,
                            delivered.messageId().toString(),
                            null,
                            delivered.recipient().userId().toString(),
                            delivered.recipient().role().name(),
                            null,
                            null);
            case ConversationRead read ->
                    new ChatEventPayload(
                            "CONVERSATION_READ",
                            read.conversationId().toString(),
                            read.at(),
                            null,
                            null,
                            read.readMessages().stream().map(MessageId::toString).toList(),
                            read.reader().userId().toString(),
                            read.reader().role().name(),
                            null,
                            null);
            case ConversationOpened opened ->
                    new ChatEventPayload(
                            "CONVERSATION_OPENED",
                            opened.conversationId().toString(),
                            opened.conversation().openedAt(),
                            null,
                            null,
                            null,
                            opened.conversation().customer().userId().toString(),
                            opened.conversation().customer().role().name(),
                            opened.conversation().status().name(),
                            opened.conversation().subject());
            case ConversationTaken taken ->
                    new ChatEventPayload(
                            "CONVERSATION_TAKEN",
                            taken.conversationId().toString(),
                            taken.at(),
                            null,
                            null,
                            null,
                            taken.agent().userId().toString(),
                            taken.agent().role().name(),
                            "TAKEN",
                            null);
            case ConversationClosed closed ->
                    new ChatEventPayload(
                            "CONVERSATION_CLOSED",
                            closed.conversationId().toString(),
                            closed.at(),
                            null,
                            null,
                            null,
                            closed.closedBy().userId().toString(),
                            closed.closedBy().role().name(),
                            "CLOSED",
                            null);
        };
    }
}
