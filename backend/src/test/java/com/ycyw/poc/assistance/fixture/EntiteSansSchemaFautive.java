package com.ycyw.poc.assistance.fixture;

import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Classe volontairement fautive : « entité » du module Assistance qui ne declare pas son schéma, et
 * atterrirait donc dans le schéma par défaut de la connexion. Sert à vérifier que la règle de
 * cloisonnement par schéma détecte bien la faute.
 *
 * <p>Volontairement dépourvue de l'annotation d'entité : le contrôle porte sur la déclaration de
 * table, et cette classe ne doit pas être reprise par le gestionnaire de persistance des tests.
 *
 * <p><b>L'analyse statique la signale comme inutilisée — c'est attendu.</b> ArchUnit charge ces
 * classes par le <b>nom de leur paquet</b>, sous forme de chaîne :
 * {@code importPackages("com.ycyw.poc.assistance.fixture")}. Aucune analyse statique ne peut suivre
 * une référence qui n'existe que dans un littéral. Supprimer cette classe ferait échouer
 * {@code ArchitectureRulesAreEffectiveTest}.
 */
@Table(name = "table_sans_schema")
@SuppressWarnings("unused")
public class EntiteSansSchemaFautive {

    private UUID id;

    public UUID id() {
        return id;
    }
}
