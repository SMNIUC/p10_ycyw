package com.ycyw.poc.assistance.domain.port;

import java.util.UUID;

/**
 * Port de génération d'identifiants.
 *
 * <p>Les identifiants sont attribués par l'application, pas par la base : un message est identifié
 * avant d'être persisté, ce qui permet d'en accuser réception sans attendre un retour de séquence.
 */
public interface IdGenerator {

    UUID newId();
}
