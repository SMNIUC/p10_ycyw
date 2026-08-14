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
 * Canal temps reel : WebSocket comme transport, STOMP comme protocole (DA-11).
 *
 * <p><b>Le basculement de broker est la decision que cette classe rend visible.</b>
 *
 * <ul>
 *   <li>{@code relay-enabled: true} — la diffusion est deleguee a un <b>broker externe</b>. Une
 *       trame publiee par l'instance 1 revient a toutes les instances abonnees, y compris
 *       l'instance 3 ou l'agent est connecte. C'est la configuration de reference.
 *   <li>{@code relay-enabled: false} — broker <b>en memoire d'instance</b>. Suffisant pour un test
 *       ou un demarrage sans dependance, et <b>inutilisable des la seconde instance</b> : chaque
 *       instance ne diffuserait qu'a ses propres abonnes, et US-24 echouerait sans erreur visible.
 * </ul>
 *
 * <p>Aucune ligne du domaine ni des adaptateurs ne change entre les deux modes : c'est la
 * definition meme d'un port (DA-06).
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
        // L'authentification de la poignee de main est portee par la chaine de filtres de securite :
        // le cookie de session accompagne la requete de bascule de protocole, et le principal ainsi
        // etabli est attache a la session WebSocket.
        registry.addEndpoint("/ws").setAllowedOriginPatterns(properties.allowedOrigins());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (properties.relayEnabled()) {
            log.info(
                    "Diffusion temps reel : relais STOMP vers le broker externe {}:{}",
                    properties.host(),
                    properties.port());
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(properties.host())
                    .setRelayPort(properties.port())
                    // L'hote virtuel est impose par l'application. Sans cette ligne, celui annonce
                    // par le client serait transmis au broker, qui refuserait la connexion — ou
                    // ouvrirait un espace que le client aurait choisi lui-meme.
                    .setVirtualHost(properties.virtualHost())
                    .setClientLogin(properties.login())
                    .setClientPasscode(properties.passcode())
                    .setSystemLogin(properties.login())
                    .setSystemPasscode(properties.passcode());
        } else {
            log.warn(
                    "Diffusion temps reel : broker en memoire d'instance. "
                            + "Mode de test uniquement — aucune diffusion entre instances.");
            registry.enableSimpleBroker("/topic", "/queue");
        }
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
