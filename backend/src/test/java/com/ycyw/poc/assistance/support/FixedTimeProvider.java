package com.ycyw.poc.assistance.support;

import com.ycyw.poc.assistance.domain.port.TimeProvider;
import java.time.Duration;
import java.time.Instant;

/**
 * Horloge maitrisee par le test.
 *
 * <p>C'est la raison d'etre du port d'horloge (DA-06) : sans lui, un test portant sur un temps
 * d'attente ou sur l'ordre de deux lectures dependrait de la date a laquelle il est execute.
 */
public class FixedTimeProvider implements TimeProvider {

    private Instant now;

    public FixedTimeProvider(Instant start) {
        this.now = start;
    }

    @Override
    public Instant now() {
        return now;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }
}
