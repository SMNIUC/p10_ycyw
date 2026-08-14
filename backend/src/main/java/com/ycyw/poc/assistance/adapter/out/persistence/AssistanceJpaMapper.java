package com.ycyw.poc.assistance.adapter.out.persistence;

import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.Message;
import com.ycyw.poc.assistance.domain.model.MessageBody;
import com.ycyw.poc.assistance.domain.model.MessageId;
import com.ycyw.poc.assistance.domain.model.Participant;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traduction entre le modele du domaine et le modele de persistance.
 *
 * <p>Ce mappage manuel est le prix de l'independance du domaine. Il est assume : c'est le seul
 * endroit ou les deux representations se rencontrent, et il tient dans un fichier.
 */
final class AssistanceJpaMapper {

    private AssistanceJpaMapper() {}

    static Conversation toDomain(ConversationEntity entity) {
        List<Participant> participants =
                entity.getParticipants().stream()
                        .map(
                                p ->
                                        Participant.rehydrate(
                                                new ParticipantId(p.getKey().getUserId(), p.getRole()),
                                                p.getJoinedAt(),
                                                p.getLastReadAt()))
                        .toList();

        ParticipantId customer =
                participants.stream()
                        .map(Participant::id)
                        .filter(id -> id.userId().equals(entity.getCustomerId()))
                        .findFirst()
                        .orElseGet(() -> ParticipantId.customer(entity.getCustomerId()));

        ParticipantId agent =
                entity.getAgentId() == null ? null : ParticipantId.agent(entity.getAgentId());

        return Conversation.rehydrate(
                new ConversationId(entity.getId()),
                entity.getSubject(),
                customer,
                agent,
                entity.getStatus(),
                entity.getOpenedAt(),
                entity.getTakenAt(),
                entity.getClosedAt(),
                participants);
    }

    /**
     * Reporte l'etat de l'agregat sur l'entite existante — plutot que de la recreer — afin de
     * conserver le numero de version du verrou optimiste.
     */
    static void apply(Conversation conversation, ConversationEntity entity) {
        entity.setId(conversation.id().value());
        entity.setSubject(conversation.subject());
        entity.setStatus(conversation.status());
        entity.setCustomerId(conversation.customer().userId());
        entity.setAgentId(conversation.agent().map(ParticipantId::userId).orElse(null));
        entity.setOpenedAt(conversation.openedAt());
        entity.setTakenAt(conversation.takenAt());
        entity.setClosedAt(conversation.closedAt());

        Map<java.util.UUID, ParticipantEntity> existing =
                entity.getParticipants().stream()
                        .collect(
                                Collectors.toMap(
                                        p -> p.getKey().getUserId(), Function.identity()));

        for (Participant participant : conversation.participants()) {
            ParticipantEntity row = existing.get(participant.id().userId());
            if (row == null) {
                row =
                        new ParticipantEntity(
                                entity,
                                new ParticipantKey(
                                        conversation.id().value(), participant.id().userId()));
                entity.getParticipants().add(row);
            }
            row.setRole(participant.id().role());
            row.setJoinedAt(participant.joinedAt());
            row.setLastReadAt(participant.lastReadAt());
        }
    }

    static Message toDomain(MessageEntity entity) {
        return Message.rehydrate(
                new MessageId(entity.getId()),
                new ConversationId(entity.getConversationId()),
                new ParticipantId(entity.getAuthorId(), entity.getAuthorRole()),
                new MessageBody(entity.getBody()),
                entity.getSentAt(),
                entity.getState());
    }

    static void apply(Message message, MessageEntity entity) {
        entity.setId(message.id().value());
        entity.setConversationId(message.conversationId().value());
        entity.setAuthorId(message.author().userId());
        entity.setAuthorRole(message.author().role());
        entity.setBody(message.body().text());
        entity.setSentAt(message.sentAt());
        entity.setState(message.state());
    }
}
