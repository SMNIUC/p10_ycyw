package com.ycyw.poc.assistance.domain.port;

import java.time.Instant;

/**
 * Port d'horloge.
 *
 * <p>Aucun fournisseur d'horloge ne sera jamais « change ». Le port existe pour la testabilite
 * (DA-06) : sans lui, l'anciennete d'une demande en file d'attente ou l'ordre des marqueurs de
 * lecture dependraient de la date d'execution du test. L'instant est absolu (UTC) ; le fuseau
 * d'affichage est une preoccupation de l'interface (ENF-08).
 */
public interface TimeProvider {

    Instant now();
}
