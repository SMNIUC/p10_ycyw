package com.ycyw.poc.assistance.adapter.out.system;

import com.ycyw.poc.assistance.domain.port.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Adaptateur d'horloge. En test, il est remplace par une horloge fixe. */
@Component
public class SystemTimeProvider implements TimeProvider {

    private final Clock clock = Clock.systemUTC();

    @Override
    public Instant now() {
        return clock.instant();
    }
}
