package com.ycyw.poc.assistance.application;

import com.ycyw.poc.assistance.domain.event.ConversationRead;
import com.ycyw.poc.assistance.domain.event.MessageDelivered;
import com.ycyw.poc.assistance.domain.event.MessagePosted;
import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.Message;
import com.ycyw.poc.assistance.domain.model.MessageBody;
import com.ycyw.poc.assistance.domain.model.MessageId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.model.exception.ConversationNotFoundException;
import com.ycyw.poc.assistance.domain.model.exception.NotAParticipantException;
import com.ycyw.poc.assistance.domain.port.ChatEventPublisher;
import com.ycyw.poc.assistance.domain.port.ConversationRepository;
import com.ycyw.poc.assistance.domain.port.IdGenerator;
import com.ycyw.poc.assistance.domain.port.MessageRepository;
import com.ycyw.poc.assistance.domain.port.TimeProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Cas d'usage de l'échange lui-même : envoi, accusé de réception, accusé de lecture (US-24).
 *
 * <p>Les champs declares ci-dessous sont les <b>ports</b> dont ce cas d'usage à besoin : deux
 * dépôts, un diffuseur, une horloge, un générateur d'identifiants. Le constructeur est généré à
 * partir d'eux ; c'est la liste des champs qui fait foi.
 */
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ChatEventPublisher publisher;
    private final TimeProvider time;
    private final IdGenerator ids;

    /**
     * Envoi d'un message.
     *
     * <p><b>L'ordre des deux dernières lignes est une décision d'architecture, pas un détail
     * d'écriture.</b> US-24 exige que « la conversation reprenne sans perte de message » après une
     * coupure : un message diffusé mais non enregistré disparaîtrait à la reconnexion. La
     * persistance fait donc autorité, le canal temps réel n'est qu'une livraison anticipée
     * (proposition d'architecture, DA-11). Un test unitaire vérifie explicitement cet ordre.
     */
    public Message post(ConversationId conversationId, ParticipantId author, String rawBody) {
        Conversation conversation = load(conversationId);
        Instant now = time.now();

        Message message =
                conversation.post(
                        new MessageId(ids.newId()), author, new MessageBody(rawBody), now);

        messages.save(message); //  1. persistance : elle fait foi
        publisher.publish(new MessagePosted(message)); //  2. diffusion : livraison anticipee

        return message;
    }

    /** Le destinataire signale avoir reçu le message : état « remis » (US-24). */
    public Optional<MessageDelivered> acknowledgeDelivery(
            ConversationId conversationId, MessageId messageId, ParticipantId recipient) {
        Conversation conversation = load(conversationId);
        requireParticipant(conversation, recipient);

        Optional<Message> found =
                messages.findById(messageId)
                        .filter(m -> m.conversationId().equals(conversationId))
                        .filter(m -> m.isAddressedTo(recipient));
        if (found.isEmpty()) {
            return Optional.empty();
        }

        Message message = found.get();
        message.markDelivered();
        messages.save(message);

        Instant now = time.now();
        MessageDelivered event = new MessageDelivered(conversationId, messageId, recipient, now);
        publisher.publish(event);
        return Optional.of(event);
    }

    /**
     * Le destinataire a ouvert la conversation : tous les messages qui lui étaient adresses passent
     * à l'état « lu », et son marqueur de lecture avance (compteur de non-lus, US-24 / US-28).
     */
    public Optional<ConversationRead> markRead(ConversationId conversationId, ParticipantId reader) {
        Conversation conversation = load(conversationId);
        Instant now = time.now();

        conversation.markReadBy(reader, now); // leve si le lecteur n'est pas participant
        conversations.save(conversation);

        List<Message> unread = messages.findUnreadFor(conversationId, reader);
        unread.forEach(
                message -> {
                    message.markRead();
                    messages.save(message);
                });

        if (unread.isEmpty()) {
            return Optional.empty();
        }
        ConversationRead event =
                new ConversationRead(
                        conversationId, reader, unread.stream().map(Message::id).toList(), now);
        publisher.publish(event);
        return Optional.of(event);
    }

    private Conversation load(ConversationId id) {
        return conversations.findById(id).orElseThrow(() -> new ConversationNotFoundException(id));
    }

    private void requireParticipant(Conversation conversation, ParticipantId who) {
        if (!conversation.hasParticipant(who.userId())) {
            throw new NotAParticipantException(conversation.id(), who);
        }
    }
}
