package com.ycyw.poc.identity;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrat publie du contexte Identite.
 *
 * <p>C'est la seule surface que les autres parties du deployable peuvent appeler : les entites, le
 * depot et l'encodeur de mot de passe restent internes au module. Une regle d'architecture verifiee
 * au build interdit d'atteindre l'interieur du module (DA-04).
 *
 * <p>Conformement a DA-03, ce contexte est <b>generique</b> : aucune modelisation de domaine n'y
 * est investie, il s'appuie sur les mecanismes du framework de securite.
 */
public interface IdentityApi {

    /** Verifie les identifiants. Retourne vide si l'un ou l'autre est invalide — sans distinguer. */
    Optional<AppUser> authenticate(String email, String rawPassword);

    Optional<AppUser> findById(UUID id);
}
