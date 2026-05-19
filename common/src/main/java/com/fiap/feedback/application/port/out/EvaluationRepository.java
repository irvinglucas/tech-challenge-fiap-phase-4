package com.fiap.feedback.application.port.out;

import com.fiap.feedback.domain.Evaluation;
import com.fiap.feedback.domain.ReportPeriod;

import java.util.List;

/**
 * Persistence output port for {@link Evaluation}s.
 *
 * <p>The current adapter is Firestore (see
 * {@code com.fiap.feedback.infrastructure.firestore.FirestoreEvaluationRepository}),
 * but use cases must not depend on that fact.</p>
 */
public interface EvaluationRepository {

    Evaluation save(Evaluation evaluation);

    List<Evaluation> findByPeriod(ReportPeriod period);
}
