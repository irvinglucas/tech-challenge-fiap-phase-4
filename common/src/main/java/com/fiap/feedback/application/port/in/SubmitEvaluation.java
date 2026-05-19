package com.fiap.feedback.application.port.in;

import com.fiap.feedback.domain.Urgency;

/**
 * Input port for submitting a new evaluation.
 *
 * <p>The {@link #handle(Command)} method is the only public surface of the
 * "submit feedback" use case — inbound adapters (HTTP/JAX-RS, CLI, tests)
 * depend on this interface, not on a concrete service.</p>
 */
public interface SubmitEvaluation {

    Result handle(Command command);

    record Command(String descricao, int nota) {
    }

    record Result(String id, Urgency urgencia) {
    }
}
