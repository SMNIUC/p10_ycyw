package com.ycyw.poc.assistance.adapter.in.rest;

import com.ycyw.poc.assistance.application.readmodel.ConversationSummary;
import com.ycyw.poc.assistance.application.readmodel.WaitingConversation;
import com.ycyw.poc.assistance.domain.model.Conversation;
import java.time.Instant;

/** Representations REST du contexte Assistance. */
final class ConversationViews {

    private ConversationViews() {}

    /** Conversation telle que l'affiche le client ou l'agent. */
    record ConversationView(
            String id, String subject, String status, Instant openedAt, String agentId) {

        static ConversationView of(Conversation conversation) {
            return new ConversationView(
                    conversation.id().toString(),
                    conversation.subject(),
                    conversation.status().name(),
                    conversation.openedAt(),
                    conversation.agent().map(a -> a.userId().toString()).orElse(null));
        }
    }

    /** Ligne de liste : de quoi afficher l'historique et le compteur de non-lus (US-25, US-28). */
    record ConversationSummaryView(
            String id,
            String subject,
            String status,
            Instant openedAt,
            Instant lastMessageAt,
            String lastMessagePreview,
            int unreadCount) {

        static ConversationSummaryView of(ConversationSummary summary) {
            return new ConversationSummaryView(
                    summary.id().toString(),
                    summary.subject(),
                    summary.status().name(),
                    summary.openedAt(),
                    summary.lastMessageAt(),
                    summary.lastMessagePreview(),
                    summary.unreadCount());
        }
    }

    /**
     * Ligne de la file d'attente des agents. Le temps d'attente est transmis en secondes, pour que
     * l'interface le mette en forme dans la langue et le format de l'utilisateur (ENF-08).
     */
    record WaitingConversationView(
            String id, String subject, Instant openedAt, long waitingSeconds) {

        static WaitingConversationView of(WaitingConversation waiting) {
            return new WaitingConversationView(
                    waiting.id().toString(),
                    waiting.subject(),
                    waiting.openedAt(),
                    waiting.waitingFor().toSeconds());
        }
    }

    /** Demande d'ouverture de conversation (US-23). */
    record OpenConversationRequest(String subject) {}
}
