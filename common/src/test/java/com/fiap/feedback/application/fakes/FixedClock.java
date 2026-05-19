package com.fiap.feedback.application.fakes;

import com.fiap.feedback.application.port.out.Clock;

import java.time.Instant;

public final class FixedClock implements Clock {

    private Instant now;

    public FixedClock(Instant now) {
        this.now = now;
    }

    public void set(Instant now) {
        this.now = now;
    }

    @Override
    public Instant now() {
        return now;
    }
}
