package com.ycyw.poc.assistance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ycyw.poc.assistance.domain.event.ConversationRead;
import com.ycyw.poc.assistance.domain.event.MessagePosted;
import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.DeliveryState;
import com.ycyw.poc.assistance.domain.model.Message;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.model.exception.NotAParticipantException;
import com.ycyw.poc.assistance.domain.port.ChatEventPublisher;
import com.ycyw.poc.assistance.domain.port.ConversationRepository;
import com.ycyw.poc.assistance.domain.port.MessageRepository;
import com.ycyw.poc.assistance.support.FixedTimeProvider;
import com.ycyw.poc.assistance.support.InMemoryConversationRepository;
import com.ycyw.poc.assistance.support.InMemoryMessageRepository;
import com.ycyw.poc.assistance.support.RecordingChatEventPublisher;
import com.ycyw.poc.assistance.support.SequentialIdGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@DisplayName("Cas d'usage de la messagerie")
class MessagingServiceTest {

    private static final Instant DEBUT = Instant.parse("2026-03-01T09:00:00Z");
    private static final ParticipantId ALICE = ParticipantId.customer(UUID.randomUUID());
    private static final ParticipantId SAM = ParticipantId.agent(UUID.randomUUID());

    private InMemoryConversationRepository conversations;
    private InMemoryMessageRepository messages;
    private RecordingChatEventPublisher publisher;
    private FixedTimeProvider time;
    private MessagingService service;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversations = new InMemoryConversationRepository();
        messages = new InMemoryMessageRepository();
        publisher = new RecordingChatEventPublisher();
        time = new FixedTimeProvider(DEBUT);
        service =
                new MessagingService(
                        conversations, messages, publisher, time, new SequentialIdGenerator());

        conversation =
                Conversation.open(
                        new ConversationId(UUID.randomUUID()), "Modifier ma location", ALICE, DEBUT);
        conversation.takeOver(SAM, DEBUT.plusSeconds(60));
        conversations.save(conversation);
    }

    @Test
    @DisplayName("persiste le message AVANT de le diffuser — condition de la reprise d'US-24")
    void persisteAvantDeDiffuser() {
        // Ce test est le seul de la suite a utiliser des doublures verifiables plutot que des
        // depots en memoire : ce qu'il controle n'est pas un resultat, c'est un ORDRE. Un message
        // diffuse mais non enregistre disparaitrait a la reconnexion du client — la conversation
        // ne reprendrait donc pas « sans perte de message ».
        ConversationRepository conversationsMock = mock(ConversationRepository.class);
        MessageRepository messagesMock = mock(MessageRepository.class);
        ChatEventPublisher publisherMock = mock(ChatEventPublisher.class);
        when(conversationsMock.findById(any())).thenReturn(Optional.of(conversation));

        new MessagingService(
                        conversationsMock,
                        messagesMock,
                        publisherMock,
                        time,
                        new SequentialIdGenerator())
                .post(conversation.id(), ALICE, "Bonjour");

        InOrder ordre = inOrder(messagesMock, publisherMock);
        ordre.verify(messagesMock).save(any(Message.class));
        ordre.verify(publisherMock).publish(any(MessagePosted.class));
        ordre.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("enregistre le message a l'etat « envoye » et le diffuse")
    void envoi() {
        Message message = service.post(conversation.id(), ALICE, "Bonjour, je dois decaler.");

        assertThat(messages.findByConversation(conversation.id())).containsExactly(message);
        assertThat(message.state()).isEqualTo(DeliveryState.SENT);
        assertThat(publisher.publishedOfType(MessagePosted.class))
                .singleElement()
                .satisfies(event -> assertThat(event.message().id()).isEqualTo(message.id()));
    }

    @Test
    @DisplayName("refuse un message d'un tiers a la conversation")
    void tiersRefuse() {
        ParticipantId intrus = ParticipantId.customer(UUID.randomUUID());

        assertThatThrownBy(() -> service.post(conversation.id(), intrus, "Bonjour"))
                .isInstanceOf(NotAParticipantException.class);

        assertThat(messages.findByConversation(conversation.id())).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("l'accuse de reception fait passer le message a « remis »")
    void accuseDeReception() {
        Message message = service.post(conversation.id(), ALICE, "Bonjour");

        service.acknowledgeDelivery(conversation.id(), message.id(), SAM);

        assertThat(messages.findById(message.id()).orElseThrow().state())
                .isEqualTo(DeliveryState.DELIVERED);
    }

    @Test
    @DisplayName("l'auteur ne peut pas accuser reception de son propre message")
    void accuseDeReceptionParLAuteur() {
        Message message = service.post(conversation.id(), ALICE, "Bonjour");

        assertThat(service.acknowledgeDelivery(conversation.id(), message.id(), ALICE)).isEmpty();
        assertThat(messages.findById(message.id()).orElseThrow().state())
                .isEqualTo(DeliveryState.SENT);
    }

    @Test
    @DisplayName("la lecture fait passer a « lu » les seuls messages recus, et avance le marqueur")
    void lecture() {
        Message dAlice = service.post(conversation.id(), ALICE, "Bonjour");
        time.advance(Duration.ofSeconds(30));
        Message deSam = service.post(conversation.id(), SAM, "Bonjour, je vous ecoute.");
        time.advance(Duration.ofSeconds(30));

        Optional<ConversationRead> evenement = service.markRead(conversation.id(), SAM);

        assertThat(messages.findById(dAlice.id()).orElseThrow().state())
                .isEqualTo(DeliveryState.READ);
        assertThat(messages.findById(deSam.id()).orElseThrow().state())
                .isEqualTo(DeliveryState.SENT);
        assertThat(evenement).isPresent();
        assertThat(evenement.orElseThrow().readMessages()).containsExactly(dAlice.id());
        assertThat(
                        conversations
                                .findById(conversation.id())
                                .orElseThrow()
                                .participant(SAM.userId())
                                .orElseThrow()
                                .lastReadAt())
                .isEqualTo(DEBUT.plus(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("ne diffuse aucun accuse de lecture s'il n'y avait rien a lire")
    void lectureSansNouveaute() {
        assertThat(service.markRead(conversation.id(), SAM)).isEmpty();
        assertThat(publisher.publishedOfType(ConversationRead.class)).isEmpty();
    }

    @Test
    @DisplayName("les messages non lus alimentent le compteur du destinataire")
    void compteurDeNonLus() {
        service.post(conversation.id(), ALICE, "Premier");
        time.advance(Duration.ofSeconds(10));
        service.post(conversation.id(), ALICE, "Second");

        List<Message> nonLus = messages.findUnreadFor(conversation.id(), SAM);

        assertThat(nonLus).hasSize(2);
        assertThat(messages.findUnreadFor(conversation.id(), ALICE)).isEmpty();
    }
}
