package com.fiap.feedback.domain;

/**
 * Pure classification rule that maps a numeric rating to an {@link Urgency}.
 *
 * <p>Kept as a standalone class (rather than a method on {@link Urgency}) so
 * the rule can be replaced in tests or evolved over time without touching the
 * enum.</p>
 */
public final class UrgencyPolicy {

    private UrgencyPolicy() {
    }

    /**
     * Thresholds:
     * <ul>
     *   <li>0..3 &rarr; {@link Urgency#ALTA}</li>
     *   <li>4..6 &rarr; {@link Urgency#MEDIA}</li>
     *   <li>7..10 &rarr; {@link Urgency#BAIXA}</li>
     * </ul>
     */
    public static Urgency classify(int nota) {
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("nota must be in [0, 10], got " + nota);
        }
        if (nota <= 3) {
            return Urgency.ALTA;
        }
        if (nota <= 6) {
            return Urgency.MEDIA;
        }
        return Urgency.BAIXA;
    }
}
