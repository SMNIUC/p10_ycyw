package com.ycyw.poc.assistance.fixture;

import com.ycyw.poc.identity.internal.DefaultIdentityService;

/**
 * Classe volontairement fautive : elle atteint l'intérieur du module Identité au lieu de passer par
 * son contrat publié. Sert à vérifier que la règle correspondante détecte bien la faute.
 *
 * <p><b>L'analyse statique la signale comme inutilisée — c'est attendu.</b> ArchUnit charge ces
 * classes par le <b>nom de leur paquet</b>, sous forme de chaîne :
 * {@code importPackages("com.ycyw.poc.assistance.fixture")}. Aucune analyse statique ne peut suivre
 * une référence qui n'existe que dans un littéral. Le champ ci-dessous n'est jamais affecté pour la
 * même raison : seul son <b>type</b> compte, puisque c'est lui qui crée la dépendance interdite que
 * la règle doit détecter. Supprimer cette classe ferait échouer
 * {@code ArchitectureRulesAreEffectiveTest}.
 */
@SuppressWarnings("unused")
public class ModuleIndiscretFautif {

    private DefaultIdentityService interneDUnAutreModule;

    public Object dependance() {
        return interneDUnAutreModule;
    }
}
