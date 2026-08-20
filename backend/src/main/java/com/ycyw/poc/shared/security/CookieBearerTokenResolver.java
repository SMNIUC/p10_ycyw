package com.ycyw.poc.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Le jeton est lu dans le cookie, jamais dans un en-tête {@code Authorization}.
 *
 * <p>Conséquence directe de DA-18 : si le jeton pouvait aussi être présenté en en-tête, un script
 * de page devrait pouvoir le lire pour le poser — et l'intérêt du cookie inaccessible au script
 * disparaîtrait. La poignée de main WebSocket bénéficie du même mécanisme : le navigateur joint le
 * cookie à la requête de bascule de protocole, sans qu'aucun code n'ait à manipuler le jeton.
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

    /** Utilitaire de lecture côté poignée de main WebSocket. */
    public Optional<String> read(HttpServletRequest request) {
        return Optional.ofNullable(resolve(request));
    }
}
