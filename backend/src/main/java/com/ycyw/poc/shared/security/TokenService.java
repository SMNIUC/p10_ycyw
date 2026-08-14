package com.ycyw.poc.shared.security;

import com.ycyw.poc.identity.AppUser;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emission du jeton d'acces et du cookie qui le transporte (DA-18).
 *
 * <p>Le jeton n'est jamais remis au script de page : il part dans un cookie {@code HttpOnly}, ce
 * qui le rend illisible par une injection de script — contrairement a un stockage navigateur.
 * {@code SameSite=Lax} limite son envoi depuis un site tiers.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final SecurityProperties properties;

    public TokenService(JwtEncoder encoder, SecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public String issueToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer("ycyw-poc")
                        .issuedAt(now)
                        .expiresAt(now.plus(properties.tokenTtl()))
                        .subject(user.id().toString())
                        .claim("role", user.role().name())
                        .claim("name", user.displayName())
                        .build();
        // L'algorithme de signature doit etre declare explicitement : sans en-tete, l'encodeur
        // choisit une signature asymetrique, incompatible avec le secret partage utilise ici.
        JwsHeader entete = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(entete, claims)).getTokenValue();
    }

    public ResponseCookie sessionCookie(String token) {
        return baseCookie(token).maxAge(properties.tokenTtl()).build();
    }

    /** Cookie vide et immediatement expire : la deconnexion ne laisse rien derriere elle. */
    public ResponseCookie expiredCookie() {
        return baseCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Lax")
                .path(properties.cookiePath());
    }
}
