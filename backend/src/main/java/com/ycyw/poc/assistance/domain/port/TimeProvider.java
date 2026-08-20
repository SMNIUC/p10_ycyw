package com.ycyw.poc.assistance.domain.port;

import java.time.Instant;

/**
 * Port d'horloge.
 *
 * <p>Aucun fournisseur d'horloge ne sera jamais « change ». Le port existe pour la testabilité
 * (DA-06) : sans lui, l'ancienneté d'une demande en file d'attente ou l'ordre des marqueurs de
 * lecture dépendraient de la date d'exécution du test. L'instant est absolu (UTC) ; le fuseau
 * d'affichage est une préoccupation de l'interface (ENF-08).
 */
public interface TimeProvider {

    Instant now();
}
