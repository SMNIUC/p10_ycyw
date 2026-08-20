package com.ycyw.poc.identity;

/**
 * Rôle d'un compte du point de vue du contexte Identité.
 *
 * <p>Volontairement distinct de {@code ParticipantRole} du contexte Assistance, bien que les deux
 * énumérations portent aujourd'hui les mêmes valeurs. Chaque contexte définit son propre
 * vocabulaire ; la traduction se fait à la frontière, dans l'adaptateur primaire. Partager
 * l'énumération serait le premier fil du couplage que C-02 documente dans l'existant.
 */
public enum UserRole {
    CUSTOMER,
    AGENT
}
