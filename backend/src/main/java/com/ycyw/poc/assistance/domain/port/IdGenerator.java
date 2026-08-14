package com.ycyw.poc.assistance.domain.port;

import java.util.UUID;

/**
 * Port de generation d'identifiants.
 *
 * <p>Les identifiants sont attribues par l'application, pas par la base : un message est identifie
 * avant d'etre persiste, ce qui permet d'en accuser reception sans attendre un retour de sequence.
 */
public interface IdGenerator {

    UUID newId();
}
