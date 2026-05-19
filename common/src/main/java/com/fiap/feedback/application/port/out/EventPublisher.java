package com.fiap.feedback.application.port.out;

import com.fiap.feedback.domain.Evaluation;

/**
 * Output port for asynchronous fan-out of domain events.
 *
 * <p>Current adapter publishes JSON to the Pub/Sub topic
 * {@code urgent-feedback}, but use cases must not depend on that fact.</p>
 */
public interface EventPublisher {

    /**
     * Publishes an {@link Evaluation} flagged as urgent so that the
     * notification handler can pick it up and notify administrators.
     */
    void publishUrgent(Evaluation evaluation);
}
