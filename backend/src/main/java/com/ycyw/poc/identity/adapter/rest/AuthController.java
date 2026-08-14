package com.ycyw.poc.identity.adapter.rest;

import com.ycyw.poc.identity.AppUser;
import com.ycyw.poc.identity.IdentityApi;
import com.ycyw.poc.shared.security.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ouverture et fermeture de session.
 *
 * <p>Le jeton n'apparait jamais dans le corps de la reponse : il part exclusivement dans un cookie
 * inaccessible au script (DA-18). L'application cliente ne le voit pas, ne le stocke pas, et n'a
 * donc aucun moyen de le divulguer.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IdentityApi identity;
    private final TokenService tokens;
    private final String instanceLabel;

    public AuthController(
            IdentityApi identity,
            TokenService tokens,
            @Value("${poc.instance-label:instance}") String instanceLabel) {
        this.identity = identity;
        this.tokens = tokens;
        this.instanceLabel = instanceLabel;
    }

    @PostMapping("/login")
    public ResponseEntity<SessionView> login(@Valid @RequestBody LoginRequest request) {
        return identity.authenticate(request.email(), request.password())
                .map(this::authenticatedResponse)
                .orElseGet(
                        () ->
                                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(SessionView.anonymous(instanceLabel)));
    }

    /**
     * Etat de la session courante.
     *
     * <p>Accessible sans authentification : c'est l'appel d'amorcage de l'application cliente, celui
     * qui lui fait recevoir le jeton anti-rejeu avant toute ecriture.
     */
    @GetMapping("/session")
    public SessionView session(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return SessionView.anonymous(instanceLabel);
        }
        return new SessionView(
                true,
                jwt.getSubject(),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("role"),
                instanceLabel);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, tokens.expiredCookie().toString())
                .build();
    }

    private ResponseEntity<SessionView> authenticatedResponse(AppUser user) {
        String token = tokens.issueToken(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokens.sessionCookie(token).toString())
                .body(
                        new SessionView(
                                true,
                                user.id().toString(),
                                user.displayName(),
                                user.role().name(),
                                instanceLabel));
    }

    /** Identifiants de connexion. */
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    /**
     * Etat de session renvoye au client.
     *
     * <p>{@code instance} identifie l'instance qui a servi la requete. Ce champ n'a pas de valeur
     * fonctionnelle : il rend visible, dans la demonstration, le fait que le client et l'agent sont
     * bien connectes a deux instances differentes.
     */
    public record SessionView(
            boolean authenticated,
            String userId,
            String displayName,
            String role,
            String instance) {

        static SessionView anonymous(String instance) {
            return new SessionView(false, null, null, null, instance);
        }
    }
}
