package com.ycyw.poc.assistance.adapter.in;

import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.model.ParticipantRole;
import com.ycyw.poc.identity.UserRole;
import com.ycyw.poc.shared.security.AuthenticatedUser;

/**
 * Traduction de l'utilisateur authentifié en participant du contexte Assistance.
 *
 * <p>C'est la frontière entre deux contextes bornés, et le seul endroit du module ou le vocabulaire
 * de l'identité est cité. Les deux énumérations coïncident aujourd'hui ; la traduction reste
 * explicite pour que l'ajout d'un rôle côté Identité — superviseur, administrateur — soit un choix
 * conscient plutôt qu'une propagation automatique.
 */
public final class IdentityTranslation {

    private IdentityTranslation() {}

    public static ParticipantId asParticipant(AuthenticatedUser user) {
        return new ParticipantId(user.id(), asParticipantRole(user.role()));
    }

    private static ParticipantRole asParticipantRole(UserRole role) {
        return switch (role) {
            case CUSTOMER -> ParticipantRole.CUSTOMER;
            case AGENT -> ParticipantRole.AGENT;
        };
    }
}
