package com.fiap.feedback.application.port.out;

import java.time.Instant;

/**
 * Output port wrapping the current instant.
 *
 * <p>Use cases depend on this interface (instead of {@link java.time.Instant#now()})
 * so they can be unit-tested deterministically with a fixed-time fake.</p>
 */
public interface Clock {
    Instant now();
}
