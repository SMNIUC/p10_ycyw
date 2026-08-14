package com.ycyw.poc.assistance.adapter.out.broker;

import com.ycyw.poc.assistance.adapter.wire.ChatEventPayload;
import com.ycyw.poc.assistance.domain.event.ChatEvent;
import com.ycyw.poc.assistance.domain.event.ConversationClosed;
import com.ycyw.poc.assistance.domain.event.ConversationOpened;
import com.ycyw.poc.assistance.domain.event.ConversationTaken;
import com.ycyw.poc.assistance.domain.port.ChatEventPublisher;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptateur secondaire de diffusion.
 *
 * <p><b>Le point que cette preuve de concept doit demontrer.</b> En configuration de production, le
 * modele de messagerie est branche sur un <b>relais vers un broker externe</b> (RabbitMQ, DA-11) :
 * la trame publiee ici sort de l'instance, atteint le broker, et revient vers <i>toutes</i> les
 * instances abonnees a la destination. Sans lui, le broker integre au framework vit en memoire
 * d'instance : un client connecte a l'instance 1 et un agent connecte a l'instance 3 ne se verraient
 * jamais, et US-24 serait inapplicable des la seconde instance.
 *
 * <p>Ce code, lui, ne change pas selon le mode : c'est la configuration qui bascule
 * ({@code poc.broker.relay-enabled}), pas l'adaptateur.
 */
@Component
public class StompChatEventPublisher implements ChatEventPublisher {

    /**
     * Destination d'une conversation : ses deux participants y sont abonnes.
     *
     * <p>Le separateur est un point, et non une barre oblique. Ce n'est pas une coquetterie : le
     * broker interprete {@code /topic/<cle>} comme une cle de routage unique, et refuse une
     * destination comportant un second niveau de chemin. C'est le genre de contrainte qu'une
     * demonstration sur broker en memoire ne revele jamais — et qui apparait au premier
     * deploiement reel.
     */
    public static final String CONVERSATION_DESTINATION = "/topic/conversations.";

    /** Destination de la file d'attente : tous les agents connectes y sont abonnes (US-26). */
    public static final String AGENT_QUEUE_DESTINATION = "/topic/agent-queue";

    private static final Logger log = LoggerFactory.getLogger(StompChatEventPublisher.class);

    private final SimpMessagingTemplate template;

    public StompChatEventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void publish(ChatEvent event) {
        ChatEventPayload payload = ChatEventPayload.from(event, Instant.now());

        template.convertAndSend(CONVERSATION_DESTINATION + event.conversationId(), payload);

        if (concernsTheAgentQueue(event)) {
            template.convertAndSend(AGENT_QUEUE_DESTINATION, payload);
        }

        log.debug(
                "Evenement {} diffuse pour la conversation {}",
                payload.type(),
                event.conversationId());
    }

    /**
     * Trois evenements modifient la file d'attente : une demande y entre, un agent la prend — et
     * elle doit alors disparaitre des autres postes (US-26) —, ou elle est cloturee.
     */
    private static boolean concernsTheAgentQueue(ChatEvent event) {
        return event instanceof ConversationOpened
                || event instanceof ConversationTaken
                || event instanceof ConversationClosed;
    }
}
