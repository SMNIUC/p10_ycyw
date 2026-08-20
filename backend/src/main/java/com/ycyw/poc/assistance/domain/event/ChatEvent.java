package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.ConversationId;

/**
 * Événement du domaine Assistance destiné à être diffusé.
 *
 * <p>Le domaine décrit <b>ce qui s'est produit</b>, jamais comment le transmettre : il ignore
 * STOMP, le broker et le format JSON. C'est l'adaptateur secondaire de diffusion qui traduit
 * l'événement en trame et choisit sa destination.
 *
 * <p>L'interface est scellée : ajouter un événement oblige à traiter le nouveau cas partout où
 * l'ensemble est parcouru, plutôt que de l'oublier silencieusement.
 */
public sealed interface ChatEvent
        permits ConversationOpened,
                ConversationTaken,
                ConversationClosed,
                MessagePosted,
                MessageDelivered,
                ConversationRead {

    /** Conversation concernée — elle détermine la destination de diffusion. */
    ConversationId conversationId();
}
