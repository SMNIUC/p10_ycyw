package com.ycyw.poc.assistance.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifiant d'un interlocuteur : un utilisateur et le role sous lequel il s'exprime.
 *
 * <p>Le contexte Assistance ne connait de l'Identite que cet identifiant. Il n'en importe aucune
 * classe : c'est ce qui rend la frontiere de DA-02 verifiable (voir les tests d'architecture).
 */
public record ParticipantId(UUID userId, ParticipantRole role) {

    public ParticipantId {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
    }

    public static ParticipantId customer(UUID userId) {
        return new ParticipantId(userId, ParticipantRole.CUSTOMER);
    }

    public static ParticipantId agent(UUID userId) {
        return new ParticipantId(userId, ParticipantRole.AGENT);
    }
}
