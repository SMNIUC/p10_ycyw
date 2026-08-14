package com.ycyw.poc.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du canal temps reel (DA-11).
 *
 * @param relayEnabled {@code true} : la diffusion passe par un broker externe, seul mode compatible
 *     avec plusieurs instances (ENF-19). {@code false} : broker en memoire d'instance, reserve aux
 *     tests et au demarrage sans dependance
 * @param host hote du broker
 * @param port port du relais STOMP du broker (61613 par convention)
 * @param login identifiant applicatif aupres du broker
 * @param passcode secret associe, fourni par l'environnement
 * @param virtualHost hote virtuel du broker, impose par l'application et non par le client
 * @param allowedOrigins origines autorisees a ouvrir la poignee de main WebSocket
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
