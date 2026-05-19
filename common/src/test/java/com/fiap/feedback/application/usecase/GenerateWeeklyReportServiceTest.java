package com.fiap.feedback.application.usecase;

import com.fiap.feedback.application.fakes.FixedClock;
import com.fiap.feedback.application.fakes.InMemoryEvaluationRepository;
import com.fiap.feedback.application.fakes.RecordingEmailSender;
import com.fiap.feedback.application.port.in.GenerateWeeklyReport;
import com.fiap.feedback.domain.Evaluation;
import com.fiap.feedback.domain.Urgency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GenerateWeeklyReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-19T11:00:00Z");
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private InMemoryEvaluationRepository repository;
    private RecordingEmailSender mailer;
    private GenerateWeeklyReportService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEvaluationRepository();
        mailer = new RecordingEmailSender();
        service = new GenerateWeeklyReportService(repository, mailer, new FixedClock(NOW));
    }

    @Test
    void aggregates_evaluations_in_the_last_seven_days() {
        repository.save(Evaluation.rehydrate("id-1", "ruim", 2, Urgency.ALTA, NOW.minus(Duration.ofDays(1))));
        repository.save(Evaluation.rehydrate("id-2", "ok", 5, Urgency.MEDIA, NOW.minus(Duration.ofDays(1))));
        repository.save(Evaluation.rehydrate("id-3", "otima", 9, Urgency.BAIXA, NOW.minus(Duration.ofDays(2))));
        repository.save(Evaluation.rehydrate("id-old", "fora da janela", 8, Urgency.BAIXA,
                NOW.minus(Duration.ofDays(10))));

        GenerateWeeklyReport.Summary summary = service.handle();

        assertThat(summary.entries()).hasSize(3);
        assertThat(summary.countPerUrgency())
                .containsEntry(Urgency.ALTA, 1L)
                .containsEntry(Urgency.MEDIA, 1L)
                .containsEntry(Urgency.BAIXA, 1L);
        assertThat(summary.averageNota()).isCloseTo((2 + 5 + 9) / 3.0, within(0.001));

        LocalDate dayMinus1 = NOW.minus(Duration.ofDays(1)).atZone(SAO_PAULO).toLocalDate();
        LocalDate dayMinus2 = NOW.minus(Duration.ofDays(2)).atZone(SAO_PAULO).toLocalDate();
        assertThat(summary.countPerDay()).containsEntry(dayMinus1, 2L).containsEntry(dayMinus2, 1L);
    }

    @Test
    void always_sends_an_email_even_when_window_is_empty() {
        GenerateWeeklyReport.Summary summary = service.handle();

        assertThat(summary.entries()).isEmpty();
        assertThat(summary.averageNota()).isEqualTo(0.0);
        assertThat(mailer.sent()).hasSize(1);
        assertThat(mailer.sent().get(0).subject()).contains("Relatório semanal");
    }
}
