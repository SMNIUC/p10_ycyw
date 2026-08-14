package com.ycyw.poc.assistance.support;

import com.ycyw.poc.assistance.domain.port.IdGenerator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Identifiants previsibles, pour que l'echec d'un test soit lisible. */
public class SequentialIdGenerator implements IdGenerator {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public UUID newId() {
        return new UUID(0L, counter.incrementAndGet());
    }
}
