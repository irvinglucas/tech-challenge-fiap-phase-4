package com.fiap.feedback.domain;

/**
 * Urgency level derived from a student's rating ({@code nota}).
 *
 * <p>The classification rule lives in {@link UrgencyPolicy}, not here, so that
 * changing the thresholds never requires editing the enum itself.</p>
 */
public enum Urgency {
    ALTA,
    MEDIA,
    BAIXA;

    public boolean isCritical() {
        return this == ALTA;
    }
}
