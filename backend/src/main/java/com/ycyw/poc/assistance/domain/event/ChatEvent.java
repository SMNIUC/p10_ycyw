package com.ycyw.poc.assistance.domain.event;

import com.ycyw.poc.assistance.domain.model.ConversationId;

/**
 * Evenement du domaine Assistance destine a etre diffuse.
 *
 * <p>Le domaine decrit <b>ce qui s'est produit</b>, jamais comment le transmettre : il ignore
 * STOMP, le broker et le format JSON. C'est l'adaptateur secondaire de diffusion qui traduit
 * l'evenement en trame et choisit sa destination.
 *
 * <p>L'interface est scellee : ajouter un evenement oblige a traiter le nouveau cas partout ou
 * l'ensemble est parcouru, plutot que de l'oublier silencieusement.
 */
public sealed interface ChatEvent
        permits ConversationOpened,
                ConversationTaken,
                ConversationClosed,
                MessagePosted,
                MessageDelivered,
                ConversationRead {

    /** Conversation concernee — elle determine la destination de diffusion. */
    ConversationId conversationId();
}
