package com.ycyw.poc.assistance.adapter.in.rest;

import com.ycyw.poc.assistance.domain.model.exception.ConversationAlreadyTakenException;
import com.ycyw.poc.assistance.domain.model.exception.ConversationClosedException;
import com.ycyw.poc.assistance.domain.model.exception.ConversationNotFoundException;
import com.ycyw.poc.assistance.domain.model.exception.NotAParticipantException;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduction des regles du domaine en reponses du protocole.
 *
 * <p>C'est ici, et seulement ici, que le vocabulaire HTTP rencontre le domaine : le domaine leve
 * « conversation deja prise en charge », l'adaptateur repond 409. L'inverse — un domaine qui
 * connaitrait les codes de statut — le rendrait dependant du canal qui l'appelle.
 */
@RestControllerAdvice(assignableTypes = ConversationController.class)
class AssistanceExceptionHandler {

    @ExceptionHandler(ConversationNotFoundException.class)
    ResponseEntity<Map<String, String>> onNotFound(ConversationNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", exception.getMessage());
    }

    /**
     * 403 et non 404 : la conversation existe, l'acces est refuse. La distinction reste sans risque
     * ici, l'identifiant etant un UUID non devinable.
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
     * Le verrou optimiste a joue : deux agents ont pris la meme demande au meme instant. Du point de
     * vue de l'appelant, c'est le meme refus que ci-dessus.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Map<String, String>> onConcurrentWrite(
            OptimisticLockingFailureException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "CONCURRENT_UPDATE",
                "La conversation a ete modifiee par ailleurs. Rechargez la file d'attente.");
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
