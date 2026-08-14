package com.ycyw.poc.shared.security;

import com.ycyw.poc.identity.UserRole;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utilisateur authentifie, tel que les adaptateurs primaires le manipulent.
 *
 * <p>Le meme type sert au canal REST et au canal WebSocket : les deux points d'entree appliquent
 * ainsi exactement la meme lecture de l'identite, et un ecart entre les deux devient impossible.
 */
public record AuthenticatedUser(UUID id, String displayName, UserRole role) {

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("name"),
                UserRole.valueOf(jwt.getClaimAsString("role")));
    }

    /** Lecture depuis le principal porte par la session WebSocket. */
    public static AuthenticatedUser from(Principal principal) {
        if (principal instanceof AbstractAuthenticationToken token
                && token.getPrincipal() instanceof Jwt jwt) {
            return from(jwt);
        }
        throw new IllegalStateException("Session non authentifiee.");
    }

    public boolean isAgent() {
        return role == UserRole.AGENT;
    }
}
