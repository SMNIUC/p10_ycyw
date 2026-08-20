package com.ycyw.poc.assistance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ycyw.poc.assistance.domain.model.exception.ConversationAlreadyTakenException;
import com.ycyw.poc.assistance.domain.model.exception.ConversationClosedException;
import com.ycyw.poc.assistance.domain.model.exception.NotAParticipantException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Règles de l'agrégat Conversation.
 *
 * <p>Aucune base, aucun broker, aucun contexte applicatif : c'est précisément ce que l'architecture
 * hexagonale rend possible (DA-05). Ces tests s'executent en quelques millisecondes et n'échouent
 * jamais pour une raison d'infrastructure.
 */
@DisplayName("Agrégat Conversation")
class ConversationTest {

    private static final Instant OPENED_AT = Instant.parse("2026-03-01T09:00:00Z");
    private static final ParticipantId ALICE = ParticipantId.customer(UUID.randomUUID());
    private static final ParticipantId SAM = ParticipantId.agent(UUID.randomUUID());
    private static final ParticipantId AUTRE_AGENT = ParticipantId.agent(UUID.randomUUID());

    private Conversation uneConversationEnAttente() {
        return Conversation.open(
                new ConversationId(UUID.randomUUID()), "Modifier ma reservation", ALICE, OPENED_AT);
    }

    @Test
    @DisplayName("s'ouvre en attente, avec le client comme unique participant")
    void ouverture() {
        Conversation conversation = uneConversationEnAttente();

        assertThat(conversation.status()).isEqualTo(ConversationStatus.WAITING);
        assertThat(conversation.agent()).isEmpty();
        assertThat(conversation.participants()).hasSize(1);
        assertThat(conversation.hasParticipant(ALICE.userId())).isTrue();
    }

    @Nested
    @DisplayName("prise en charge (US-26)")
    class PriseEnCharge {

        @Test
        @DisplayName("ajoute l'agent aux participants et fait passer la conversation en cours")
        void priseEnCharge() {
            Conversation conversation = uneConversationEnAttente();

            conversation.takeOver(SAM, OPENED_AT.plusSeconds(120));

            assertThat(conversation.status()).isEqualTo(ConversationStatus.TAKEN);
            assertThat(conversation.agent()).contains(SAM);
            assertThat(conversation.hasParticipant(SAM.userId())).isTrue();
        }

        @Test
        @DisplayName("est refusée à un second agent : la demande n'est plus proposée")
        void secondAgentRefuse() {
            Conversation conversation = uneConversationEnAttente();
            conversation.takeOver(SAM, OPENED_AT.plusSeconds(120));

            assertThatThrownBy(() -> conversation.takeOver(AUTRE_AGENT, OPENED_AT.plusSeconds(121)))
                    .isInstanceOf(ConversationAlreadyTakenException.class);

            assertThat(conversation.agent()).contains(SAM);
        }

        @Test
        @DisplayName("n'est pas ouverte à un client")
        void clientNePrendPasEnCharge() {
            Conversation conversation = uneConversationEnAttente();

            assertThatThrownBy(() -> conversation.takeOver(ALICE, OPENED_AT))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("envoi de message")
    class Envoi {

        @Test
        @DisplayName("est refusé à qui ne participe pas à la conversation")
        void tiersRefuse() {
            Conversation conversation = uneConversationEnAttente();
            ParticipantId intrus = ParticipantId.customer(UUID.randomUUID());

            assertThatThrownBy(
                            () ->
                                    conversation.post(
                                            new MessageId(UUID.randomUUID()),
                                            intrus,
                                            new MessageBody("Bonjour"),
                                            OPENED_AT))
                    .isInstanceOf(NotAParticipantException.class);
        }

        @Test
        @DisplayName("est refusé après clôture")
        void conversationCloturee() {
            Conversation conversation = uneConversationEnAttente();
            conversation.close(ALICE, OPENED_AT.plusSeconds(600));

            assertThatThrownBy(
                            () ->
                                    conversation.post(
                                            new MessageId(UUID.randomUUID()),
                                            ALICE,
                                            new MessageBody("Encore une question"),
                                            OPENED_AT.plusSeconds(700)))
                    .isInstanceOf(ConversationClosedException.class);
        }

        @Test
        @DisplayName("produit un message à l'état « envoyé »")
        void messageEnvoye() {
            Conversation conversation = uneConversationEnAttente();

            Message message =
                    conversation.post(
                            new MessageId(UUID.randomUUID()),
                            ALICE,
                            new MessageBody("Bonjour, je souhaite decaler ma location."),
                            OPENED_AT);

            assertThat(message.state()).isEqualTo(DeliveryState.SENT);
            assertThat(message.author()).isEqualTo(ALICE);
            assertThat(message.conversationId()).isEqualTo(conversation.id());
        }
    }

    @Nested
    @DisplayName("marqueur de lecture")
    class Lecture {

        @Test
        @DisplayName("avance, et ne recule jamais")
        void marqueurMonotone() {
            Conversation conversation = uneConversationEnAttente();

            conversation.markReadBy(ALICE, OPENED_AT.plusSeconds(300));
            conversation.markReadBy(ALICE, OPENED_AT.plusSeconds(100));

            assertThat(conversation.participant(ALICE.userId()).orElseThrow().lastReadAt())
                    .isEqualTo(OPENED_AT.plusSeconds(300));
        }

        @Test
        @DisplayName("est refusé à qui ne participe pas")
        void tiersRefuse() {
            Conversation conversation = uneConversationEnAttente();

            assertThatThrownBy(
                            () ->
                                    conversation.markReadBy(
                                            ParticipantId.agent(UUID.randomUUID()), OPENED_AT))
                    .isInstanceOf(NotAParticipantException.class);
        }
    }
}
