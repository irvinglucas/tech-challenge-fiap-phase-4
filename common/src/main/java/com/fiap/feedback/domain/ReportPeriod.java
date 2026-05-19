package com.fiap.feedback.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Closed-open time window {@code [from, to)} used by the weekly report.
 *
 * <p>The boundaries are stored as {@link Instant}s so this value object stays
 * timezone-agnostic; presentation formatting is the responsibility of the
 * adapter that renders the report (e.g. e-mail HTML in São Paulo time).</p>
 */
public record ReportPeriod(Instant from, Instant to) {

    public ReportPeriod {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be strictly before to");
        }
    }

    public static ReportPeriod lastSevenDaysEndingAt(Instant referenceTo) {
        Instant truncatedTo = referenceTo.truncatedTo(ChronoUnit.SECONDS);
        return new ReportPeriod(truncatedTo.minus(Duration.ofDays(7)), truncatedTo);
    }

    public boolean contains(Instant when) {
        return !when.isBefore(from) && when.isBefore(to);
    }
}
