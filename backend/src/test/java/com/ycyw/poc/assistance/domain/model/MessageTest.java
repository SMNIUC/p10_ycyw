package com.ycyw.poc.assistance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Message")
class MessageTest {

    private static final ParticipantId ALICE = ParticipantId.customer(UUID.randomUUID());
    private static final ParticipantId SAM = ParticipantId.agent(UUID.randomUUID());

    private Message unMessageDAlice() {
        return Conversation.open(
                        new ConversationId(UUID.randomUUID()),
                        "Question",
                        ALICE,
                        Instant.parse("2026-03-01T09:00:00Z"))
                .post(
                        new MessageId(UUID.randomUUID()),
                        ALICE,
                        new MessageBody("Bonjour"),
                        Instant.parse("2026-03-01T09:00:00Z"));
    }

    @Test
    @DisplayName("progresse d'envoyé à remis puis lu")
    void progression() {
        Message message = unMessageDAlice();

        message.markDelivered();
        assertThat(message.state()).isEqualTo(DeliveryState.DELIVERED);

        message.markRead();
        assertThat(message.state()).isEqualTo(DeliveryState.READ);
    }

    @Test
    @DisplayName("ne redescend jamais d'état, quel que soit l'ordre d'arrivée des accusés")
    void progressionMonotone() {
        Message message = unMessageDAlice();

        message.markRead();
        message.markDelivered(); // accuse de reception arrive en retard

        assertThat(message.state()).isEqualTo(DeliveryState.READ);
    }

    @Test
    @DisplayName("distingue son destinataire de son auteur")
    void destinataire() {
        Message message = unMessageDAlice();

        assertThat(message.isAddressedTo(SAM)).isTrue();
        assertThat(message.isAddressedTo(ALICE)).isFalse();
    }

    @Test
    @DisplayName("refuse un contenu vide")
    void contenuVide() {
        assertThatThrownBy(() -> new MessageBody("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuse un contenu au-delà de la limite")
    void contenuTropLong() {
        String trop = "a".repeat(MessageBody.MAX_LENGTH + 1);

        assertThatThrownBy(() -> new MessageBody(trop))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
