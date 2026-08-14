package com.ycyw.poc.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Le jeton est lu dans le cookie, jamais dans un en-tete {@code Authorization}.
 *
 * <p>Consequence directe de DA-18 : si le jeton pouvait aussi etre presente en en-tete, un script
 * de page devrait pouvoir le lire pour le poser — et l'interet du cookie inaccessible au script
 * disparaitrait. La poignee de main WebSocket beneficie du meme mecanisme : le navigateur joint le
 * cookie a la requete de bascule de protocole, sans qu'aucun code n'ait a manipuler le jeton.
 */
@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final SecurityProperties properties;

    public CookieBearerTokenResolver(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    /** Utilitaire de lecture cote poignee de main WebSocket. */
    public Optional<String> read(HttpServletRequest request) {
        return Optional.ofNullable(resolve(request));
    }
}
