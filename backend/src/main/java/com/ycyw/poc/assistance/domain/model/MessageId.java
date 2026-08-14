package com.ycyw.poc.assistance.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Identifiant de message. */
public record MessageId(UUID value) {

    public MessageId {
        Objects.requireNonNull(value, "value");
    }

    public static MessageId of(String value) {
        return new MessageId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
