package com.fiap.feedback.infrastructure.time;

import com.fiap.feedback.application.port.out.Clock;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

/**
 * Adapter that returns the real wall-clock time. Replaced in tests by a fixed
 * {@code FixedClock}.
 */
@ApplicationScoped
public class SystemClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
