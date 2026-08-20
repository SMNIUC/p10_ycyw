package com.ycyw.poc.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du canal temps réel (DA-11).
 *
 * @param relayEnabled {@code true} : la diffusion passe par un broker externe, seul mode compatible
 *     avec plusieurs instances (ENF-19). {@code false} : broker en mémoire d'instance, réservé aux
 *     tests et au démarrage sans dépendance
 * @param host hôte du broker
 * @param port port du relais STOMP du broker (61613 par convention)
 * @param login identifiant applicatif auprès du broker
 * @param passcode secret associe, fourni par l'environnement
 * @param virtualHost hôte virtuel du broker, imposé par l'application et non par le client
 * @param allowedOrigins origines autorisées à ouvrir la poignée de main WebSocket
 */
@ConfigurationProperties(prefix = "poc.broker")
public record BrokerProperties(
        boolean relayEnabled,
        String host,
        int port,
        String login,
        String passcode,
        String virtualHost,
        String[] allowedOrigins) {

    public BrokerProperties {
        host = host == null ? "localhost" : host;
        port = port == 0 ? 61613 : port;
        virtualHost = virtualHost == null ? "/" : virtualHost;
        allowedOrigins = allowedOrigins == null ? new String[] {"http://localhost:4200"} : allowedOrigins;
    }
}
