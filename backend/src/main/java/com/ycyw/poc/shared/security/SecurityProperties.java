package com.ycyw.poc.shared.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametres de securite, tous externalises.
 *
 * <p>Aucune valeur secrete n'a de valeur par defaut : l'application refuse de demarrer si le secret
 * de signature n'est pas fourni par l'environnement. C'est la reponse au constat F-13 de l'audit —
 * des secrets deposes en fichiers de configuration sur les serveurs europeens.
 *
 * @param jwtSecret secret de signature HMAC, 32 octets minimum
 * @param tokenTtl duree de validite du jeton d'acces — courte par construction
 * @param cookieName nom du cookie porteur du jeton
 * @param cookiePath chemin du cookie ; distinct par instance dans la demonstration, ce qui permet
 *     d'ouvrir deux sessions differentes dans le meme navigateur
 * @param cookieSecure {@code true} des que le service est servi en HTTPS (toujours en production)
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
