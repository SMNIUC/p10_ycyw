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
 * <p>Il porte ce que le canal temps réel ne peut pas porter : l'ouverture d'une conversation,
 * l'historique — donc la <b>reprise après coupure</b> exigée par US-24 —, la file d'attente des
 * agents et la prise en charge.
 *
 * <p>Les deux canaux appellent les <b>mêmes</b> services applicatifs. Aucune règle n'est dupliquée,
 * et c'est la condition pour qu'un troisième point d'entrée — l'API des applications d'agence —
 * puisse être ajoute sans réécrire de métier (US-30).
 */
@RestController
@RequestMapping("/api")
class ConversationController {

    private final ConversationLifecycleService lifecycle;
    private final ConversationQueryService queries;
    private final MessagingService messaging;

    ConversationController(
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
    ConversationView open(
            @RequestBody OpenConversationRequest request, @AuthenticationPrincipal Jwt jwt) {
        Conversation conversation = lifecycle.open(participant(jwt), request.subject());
        return ConversationView.of(conversation);
    }

    /** US-25 : mes conversations, avec leur compteur de messages non lus. */
    @GetMapping("/conversations")
    @Transactional(readOnly = true)
    List<ConversationSummaryView> mine(@AuthenticationPrincipal Jwt jwt) {
        return queries.conversationsOf(participant(jwt)).stream()
                .map(ConversationSummaryView::of)
                .toList();
    }

    /**
     * Historique complet.
     *
     * <p>C'est l'appel qui rend vraie la reprise sans perte d'US-24 : à la reconnexion, le client
     * recharge l'historique persisté plutôt que de se fier à ce qu'il avait en mémoire. La
     * persistance fait autorité, le canal temps réel n'est qu'une livraison anticipée.
     */
    @GetMapping("/conversations/{id}/messages")
    @Transactional(readOnly = true)
    List<MessagePayload> history(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return queries.history(ConversationId.of(id), participant(jwt)).stream()
                .map(MessagePayload::of)
                .toList();
    }

    /** Accusé de lecture par le canal REST — utile quand la connexion temps réel est coupée. */
    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void markRead(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        messaging.markRead(ConversationId.of(id), participant(jwt));
    }

    /** US-26 : clôture, diffusée au client. */
    @PostMapping("/conversations/{id}/close")
    @Transactional
    ConversationView close(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ConversationView.of(lifecycle.close(ConversationId.of(id), participant(jwt)));
    }

    /** US-26 : file d'attente, par ordre d'arrivée, avec le temps d'attente. Réservée aux agents. */
    @GetMapping("/agent/queue")
    @Transactional(readOnly = true)
    List<WaitingConversationView> queue() {
        return queries.waitingQueue().stream().map(WaitingConversationView::of).toList();
    }

    /**
     * US-26 : prise en charge.
     *
     * <p>Deux agents peuvent cliquer au même instant depuis deux instances. Le refus du second vient
     * de l'agrégat et du verrou optimiste de la base, pas d'une vérification préalable qui laisserait
     * une fenêtre entre le contrôle et l'écriture.
     */
    @PostMapping("/agent/conversations/{id}/take")
    @Transactional
    ConversationView takeOver(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ConversationView.of(lifecycle.takeOver(ConversationId.of(id), participant(jwt)));
    }

    private static ParticipantId participant(Jwt jwt) {
        return IdentityTranslation.asParticipant(AuthenticatedUser.from(jwt));
    }
}
