package com.fiap.feedback.api;

import com.fiap.feedback.application.port.out.Clock;
import com.fiap.feedback.application.port.out.EmailSender;
import com.fiap.feedback.application.port.out.EvaluationRepository;
import com.fiap.feedback.application.port.out.EventPublisher;
import com.fiap.feedback.domain.Evaluation;
import com.fiap.feedback.domain.ReportPeriod;

import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end HTTP test for the API inbound adapter. The outbound ports are
 * replaced with in-memory {@link Mock} beans so the test runs without any GCP
 * dependency.
 */
@QuarkusTest
class EvaluationResourceTest {

    @Test
    void accepts_a_valid_payload_and_returns_201_with_id_and_urgency() {
        given()
                .contentType("application/json")
                .body("""
                        { "descricao": "Aula muito boa", "nota": 9 }
                        """)
                .when().post("/avaliacao")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("urgencia", equalTo("BAIXA"));
    }

    @Test
    void critical_rating_yields_ALTA() {
        given()
                .contentType("application/json")
                .body("""
                        { "descricao": "Conteudo confuso", "nota": 1 }
                        """)
                .when().post("/avaliacao")
                .then()
                .statusCode(201)
                .body("urgencia", equalTo("ALTA"));
    }

    @Test
    void rejects_nota_out_of_range_with_400() {
        given()
                .contentType("application/json")
                .body("""
                        { "descricao": "ok", "nota": 11 }
                        """)
                .when().post("/avaliacao")
                .then().statusCode(400);
    }

    @Test
    void rejects_blank_descricao_with_400() {
        given()
                .contentType("application/json")
                .body("""
                        { "descricao": "", "nota": 5 }
                        """)
                .when().post("/avaliacao")
                .then().statusCode(400);
    }

    @Mock
    @ApplicationScoped
    public static class MockEvaluationRepository implements EvaluationRepository {
        private final List<Evaluation> store = new ArrayList<>();
        @Override
        public Evaluation save(Evaluation evaluation) {
            store.add(evaluation);
            return evaluation;
        }
        @Override
        public List<Evaluation> findByPeriod(ReportPeriod period) {
            return store.stream().filter(e -> period.contains(e.dataEnvio())).toList();
        }
    }

    @Mock
    @ApplicationScoped
    public static class MockEventPublisher implements EventPublisher {
        @Override
        public void publishUrgent(Evaluation evaluation) {
            // no-op in tests
        }
    }

    @Mock
    @ApplicationScoped
    public static class MockClock implements Clock {
        @Override
        public Instant now() {
            return Instant.parse("2026-05-19T18:00:00Z");
        }
    }

    @Mock
    @ApplicationScoped
    public static class MockEmailSender implements EmailSender {
        @Override
        public void send(String subject, String htmlBody, String dedupeKey) {
            // no-op in tests
        }
    }
}
