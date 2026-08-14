package com.ycyw.poc.assistance.domain.model;

/**
 * Contenu d'un message.
 *
 * <p>La validation vit dans le domaine, pas dans l'adaptateur : un message vide ou demesure doit
 * etre refuse quel que soit le point d'entree — WebSocket aujourd'hui, API d'agence demain.
 */
public record MessageBody(String text) {

    public static final int MAX_LENGTH = 4000;

    public MessageBody {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Le contenu d'un message ne peut pas etre vide.");
        }
        text = text.strip();
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Le contenu d'un message est limite a " + MAX_LENGTH + " caracteres.");
        }
    }

    @Override
    public String toString() {
        return text;
    }
}
