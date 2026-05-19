package com.fiap.feedback.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportPeriodTest {

    @Test
    void lastSevenDaysEndingAt_spans_exactly_seven_days() {
        Instant ref = Instant.parse("2026-05-19T11:00:00Z");
        ReportPeriod period = ReportPeriod.lastSevenDaysEndingAt(ref);
        assertThat(Duration.between(period.from(), period.to())).isEqualTo(Duration.ofDays(7));
        assertThat(period.to()).isEqualTo(ref);
    }

    @Test
    void contains_uses_closed_open_semantics() {
        Instant from = Instant.parse("2026-05-12T11:00:00Z");
        Instant to = Instant.parse("2026-05-19T11:00:00Z");
        ReportPeriod period = new ReportPeriod(from, to);

        assertThat(period.contains(from)).isTrue();
        assertThat(period.contains(to)).isFalse();
        assertThat(period.contains(from.plus(Duration.ofDays(1)))).isTrue();
    }

    @Test
    void rejects_inverted_or_empty_window() {
        Instant t = Instant.parse("2026-05-19T11:00:00Z");
        assertThatThrownBy(() -> new ReportPeriod(t, t)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReportPeriod(t, t.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }
}
