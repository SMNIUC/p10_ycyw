package com.ycyw.poc.assistance.adapter.wire;

import com.ycyw.poc.assistance.domain.model.Message;
import java.time.Instant;

/**
 * Representation d'un message sur le fil — REST comme WebSocket.
 *
 * <p>Une seule forme pour les deux canaux : l'historique recharge apres coupure et le message recu
 * en temps reel doivent etre indiscernables cote client, sinon la reprise d'US-24 fait apparaitre
 * deux affichages differents du meme message.
 *
 * <p>L'instant est transmis en UTC ; la mise en forme dans le fuseau du lecteur est une
 * preoccupation de l'interface (ENF-08).
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
