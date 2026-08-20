package com.ycyw.poc.assistance.domain.fixture;

import com.ycyw.poc.assistance.adapter.wire.MessagePayload;

/**
 * Classe volontairement fautive : une classe du domaine qui dépend d'un adaptateur, alors que la
 * dépendance ne va que dans l'autre sens. Sert à vérifier que la règle
 * {@code le_domaine_ignore_les_adaptateurs} détecte bien la faute.
 *
 * <p><b>Pourquoi cette classe existe séparément.</b> {@code AggregatFautif} porte les fautes que
 * détectent les règles sur le framework et les accesseurs. Celle-ci porte la seule qui restait sans
 * preuve : l'inversion de dépendance de DA-05. Une classe par faute garde la correspondance lisible
 * entre ce qui est éprouvé et la règle qui l'éprouve.
 *
 * <p>Le type choisi — {@code MessagePayload} — est une représentation de transport, sans dépendance
 * de framework : la faute constatée est bien celle du sens de dépendance, et pas une autre.
 *
 * <p><b>L'analyse statique la signale comme inutilisée — c'est attendu.</b> ArchUnit charge ces
 * classes par le <b>nom de leur paquet</b>, sous forme de chaîne :
 * {@code importPackages("com.ycyw.poc.assistance.domain.fixture")}. Aucune analyse statique ne peut
 * suivre une référence qui n'existe que dans un littéral. Supprimer cette classe ferait échouer
 * {@code ArchitectureRulesAreEffectiveTest}.
 */
@SuppressWarnings("unused")
public class DomaineIndiscretFautif {

    private MessagePayload sortieDAdaptateur;

    public Object dependance() {
        return sortieDAdaptateur;
    }
}
