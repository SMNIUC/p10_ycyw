package com.ycyw.poc.assistance.adapter.in;

import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.model.ParticipantRole;
import com.ycyw.poc.identity.UserRole;
import com.ycyw.poc.shared.security.AuthenticatedUser;

/**
 * Traduction de l'utilisateur authentifie en participant du contexte Assistance.
 *
 * <p>C'est la frontiere entre deux contextes bornes, et le seul endroit du module ou le vocabulaire
 * de l'Identite est cite. Les deux enumerations coincident aujourd'hui ; la traduction reste
 * explicite pour que l'ajout d'un role cote Identite — superviseur, administrateur — soit un choix
 * conscient plutot qu'une propagation automatique.
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
