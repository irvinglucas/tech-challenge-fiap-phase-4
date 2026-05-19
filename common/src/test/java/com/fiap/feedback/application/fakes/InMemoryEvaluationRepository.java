package com.fiap.feedback.application.fakes;

import com.fiap.feedback.application.port.out.EvaluationRepository;
import com.fiap.feedback.domain.Evaluation;
import com.fiap.feedback.domain.ReportPeriod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InMemoryEvaluationRepository implements EvaluationRepository {

    private final List<Evaluation> storage = new ArrayList<>();

    @Override
    public Evaluation save(Evaluation evaluation) {
        storage.add(evaluation);
        return evaluation;
    }

    @Override
    public List<Evaluation> findByPeriod(ReportPeriod period) {
        return storage.stream()
                .filter(e -> period.contains(e.dataEnvio()))
                .sorted(Comparator.comparing(Evaluation::dataEnvio))
                .toList();
    }

    public List<Evaluation> all() {
        return List.copyOf(storage);
    }
}
