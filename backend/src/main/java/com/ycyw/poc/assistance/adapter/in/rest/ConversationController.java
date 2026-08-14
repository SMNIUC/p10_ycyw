package com.ycyw.poc.assistance.adapter.in.rest;

import com.ycyw.poc.assistance.adapter.in.IdentityTranslation;
import com.ycyw.poc.assistance.adapter.in.rest.ConversationViews.ConversationSummaryView;
import com.ycyw.poc.assistance.adapter.in.rest.ConversationViews.ConversationView;
import com.ycyw.poc.assistance.adapter.in.rest.ConversationViews.OpenConversationRequest;
import com.ycyw.poc.assistance.adapter.in.rest.ConversationViews.WaitingConversationView;
import com.ycyw.poc.assistance.adapter.wire.MessagePayload;
import com.ycyw.poc.assistance.application.ConversationLifecycleService;
import com.ycyw.poc.assistance.application.ConversationQueryService;
import com.ycyw.poc.assistance.application.MessagingService;
import com.ycyw.poc.assistance.domain.model.Conversation;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.shared.security.AuthenticatedUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur primaire REST du contexte Assistance.
 *
 * <p>Il porte ce que le canal temps reel ne peut pas porter : l'ouverture d'une conversation,
 * l'historique — donc la <b>reprise apres coupure</b> exigee par US-24 —, la file d'attente des
 * agents et la prise en charge.
 *
 * <p>Les deux canaux appellent les <b>memes</b> services applicatifs. Aucune regle n'est dupliquee,
 * et c'est la condition pour qu'un troisieme point d'entree — l'API des applications d'agence —
 * puisse etre ajoute sans reecrire de metier (US-30).
 */
@RestController
@RequestMapping("/api")
public class ConversationController {

    private final ConversationLifecycleService lifecycle;
    private final ConversationQueryService queries;
    private final MessagingService messaging;

    public ConversationController(
            ConversationLifecycleService lifecycle,
            ConversationQueryService queries,
            MessagingService messaging) {
        this.lifecycle = lifecycle;
        this.queries = queries;
        this.messaging = messaging;
    }

    /** US-23 : le client ouvre une demande d'assistance. */
    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ConversationView open(
            @RequestBody OpenConversationRequest request, @AuthenticationPrincipal Jwt jwt) {
        Conversation conversation = lifecycle.open(participant(jwt), request.subject());
        return ConversationView.of(conversation);
    }

    /** US-25 : mes conversations, avec leur compteur de messages non lus. */
    @GetMapping("/conversations")
    @Transactional(readOnly = true)
    public List<ConversationSummaryView> mine(@AuthenticationPrincipal Jwt jwt) {
        return queries.conversationsOf(participant(jwt)).stream()
                .map(ConversationSummaryView::of)
                .toList();
    }

    /**
     * Historique complet.
     *
     * <p>C'est l'appel qui rend vraie la reprise sans perte d'US-24 : a la reconnexion, le client
     * recharge l'historique persiste plutot que de se fier a ce qu'il avait en memoire. La
     * persistance fait autorite, le canal temps reel n'est qu'une livraison anticipee.
     */
    @GetMapping("/conversations/{id}/messages")
    @Transactional(readOnly = true)
    public List<MessagePayload> history(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return queries.history(ConversationId.of(id), participant(jwt)).stream()
                .map(MessagePayload::of)
                .toList();
    }

    /** Accuse de lecture par le canal REST — utile quand la connexion temps reel est coupee. */
    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void markRead(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        messaging.markRead(ConversationId.of(id), participant(jwt));
    }

    /** US-26 : cloture, diffusee au client. */
    @PostMapping("/conversations/{id}/close")
    @Transactional
    public ConversationView close(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ConversationView.of(lifecycle.close(ConversationId.of(id), participant(jwt)));
    }

    /** US-26 : file d'attente, par ordre d'arrivee, avec le temps d'attente. Reservee aux agents. */
    @GetMapping("/agent/queue")
    @Transactional(readOnly = true)
    public List<WaitingConversationView> queue() {
        return queries.waitingQueue().stream().map(WaitingConversationView::of).toList();
    }

    /**
     * US-26 : prise en charge.
     *
     * <p>Deux agents peuvent cliquer au meme instant depuis deux instances. Le refus du second vient
     * de l'agregat et du verrou optimiste de la base, pas d'une verification prealable qui laisserait
     * une fenetre entre le controle et l'ecriture.
     */
    @PostMapping("/agent/conversations/{id}/take")
    @Transactional
    public ConversationView takeOver(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ConversationView.of(lifecycle.takeOver(ConversationId.of(id), participant(jwt)));
    }

    private static ParticipantId participant(Jwt jwt) {
        return IdentityTranslation.asParticipant(AuthenticatedUser.from(jwt));
    }
}
