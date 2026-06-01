package com.fiap.feedback.application.port.in;

import com.fiap.feedback.domain.Urgency;

import java.time.Instant;

/**
 * Input port for the urgent-feedback notification use case.
 *
 * <p>The Pub/Sub inbound adapter calls {@link #handle(Command)} with the
 * deserialized event; the use case takes care of building the e-mail and
 * delegating to the {@link com.fiap.feedback.application.port.out.EmailSender}
 * port.</p>
 */
public interface NotifyUrgentFeedback {

    void handle(Command command);

    record Command(
            String evaluationId,
            String descricao,
            int nota,
            Urgency urgencia,
            Instant dataEnvio,
            String dedupeKey
    ) {
    }
}
