package com.ycyw.poc.identity;

import java.util.UUID;

/**
 * Compte utilisateur tel que le reste de l'application le voit.
 *
 * <p>Ni mot de passe ni empreinte : ce qui sort du contexte Identité est le strict nécessaire
 * (minimisation, ENF-14).
 */
public record AppUser(UUID id, String email, String displayName, UserRole role) {}
