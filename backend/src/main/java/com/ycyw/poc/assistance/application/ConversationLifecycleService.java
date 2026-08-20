package com.ycyw.poc.assistance.application;

import com.ycyw.poc.assistance.domain.event.ConversationClosed;
import com.ycyw.poc.assistance.domain.event.ConversationOpened;
import com.ycyw.poc.assistance.domain.event.ConversationTaken;
import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.model.exception.ConversationNotFoundException;
import com.ycyw.poc.assistance.domain.port.ChatEventPublisher;
import com.ycyw.poc.assistance.domain.port.ConversationRepository;
import com.ycyw.poc.assistance.domain.port.IdGenerator;
import com.ycyw.poc.assistance.domain.port.TimeProvider;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

/**
 * Cas d'usage du cycle de vie d'une conversation : ouverture (US-23), prise en charge et clôture
 * (US-26).
 *
 * <p>Le service <b>orchestre</b> : il charge l'agrégat, appelle une de ses méthodes, persiste, puis
 * diffuse. Il ne décide d'aucune règle métier — celles-ci sont dans Conversation, et une règle
 * d'architecture vérifiée au build interdit à un service de modifier l'état d'un agrégat autrement
 * qu'en appelant une de ses méthodes.
 */
@RequiredArgsConstructor
public class ConversationLifecycleService {

    private final ConversationRepository conversations;
    private final ChatEventPublisher publisher;
    private final TimeProvider time;
    private final IdGenerator ids;

    /** US-23 : le client ouvre une demande, qui entre dans la file d'attente des agents. */
    public Conversation open(ParticipantId customer, String subject) {
        Instant now = time.now();
        Conversation conversation =
                Conversation.open(new ConversationId(ids.newId()), subject, customer, now);
        conversations.save(conversation);
        publisher.publish(new ConversationOpened(conversation));
        return conversation;
    }

    /**
     * US-26 : un agent prend la demande en charge.
     *
     * <p>Le refus du second agent est porté par l'agrégat, pas par une vérification préalable du
     * service : entre un « est-elle encore libre ? » et l'affectation, une autre instance peut
     * l'avoir prise.
     */
    public Conversation takeOver(ConversationId id, ParticipantId agent) {
        Conversation conversation = load(id);
        Instant now = time.now();
        conversation.takeOver(agent, now);
        conversations.save(conversation);
        publisher.publish(new ConversationTaken(id, agent, now));
        return conversation;
    }

    /** US-26 : la clôture est diffusée pour que le client en soit informé. */
    public Conversation close(ConversationId id, ParticipantId by) {
        Conversation conversation = load(id);
        Instant now = time.now();
        conversation.close(by, now);
        conversations.save(conversation);
        publisher.publish(new ConversationClosed(id, by, now));
        return conversation;
    }

    private Conversation load(ConversationId id) {
        return conversations.findById(id).orElseThrow(() -> new ConversationNotFoundException(id));
    }
}
