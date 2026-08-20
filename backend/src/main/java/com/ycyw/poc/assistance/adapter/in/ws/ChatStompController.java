package com.ycyw.poc.assistance.adapter.in.ws;

import com.ycyw.poc.assistance.adapter.in.IdentityTranslation;
import com.ycyw.poc.assistance.application.MessagingService;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.MessageId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.model.exception.DomainRuleViolation;
import com.ycyw.poc.shared.security.AuthenticatedUser;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptateur primaire temps réel.
 *
 * <p>Il ne contient aucune règle : il lit l'identité de la session, traduit la trame en appel de
 * cas d'usage, et laisse le service applicatif persister puis diffuser. C'est ce qui permet aux
 * mêmes cas d'usage d'être appelés demain par un autre point d'entrée — l'API des applications
 * d'agence, par exemple — sans dupliquer une seule règle.
 *
 * <p><b>La transaction est ouverte ici</b>, à la frontière : les services applicatifs sont des
 * objets sans annotation de framework (DA-05), ils ne peuvent donc pas la porter eux-mêmes. C'est
 * le compromis assumé de cette architecture, et il place la limite transactionnelle exactement la
 * où commence le cas d'usage.
 */
@Controller
public class ChatStompController {

    private static final Logger log = LoggerFactory.getLogger(ChatStompController.class);

    private final MessagingService messaging;

    public ChatStompController(MessagingService messaging) {
        this.messaging = messaging;
    }

    /** Envoi d'un message : destination {@code /app/conversations/{id}/messages}. */
    @MessageMapping("/conversations/{conversationId}/messages")
    @Transactional
    public void send(
            @DestinationVariable String conversationId,
            @Payload SendMessageCommand command,
            Principal principal) {
        ParticipantId author = author(principal);
        messaging.post(ConversationId.of(conversationId), author, command.body());
    }

    /** Accusé de réception : le destinataire a reçu la trame (état « remis », US-24). */
    @MessageMapping("/conversations/{conversationId}/delivered")
    @Transactional
    public void acknowledgeDelivery(
            @DestinationVariable String conversationId,
            @Payload AcknowledgeCommand command,
            Principal principal) {
        messaging.acknowledgeDelivery(
                ConversationId.of(conversationId),
                MessageId.of(command.messageId()),
                author(principal));
    }

    /** Accusé de lecture : la conversation est ouverte à l'écran (état « lu », US-24). */
    @MessageMapping("/conversations/{conversationId}/read")
    @Transactional
    public void markRead(@DestinationVariable String conversationId, Principal principal) {
        messaging.markRead(ConversationId.of(conversationId), author(principal));
    }

    /**
     * Une règle du domaine violée revient à son émetteur sur sa destination privée, sans interrompre
     * la connexion ni affecter l'autre participant.
     */
    @MessageExceptionHandler(DomainRuleViolation.class)
    @SendToUser("/queue/errors")
    public ChatErrorPayload onDomainRuleViolation(DomainRuleViolation exception) {
        log.info("Règle du domaine refusée sur le canal temps réel : {}", exception.getMessage());
        return new ChatErrorPayload("DOMAIN_RULE", exception.getMessage());
    }

    /** Contenu vide, trop long, identifiant mal forme : refus explicite, sans trace d'erreur. */
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public ChatErrorPayload onInvalidCommand(IllegalArgumentException exception) {
        return new ChatErrorPayload("INVALID_COMMAND", exception.getMessage());
    }

    private static ParticipantId author(Principal principal) {
        return IdentityTranslation.asParticipant(AuthenticatedUser.from(principal));
    }
}
