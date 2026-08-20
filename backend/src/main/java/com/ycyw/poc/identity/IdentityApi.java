package com.ycyw.poc.identity;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrat publié du contexte Identité.
 *
 * <p>C'est la seule surface que les autres parties du déployable peuvent appeler : les entités, le
 * dépôt et l'encodeur de mot de passe restent internes au module. Une règle d'architecture vérifiée
 * au build interdit d'atteindre l'intérieur du module (DA-04).
 *
 * <p>Conformément à DA-03, ce contexte est <b>générique</b> : aucune modélisation de domaine n'y
 * est investie, il s'appuie sur les mécanismes du framework de sécurité.
 */
public interface IdentityApi {

    /** Vérifie les identifiants. Retourne vide si l'un ou l'autre est invalide — sans distinguer. */
    Optional<AppUser> authenticate(String email, String rawPassword);

    Optional<AppUser> findById(UUID id);
}
