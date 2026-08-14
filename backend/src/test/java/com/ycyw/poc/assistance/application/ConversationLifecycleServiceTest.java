package com.ycyw.poc.assistance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ycyw.poc.assistance.application.readmodel.WaitingConversation;
import com.ycyw.poc.assistance.domain.event.ConversationClosed;
import com.ycyw.poc.assistance.domain.event.ConversationOpened;
import com.ycyw.poc.assistance.domain.event.ConversationTaken;
import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationStatus;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.model.exception.ConversationAlreadyTakenException;
import com.ycyw.poc.assistance.support.FixedTimeProvider;
import com.ycyw.poc.assistance.support.InMemoryConversationRepository;
import com.ycyw.poc.assistance.support.InMemoryMessageRepository;
import com.ycyw.poc.assistance.support.RecordingChatEventPublisher;
import com.ycyw.poc.assistance.support.SequentialIdGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cycle de vie d'une conversation")
class ConversationLifecycleServiceTest {

    private static final Instant DEBUT = Instant.parse("2026-03-01T09:00:00Z");
    private static final ParticipantId ALICE = ParticipantId.customer(UUID.randomUUID());
    private static final ParticipantId SAM = ParticipantId.agent(UUID.randomUUID());
    private static final ParticipantId LEA = ParticipantId.agent(UUID.randomUUID());

    private InMemoryConversationRepository conversations;
    private RecordingChatEventPublisher publisher;
    private FixedTimeProvider time;
    private ConversationLifecycleService service;
    private ConversationQueryService queries;

    @BeforeEach
    void setUp() {
        conversations = new InMemoryConversationRepository();
        publisher = new RecordingChatEventPublisher();
        time = new FixedTimeProvider(DEBUT);
        service =
                new ConversationLifecycleService(
                        conversations, publisher, time, new SequentialIdGenerator());
        queries =
                new ConversationQueryService(conversations, new InMemoryMessageRepository(), time);
    }

    @Test
    @DisplayName("l'ouverture met la demande en attente et l'annonce a la file des agents")
    void ouverture() {
        Conversation conversation = service.open(ALICE, "Modifier ma location");

        assertThat(conversation.status()).isEqualTo(ConversationStatus.WAITING);
        assertThat(conversations.findWaiting()).containsExactly(conversation);
        assertThat(publisher.publishedOfType(ConversationOpened.class)).hasSize(1);
    }

    @Test
    @DisplayName("la file d'attente est ordonnee par arrivee et porte le temps d'attente (US-26)")
    void fileDAttente() {
        service.open(ALICE, "Premiere demande");
        time.advance(Duration.ofMinutes(3));
        service.open(ParticipantId.customer(UUID.randomUUID()), "Seconde demande");
        time.advance(Duration.ofMinutes(1));

        List<WaitingConversation> file = queries.waitingQueue();

        assertThat(file).extracting(WaitingConversation::subject)
                .containsExactly("Premiere demande", "Seconde demande");
        assertThat(file.get(0).waitingFor()).isEqualTo(Duration.ofMinutes(4));
        assertThat(file.get(1).waitingFor()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("la prise en charge retire la demande de la file et est diffusee")
    void priseEnCharge() {
        Conversation conversation = service.open(ALICE, "Modifier ma location");

        service.takeOver(conversation.id(), SAM);

        assertThat(conversations.findWaiting()).isEmpty();
        assertThat(publisher.publishedOfType(ConversationTaken.class)).hasSize(1);
    }

    @Test
    @DisplayName("un second agent est refuse, et rien n'est diffuse")
    void priseEnChargeConcurrente() {
        Conversation conversation = service.open(ALICE, "Modifier ma location");
        service.takeOver(conversation.id(), SAM);

        assertThatThrownBy(() -> service.takeOver(conversation.id(), LEA))
                .isInstanceOf(ConversationAlreadyTakenException.class);

        assertThat(publisher.publishedOfType(ConversationTaken.class)).hasSize(1);
    }

    @Test
    @DisplayName("la cloture est diffusee pour que le client en soit informe")
    void cloture() {
        Conversation conversation = service.open(ALICE, "Modifier ma location");
        service.takeOver(conversation.id(), SAM);

        service.close(conversation.id(), SAM);

        assertThat(conversations.findById(conversation.id()).orElseThrow().status())
                .isEqualTo(ConversationStatus.CLOSED);
        assertThat(publisher.publishedOfType(ConversationClosed.class)).hasSize(1);
    }
}
