package com.fiap.feedback.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrgencyPolicyTest {

    @ParameterizedTest
    @CsvSource({
            "0, ALTA",
            "1, ALTA",
            "2, ALTA",
            "3, ALTA",
            "4, MEDIA",
            "5, MEDIA",
            "6, MEDIA",
            "7, BAIXA",
            "8, BAIXA",
            "9, BAIXA",
            "10, BAIXA"
    })
    void classifies_each_rating_into_the_expected_urgency(int nota, Urgency expected) {
        assertThat(UrgencyPolicy.classify(nota)).isEqualTo(expected);
    }

    @Test
    void rejects_ratings_below_zero() {
        assertThatThrownBy(() -> UrgencyPolicy.classify(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_ratings_above_ten() {
        assertThatThrownBy(() -> UrgencyPolicy.classify(11))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
