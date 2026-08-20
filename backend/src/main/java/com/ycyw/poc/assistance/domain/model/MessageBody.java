package com.ycyw.poc.assistance.domain.model;

/**
 * Contenu d'un message.
 *
 * <p>La validation vit dans le domaine, pas dans l'adaptateur : un message vide ou démesuré doit
 * être refusé quel que soit le point d'entrée — WebSocket aujourd'hui, API d'agence demain.
 */
public record MessageBody(String text) {

    public static final int MAX_LENGTH = 4000;

    public MessageBody {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Le contenu d'un message ne peut pas être vide.");
        }
        text = text.strip();
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Le contenu d'un message est limité à " + MAX_LENGTH + " caractères.");
        }
    }

    @Override
    public String toString() {
        return text;
    }
}
