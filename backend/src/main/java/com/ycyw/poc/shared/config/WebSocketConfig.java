package com.ycyw.poc.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal temps réel : WebSocket comme transport, STOMP comme protocole (DA-11).
 *
 * <p><b>Le basculement de broker est la décision que cette classe rend visible.</b>
 *
 * <ul>
 *   <li>{@code relay-enabled: true} — la diffusion est déléguée à un <b>broker externe</b>. Une
 *       trame publiée par l'instance 1 revient à toutes les instances abonnées, y compris
 *       l'instance 3 où l'agent est connecté. C'est la configuration de référence.
 *   <li>{@code relay-enabled: false} — broker <b>en mémoire d'instance</b>. Suffisant pour un test
 *       ou un démarrage sans dépendance, et <b>inutilisable dès la seconde instance</b> : chaque
 *       instance ne diffuserait qu'à ses propres abonnés, et US-24 échouerait sans erreur visible.
 * </ul>
 *
 * <p>Aucune ligne du domaine ni des adaptateurs ne change entre les deux modes : c'est la
 * définition même d'un port (DA-06).
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(BrokerProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final BrokerProperties properties;

    public WebSocketConfig(BrokerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // L'authentification de la poignée de main est portée par la chaîne de filtres de sécurité :
        // le cookie de session accompagne la requête de bascule de protocole, et le principal ainsi
        // établi est attaché à la session WebSocket.
        registry.addEndpoint("/ws").setAllowedOriginPatterns(properties.allowedOrigins());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (properties.relayEnabled()) {
            log.info(
                    "Diffusion temps réel : relais STOMP vers le broker externe {}:{}",
                    properties.host(),
                    properties.port());
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(properties.host())
                    .setRelayPort(properties.port())
                    // L'hôte virtuel est imposé par l'application. Sans cette ligne, celui annoncé
                    // par le client serait transmis au broker, qui refuserait la connexion — ou
                    // ouvrirait un espace que le client aurait choisi lui-même.
                    .setVirtualHost(properties.virtualHost())
                    .setClientLogin(properties.login())
                    .setClientPasscode(properties.passcode())
                    .setSystemLogin(properties.login())
                    .setSystemPasscode(properties.passcode());
        } else {
            log.warn(
                    "Diffusion temps réel : broker en mémoire d'instance. "
                            + "Mode de test uniquement — aucune diffusion entre instances.");
            registry.enableSimpleBroker("/topic", "/queue");
        }
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
