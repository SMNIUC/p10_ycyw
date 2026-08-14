package com.ycyw.poc.assistance.adapter.in.ws;

import com.ycyw.poc.assistance.application.ConversationQueryService;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.shared.security.AuthenticatedUser;
import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Controle d'acces des abonnements STOMP.
 *
 * <p><b>Pourquoi ce controle est indispensable.</b> Une destination de diffusion est une chaine de
 * caracteres previsible : {@code /topic/conversations/<identifiant>}. Sans verification, un
 * utilisateur authentifie pourrait s'abonner a la conversation d'un autre et lire ses echanges. La
 * regle appliquee ici est exactement celle de la lecture d'historique — « seul un participant lit
 * une conversation » —, ce qui evite qu'un canal soit plus permissif que l'autre.
 */
@Component
public class SubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    // Le separateur est un point : contrainte de nommage du broker (voir StompChatEventPublisher).
    private static final String CONVERSATION_PREFIX = "/topic/conversations.";
    private static final String AGENT_QUEUE = "/topic/agent-queue";
    private static final String USER_PREFIX = "/user/";

    private final ConversationQueryService queries;

    public SubscriptionAuthorizationInterceptor(ConversationQueryService queries) {
        this.queries = queries;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            AuthenticatedUser user = requireAuthenticated(accessor.getUser());
            if (StompCommand.SUBSCRIBE.equals(command)) {
                authorizeSubscription(accessor.getDestination(), user);
            }
        }
        return message;
    }

    private void authorizeSubscription(String destination, AuthenticatedUser user) {
        if (destination == null) {
            throw new AccessDeniedException("Abonnement sans destination.");
        }
        if (destination.startsWith(USER_PREFIX)) {
            return; // destination privee : resolue par le serveur pour la session courante
        }
        if (destination.equals(AGENT_QUEUE)) {
            if (!user.isAgent()) {
                throw new AccessDeniedException(
                        "La file d'attente est reservee aux agents du service client.");
            }
            return;
        }
        if (destination.startsWith(CONVERSATION_PREFIX)) {
            ConversationId conversationId =
                    ConversationId.of(destination.substring(CONVERSATION_PREFIX.length()));
            if (!queries.isParticipant(conversationId, user.id())) {
                throw new AccessDeniedException("Conversation inaccessible.");
            }
            return;
        }
        throw new AccessDeniedException("Destination inconnue : " + destination);
    }

    private static AuthenticatedUser requireAuthenticated(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Session temps reel non authentifiee.");
        }
        return AuthenticatedUser.from(principal);
    }
}
