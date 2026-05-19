package com.fiap.feedback.application.usecase;

import com.fiap.feedback.application.fakes.FixedClock;
import com.fiap.feedback.application.fakes.InMemoryEvaluationRepository;
import com.fiap.feedback.application.fakes.RecordingEventPublisher;
import com.fiap.feedback.application.port.in.SubmitEvaluation;
import com.fiap.feedback.domain.Urgency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmitEvaluationServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-19T18:00:00Z");

    private InMemoryEvaluationRepository repository;
    private RecordingEventPublisher events;
    private SubmitEvaluationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEvaluationRepository();
        events = new RecordingEventPublisher();
        service = new SubmitEvaluationService(repository, events, new FixedClock(NOW));
    }

    @Test
    void persists_and_returns_classified_evaluation() {
        SubmitEvaluation.Result result = service.handle(new SubmitEvaluation.Command("ok", 8));

        assertThat(result.urgencia()).isEqualTo(Urgency.BAIXA);
        assertThat(repository.all()).hasSize(1);
        assertThat(repository.all().get(0).id()).isEqualTo(result.id());
        assertThat(repository.all().get(0).dataEnvio()).isEqualTo(NOW);
    }

    @Test
    void publishes_event_only_when_urgency_is_critical() {
        service.handle(new SubmitEvaluation.Command("low score", 2));   // ALTA
        service.handle(new SubmitEvaluation.Command("mid", 5));         // MEDIA
        service.handle(new SubmitEvaluation.Command("great", 9));       // BAIXA

        assertThat(events.published()).hasSize(1);
        assertThat(events.published().get(0).urgencia()).isEqualTo(Urgency.ALTA);
    }

    @Test
    void propagates_validation_errors_from_the_domain() {
        assertThatThrownBy(() -> service.handle(new SubmitEvaluation.Command("ok", 11)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.handle(new SubmitEvaluation.Command("", 5)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(repository.all()).isEmpty();
        assertThat(events.published()).isEmpty();
    }
}
