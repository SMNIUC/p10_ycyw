package com.ycyw.poc.assistance.adapter.in.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Le module Assistance branche son propre controle d'abonnement sur le canal entrant.
 *
 * <p>La configuration du transport reste partagee ; la regle d'acces, elle, appartient au module qui
 * detient les conversations. Un module ajoute demain apporterait la sienne sans modifier la
 * configuration commune.
 */
@Configuration
public class AssistanceWebSocketConfigurer implements WebSocketMessageBrokerConfigurer {

    private final SubscriptionAuthorizationInterceptor subscriptionAuthorization;

    public AssistanceWebSocketConfigurer(
            SubscriptionAuthorizationInterceptor subscriptionAuthorization) {
        this.subscriptionAuthorization = subscriptionAuthorization;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(subscriptionAuthorization);
    }
}
