package com.ycyw.poc.assistance.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Identifiant de conversation. Objet-valeur : jamais un UUID nu circulant dans les signatures. */
public record ConversationId(UUID value) {

    public ConversationId {
        Objects.requireNonNull(value, "value");
    }

    public static ConversationId of(String value) {
        return new ConversationId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
