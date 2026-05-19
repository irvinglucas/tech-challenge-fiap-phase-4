package com.fiap.feedback.application.usecase;

import com.fiap.feedback.application.port.in.SubmitEvaluation;
import com.fiap.feedback.application.port.out.Clock;
import com.fiap.feedback.application.port.out.EvaluationRepository;
import com.fiap.feedback.application.port.out.EventPublisher;
import com.fiap.feedback.domain.Evaluation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

/**
 * Submit-evaluation use case: validates the command, classifies urgency,
 * persists the evaluation and — if classified as {@code ALTA} — publishes an
 * event for the notification handler.
 *
 * <p>Depends only on output ports, never on a concrete adapter. The CDI
 * scope is application-wide so a single instance is reused per JVM.</p>
 */
@ApplicationScoped
public class SubmitEvaluationService implements SubmitEvaluation {

    private static final Logger LOG = Logger.getLogger(SubmitEvaluationService.class);

    private final EvaluationRepository repository;
    private final EventPublisher events;
    private final Clock clock;

    @Inject
    public SubmitEvaluationService(EvaluationRepository repository,
                                   EventPublisher events,
                                   Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        Evaluation evaluation = Evaluation.newSubmission(
                command.descricao(),
                command.nota(),
                clock.now());

        Evaluation persisted = repository.save(evaluation);
        LOG.infof("Persisted evaluation id=%s urgencia=%s", persisted.id(), persisted.urgencia());

        if (persisted.urgencia().isCritical()) {
            events.publishUrgent(persisted);
            LOG.infof("Published urgent-feedback event for id=%s", persisted.id());
        }

        return new Result(persisted.id(), persisted.urgencia());
    }
}
