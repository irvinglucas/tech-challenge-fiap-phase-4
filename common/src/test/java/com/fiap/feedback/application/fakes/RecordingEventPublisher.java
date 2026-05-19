package com.fiap.feedback.application.fakes;

import com.fiap.feedback.application.port.out.EventPublisher;
import com.fiap.feedback.domain.Evaluation;

import java.util.ArrayList;
import java.util.List;

public final class RecordingEventPublisher implements EventPublisher {

    private final List<Evaluation> published = new ArrayList<>();

    @Override
    public void publishUrgent(Evaluation evaluation) {
        published.add(evaluation);
    }

    public List<Evaluation> published() {
        return List.copyOf(published);
    }
}
