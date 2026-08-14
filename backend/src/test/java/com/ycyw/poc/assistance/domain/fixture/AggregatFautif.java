package com.ycyw.poc.assistance.domain.fixture;

import org.springframework.transaction.annotation.Transactional;

/**
 * Classe volontairement fautive, utilisee pour verifier que les regles d'architecture detectent
 * bien ce qu'elles pretendent detecter.
 *
 * <p>Elle cumule trois fautes : une annotation de framework, une dependance vers le framework, et
 * un accesseur en ecriture public dans le domaine. Elle vit dans les sources de test, n'est jamais
 * analysee par la suite de regles elle-meme, et ne declare aucun stereotype qui la transformerait
 * en composant du contexte applicatif.
 */
@Transactional
public class AggregatFautif {

    private String sujet;

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String sujet() {
        return sujet;
    }
}
