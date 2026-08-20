package com.ycyw.poc.assistance.adapter.wire;

import com.ycyw.poc.assistance.domain.model.Message;
import java.time.Instant;

/**
 * Représentation d'un message sur le fil — REST comme WebSocket.
 *
 * <p>Une seule forme pour les deux canaux : l'historique rechargé après coupure et le message reçu
 * en temps réel doivent être indiscernables côté client, sinon la reprise d'US-24 fait apparaitre
 * deux affichages différents du même message.
 *
 * <p>L'instant est transmis en UTC ; la mise en forme dans le fuseau du lecteur est une
 * préoccupation de l'interface (ENF-08).
 */
public record MessagePayload(
        String id,
        String conversationId,
        String authorId,
        String authorRole,
        String body,
        Instant sentAt,
        String state) {

    public static MessagePayload of(Message message) {
        return new MessagePayload(
                message.id().toString(),
                message.conversationId().toString(),
                message.author().userId().toString(),
                message.author().role().name(),
                message.body().text(),
                message.sentAt(),
                message.state().name());
    }
}
