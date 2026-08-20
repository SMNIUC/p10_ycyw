package com.ycyw.poc.assistance.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Message échange dans une conversation.
 *
 * <p><b>Pourquoi le message n'est pas charge dans l'agrégat.</b> Conversation est la racine
 * d'agrégat (proposition d'architecture, § 5.4) : elle protege les invariants d'appartenance et de
 * cycle de vie. Les messages, eux, sont créés par la racine puis persistes par leur propre port :
 * charger l'historique complet à chaque envoi coûterait davantage à chaque message échange, sans
 * proteger aucun invariant supplémentaire — un message ne dépend pas des précédents.
 */
public final class Message {

    private final MessageId id;
    private final ConversationId conversationId;
    private final ParticipantId author;
    private final MessageBody body;
    private final Instant sentAt;
    private DeliveryState state;

    private Message(
            MessageId id,
            ConversationId conversationId,
            ParticipantId author,
            MessageBody body,
            Instant sentAt,
            DeliveryState state) {
        this.id = Objects.requireNonNull(id, "id");
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId");
        this.author = Objects.requireNonNull(author, "author");
        this.body = Objects.requireNonNull(body, "body");
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt");
        this.state = Objects.requireNonNull(state, "state");
    }

    static Message sent(
            MessageId id,
            ConversationId conversationId,
            ParticipantId author,
            MessageBody body,
            Instant sentAt) {
        return new Message(id, conversationId, author, body, sentAt, DeliveryState.SENT);
    }

    /** Reconstruction depuis la persistance. Réservé à l'adaptateur secondaire. */
    public static Message rehydrate(
            MessageId id,
            ConversationId conversationId,
            ParticipantId author,
            MessageBody body,
            Instant sentAt,
            DeliveryState state) {
        return new Message(id, conversationId, author, body, sentAt, state);
    }

    /** Le destinataire a reçu le message sur son canal temps réel. */
    public void markDelivered() {
        advanceTo(DeliveryState.DELIVERED);
    }

    /** Le destinataire a ouvert la conversation contenant le message. */
    public void markRead() {
        advanceTo(DeliveryState.READ);
    }

    private void advanceTo(DeliveryState target) {
        if (state.precedes(target)) {
            state = target;
        }
    }

    /** Vrai si l'utilisateur donne n'est pas l'auteur — donc destinataire du message. */
    public boolean isAddressedTo(ParticipantId participant) {
        return !author.userId().equals(participant.userId());
    }

    public MessageId id() {
        return id;
    }

    public ConversationId conversationId() {
        return conversationId;
    }

    public ParticipantId author() {
        return author;
    }

    public MessageBody body() {
        return body;
    }

    public Instant sentAt() {
        return sentAt;
    }

    public DeliveryState state() {
        return state;
    }
}
