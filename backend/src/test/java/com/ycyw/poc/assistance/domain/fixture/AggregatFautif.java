package com.ycyw.poc.assistance.domain.fixture;

import org.springframework.transaction.annotation.Transactional;

/**
 * Classe volontairement fautive, utilisée pour vérifier que les règles d'architecture détectent
 * bien ce qu'elles prétendent détecter.
 *
 * <p>Elle cumule trois fautes : une annotation de framework, une dépendance vers le framework, et
 * un accesseur en écriture public dans le domaine. Elle vit dans les sources de test, n'est jamais
 * analysée par la suite de règles elle-même, et ne declare aucun stéréotype qui la transformerait
 * en composant du contexte applicatif.
 *
 * <p><b>L'analyse statique la signale comme inutilisée — c'est attendu.</b> ArchUnit charge ces
 * classes par le <b>nom de leur paquet</b>, sous forme de chaîne :
 * {@code importPackages("com.ycyw.poc.assistance.domain.fixture")}. Aucune analyse statique ne peut
 * suivre une référence qui n'existe que dans un littéral. Supprimer cette classe ferait échouer
 * {@code ArchitectureRulesAreEffectiveTest}.
 *
 * <p><b>L'accesseur en écriture reste écrit à la main : ne pas accepter la conversion en
 * {@code @Setter} que propose l'IDE.</b> Lombok n'entre pas dans le domaine (README § 6.6) — c'est
 * la convention même que la faute ci-dessous sert à faire respecter. Et la démonstration exige que
 * cette classe porte <b>exactement</b> les trois fautes citées plus haut : y ajouter un mécanisme
 * de génération brouillerait la correspondance entre chaque faute et la règle qu'elle éprouve.
 */
@Transactional
@SuppressWarnings({"unused", "LombokSetterMayBeUsed"})
public class AggregatFautif {

    private String sujet;

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String sujet() {
        return sujet;
    }
}
