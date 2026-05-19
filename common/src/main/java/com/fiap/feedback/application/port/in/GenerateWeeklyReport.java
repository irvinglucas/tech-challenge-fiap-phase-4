package com.fiap.feedback.application.port.in;

import com.fiap.feedback.domain.Urgency;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Input port for the weekly report use case.
 *
 * <p>The HTTP inbound adapter (invoked by Cloud Scheduler) calls
 * {@link #handle()}, which derives the 7-day window from the
 * {@link com.fiap.feedback.application.port.out.Clock} and dispatches the
 * report e-mail.</p>
 */
public interface GenerateWeeklyReport {

    Summary handle();

    record Summary(
            Instant periodFrom,
            Instant periodTo,
            List<Entry> entries,
            Map<LocalDate, Long> countPerDay,
            Map<Urgency, Long> countPerUrgency,
            double averageNota
    ) {
    }

    record Entry(String descricao, Urgency urgencia, Instant dataEnvio) {
    }
}
