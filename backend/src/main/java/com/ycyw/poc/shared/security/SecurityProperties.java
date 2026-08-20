package com.ycyw.poc.shared.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de sécurité, tous externalisés.
 *
 * <p>Aucune valeur secrète n'a de valeur par défaut : l'application refuse de démarrer si le secret
 * de signature n'est pas fourni par l'environnement. C'est la réponse au constat C-13 de l'audit —
 * des secrets déposés en fichiers de configuration sur les serveurs européens.
 *
 * @param jwtSecret secret de signature HMAC, 32 octets minimum
 * @param tokenTtl durée de validité du jeton d'accès — courte par construction
 * @param cookieName nom du cookie porteur du jeton
 * @param cookiePath chemin du cookie ; distinct par instance dans la démonstration, ce qui permet
 *     d'ouvrir deux sessions différentes dans le même navigateur
 * @param cookieSecure {@code true} dès que le service est servi en HTTPS (toujours en production)
 */
@ConfigurationProperties(prefix = "poc.security")
public record SecurityProperties(
        String jwtSecret,
        Duration tokenTtl,
        String cookieName,
        String cookiePath,
        boolean cookieSecure) {

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "poc.security.jwt-secret est obligatoire et doit faire au moins 32 octets.");
        }
        tokenTtl = tokenTtl == null ? Duration.ofMinutes(30) : tokenTtl;
        cookieName = cookieName == null ? "ycyw_session" : cookieName;
        cookiePath = cookiePath == null ? "/" : cookiePath;
    }
}
