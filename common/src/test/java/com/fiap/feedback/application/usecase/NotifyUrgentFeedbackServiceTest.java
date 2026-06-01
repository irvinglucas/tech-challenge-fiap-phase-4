package com.fiap.feedback.application.usecase;

import com.fiap.feedback.application.fakes.RecordingEmailSender;
import com.fiap.feedback.application.port.in.NotifyUrgentFeedback;
import com.fiap.feedback.domain.Urgency;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotifyUrgentFeedbackServiceTest {

    @Test
    void sends_email_with_required_fields_and_dedupe_key() {
        RecordingEmailSender mailer = new RecordingEmailSender();
        NotifyUrgentFeedbackService service = new NotifyUrgentFeedbackService(mailer);

        service.handle(new NotifyUrgentFeedback.Command(
                "eval-1",
                "Aula confusa & demorada",
                2,
                Urgency.ALTA,
                Instant.parse("2026-05-19T18:00:00Z"),
                "pubsub-msg-id-123"));

        assertThat(mailer.sent()).hasSize(1);
        RecordingEmailSender.SentEmail email = mailer.sent().get(0);
        assertThat(email.subject()).contains("ALTA");
        assertThat(email.htmlBody()).contains("Aula confusa &amp; demorada");
        assertThat(email.htmlBody()).contains("<b>Nota:</b>");
        assertThat(email.htmlBody()).contains(">2<");
        assertThat(email.htmlBody()).contains("ALTA");
        assertThat(email.htmlBody()).contains("eval-1");
        assertThat(email.dedupeKey()).isEqualTo("pubsub-msg-id-123");
    }
}
