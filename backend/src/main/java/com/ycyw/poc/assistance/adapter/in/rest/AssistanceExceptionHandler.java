package com.ycyw.poc.assistance.adapter.in.rest;

import com.ycyw.poc.assistance.domain.model.exception.ConversationAlreadyTakenException;
import com.ycyw.poc.assistance.domain.model.exception.ConversationClosedException;
import com.ycyw.poc.assistance.domain.model.exception.ConversationNotFoundException;
import com.ycyw.poc.assistance.domain.model.exception.NotAParticipantException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduction des règles du domaine en réponses du protocole.
 *
 * <p>C'est ici, et seulement ici, que le vocabulaire HTTP rencontre le domaine : le domaine lève
 * « conversation déjà prise en charge », l'adaptateur répond 409. L'inverse — un domaine qui
 * connaîtrait les codes de statut — le rendrait dépendant du canal qui l'appelle.
 */
@RestControllerAdvice(assignableTypes = ConversationController.class)
class AssistanceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AssistanceExceptionHandler.class);

    @ExceptionHandler(ConversationNotFoundException.class)
    ResponseEntity<Map<String, String>> onNotFound(ConversationNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", exception.getMessage());
    }

    /**
     * 403 et non 404 : la conversation existe, l'accès est refusé. La distinction reste sans risque
     * ici, l'identifiant étant un UUID non devinable.
     */
    @ExceptionHandler(NotAParticipantException.class)
    ResponseEntity<Map<String, String>> onForbidden(NotAParticipantException exception) {
        return problem(HttpStatus.FORBIDDEN, "NOT_A_PARTICIPANT", exception.getMessage());
    }

    @ExceptionHandler(ConversationAlreadyTakenException.class)
    ResponseEntity<Map<String, String>> onAlreadyTaken(
            ConversationAlreadyTakenException exception) {
        return problem(HttpStatus.CONFLICT, "ALREADY_TAKEN", exception.getMessage());
    }

    @ExceptionHandler(ConversationClosedException.class)
    ResponseEntity<Map<String, String>> onClosed(ConversationClosedException exception) {
        return problem(HttpStatus.CONFLICT, "CONVERSATION_CLOSED", exception.getMessage());
    }

    /**
     * Le verrou optimiste a joué : deux agents ont pris la même demande au même instant. Du point de
     * vue de l'appelant, c'est le même refus que ci-dessus.
     *
     * <p><b>Le message de l'exception est tracé, jamais renvoyé.</b> Celui que produit le
     * gestionnaire de persistance nomme l'entité et la version attendue : c'est ce qu'il faut pour
     * diagnostiquer une contention, et exactement ce qu'il ne faut pas exposer à un client. La
     * réponse porte donc un texte fixe, la trace porte le détail.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Map<String, String>> onConcurrentWrite(
            OptimisticLockingFailureException exception) {
        log.info("Écriture concurrente refusée par le verrou optimiste : {}", exception.getMessage());
        return problem(
                HttpStatus.CONFLICT,
                "CONCURRENT_UPDATE",
                "La conversation a été modifiée par ailleurs. Rechargez la file d'attente.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> onInvalidInput(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", exception.getMessage());
    }

    private static ResponseEntity<Map<String, String>> problem(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}
