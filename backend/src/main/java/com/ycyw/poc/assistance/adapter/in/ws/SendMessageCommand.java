package com.ycyw.poc.assistance.adapter.in.ws;

/**
 * Trame d'envoi d'un message.
 *
 * <p>Elle ne porte ni auteur ni horodatage : l'auteur vient de la session authentifiée et l'instant
 * de l'horloge du serveur. Accepter ces deux valeurs du client permettrait d'écrire sous une autre
 * identité ou d'antidater un échange.
 */
public record SendMessageCommand(String body) {}
