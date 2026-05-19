package com.fiap.feedback.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Core domain entity: a single feedback submitted by a student.
 *
 * <p>Immutable. Invariants are enforced in the static factory:
 * <ul>
 *   <li>{@code descricao} must be non-blank</li>
 *   <li>{@code nota} must be in {@code [0, 10]}</li>
 *   <li>{@code urgencia} is always derived from {@code nota} via
 *       {@link UrgencyPolicy} (kept as a field so the value persists)</li>
 * </ul>
 *
 * <p>The {@code id} is provided by the repository on persist, or by the
 * caller (e.g. tests). {@link #newSubmission(String, int, Instant)} creates an
 * entity with a fresh UUID; {@link #rehydrate(String, String, int, Urgency,
 * Instant)} reconstructs one from storage without re-classifying.</p>
 */
public final class Evaluation {

    private static final int DESCRICAO_MAX_LENGTH = 4000;

    private final String id;
    private final String descricao;
    private final int nota;
    private final Urgency urgencia;
    private final Instant dataEnvio;

    private Evaluation(String id, String descricao, int nota, Urgency urgencia, Instant dataEnvio) {
        this.id = Objects.requireNonNull(id, "id");
        this.descricao = Objects.requireNonNull(descricao, "descricao");
        this.nota = nota;
        this.urgencia = Objects.requireNonNull(urgencia, "urgencia");
        this.dataEnvio = Objects.requireNonNull(dataEnvio, "dataEnvio");
    }

    public static Evaluation newSubmission(String descricao, int nota, Instant dataEnvio) {
        validate(descricao, nota);
        return new Evaluation(
                UUID.randomUUID().toString(),
                descricao.trim(),
                nota,
                UrgencyPolicy.classify(nota),
                dataEnvio);
    }

    public static Evaluation rehydrate(String id, String descricao, int nota, Urgency urgencia, Instant dataEnvio) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(descricao, "descricao");
        Objects.requireNonNull(urgencia, "urgencia");
        Objects.requireNonNull(dataEnvio, "dataEnvio");
        return new Evaluation(id, descricao, nota, urgencia, dataEnvio);
    }

    private static void validate(String descricao, int nota) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descricao must not be blank");
        }
        if (descricao.length() > DESCRICAO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "descricao must be at most " + DESCRICAO_MAX_LENGTH + " characters");
        }
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("nota must be in [0, 10], got " + nota);
        }
    }

    public String id() {
        return id;
    }

    public String descricao() {
        return descricao;
    }

    public int nota() {
        return nota;
    }

    public Urgency urgencia() {
        return urgencia;
    }

    public Instant dataEnvio() {
        return dataEnvio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Evaluation that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Evaluation{" +
                "id='" + id + '\'' +
                ", nota=" + nota +
                ", urgencia=" + urgencia +
                ", dataEnvio=" + dataEnvio +
                '}';
    }
}
