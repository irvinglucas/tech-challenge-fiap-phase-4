package com.fiap.feedback.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluationTest {

    private static final Instant NOW = Instant.parse("2026-05-19T18:00:00Z");

    @Test
    void newSubmission_derives_urgency_from_nota() {
        Evaluation alta = Evaluation.newSubmission("Aula muito ruim", 2, NOW);
        assertThat(alta.urgencia()).isEqualTo(Urgency.ALTA);
        assertThat(alta.id()).isNotBlank();
    }

    @Test
    void newSubmission_trims_descricao() {
        Evaluation e = Evaluation.newSubmission("  comentario  ", 8, NOW);
        assertThat(e.descricao()).isEqualTo("comentario");
    }

    @Test
    void newSubmission_rejects_blank_descricao() {
        assertThatThrownBy(() -> Evaluation.newSubmission("   ", 5, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descricao");
    }

    @Test
    void newSubmission_rejects_descricao_longer_than_4000_chars() {
        String tooLong = "x".repeat(4001);
        assertThatThrownBy(() -> Evaluation.newSubmission(tooLong, 5, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newSubmission_rejects_nota_outside_range() {
        assertThatThrownBy(() -> Evaluation.newSubmission("ok", -1, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Evaluation.newSubmission("ok", 11, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rehydrate_keeps_provided_urgency_without_reclassifying() {
        Evaluation e = Evaluation.rehydrate("id-1", "x", 2, Urgency.BAIXA, NOW);
        assertThat(e.urgencia()).isEqualTo(Urgency.BAIXA);
    }

    @Test
    void equals_and_hashCode_are_keyed_by_id() {
        Evaluation a = Evaluation.rehydrate("id-1", "x", 5, Urgency.MEDIA, NOW);
        Evaluation b = Evaluation.rehydrate("id-1", "y", 9, Urgency.BAIXA, NOW);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
