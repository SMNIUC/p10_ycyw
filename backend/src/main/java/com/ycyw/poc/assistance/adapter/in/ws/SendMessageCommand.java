package com.ycyw.poc.assistance.adapter.in.ws;

/**
 * Trame d'envoi d'un message.
 *
 * <p>Elle ne porte ni auteur ni horodatage : l'auteur vient de la session authentifiee et l'instant
 * de l'horloge du serveur. Accepter ces deux valeurs du client permettrait d'ecrire sous une autre
 * identite ou d'antidater un echange.
 */
public record SendMessageCommand(String body) {}
