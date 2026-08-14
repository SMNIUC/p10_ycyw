package com.ycyw.poc.assistance.fixture;

import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Classe volontairement fautive : « entite » du module Assistance qui ne declare pas son schema, et
 * atterrirait donc dans le schema par defaut de la connexion. Sert a verifier que la regle de
 * cloisonnement par schema detecte bien la faute.
 *
 * <p>Volontairement depourvue de l'annotation d'entite : le controle porte sur la declaration de
 * table, et cette classe ne doit pas etre reprise par le gestionnaire de persistance des tests.
 */
@Table(name = "table_sans_schema")
public class EntiteSansSchemaFautive {

    private UUID id;

    public UUID id() {
        return id;
    }
}
