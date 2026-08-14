package com.ycyw.poc.identity;

/**
 * Role d'un compte du point de vue du contexte Identite.
 *
 * <p>Volontairement distinct de {@code ParticipantRole} du contexte Assistance, bien que les deux
 * enumerations portent aujourd'hui les memes valeurs. Chaque contexte definit son propre
 * vocabulaire ; la traduction se fait a la frontiere, dans l'adaptateur primaire. Partager
 * l'enumeration serait le premier fil du couplage que C-02 documente dans l'existant.
 */
public enum UserRole {
    CUSTOMER,
    AGENT
}
