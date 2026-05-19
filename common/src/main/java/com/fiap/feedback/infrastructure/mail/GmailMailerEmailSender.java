package com.fiap.feedback.infrastructure.mail;

import com.fiap.feedback.application.port.out.EmailSender;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Gmail SMTP adapter for {@link EmailSender}.
 *
 * <p>Uses {@code quarkus-mailer} which is configured at deploy time to point
 * at {@code smtp.gmail.com:587} with STARTTLS and an App Password (see
 * {@code application.properties} in each function module). The recipient
 * comes from the {@code feedback.admin-email} config property, which the
 * functions populate from a Secret Manager-backed env var.</p>
 */
@ApplicationScoped
public class GmailMailerEmailSender implements EmailSender {

    private static final Logger LOG = Logger.getLogger(GmailMailerEmailSender.class);

    private final Mailer mailer;
    private final String adminEmail;

    @Inject
    public GmailMailerEmailSender(
            Mailer mailer,
            @ConfigProperty(name = "feedback.admin-email") String adminEmail) {
        this.mailer = mailer;
        this.adminEmail = adminEmail;
    }

    @Override
    public void send(String subject, String htmlBody, String dedupeKey) {
        Mail mail = Mail.withHtml(adminEmail, subject, htmlBody);
        if (dedupeKey != null && !dedupeKey.isBlank()) {
            mail.addHeader("Message-ID", "<" + dedupeKey + "@fiap-feedback>");
            mail.addHeader("X-Dedupe-Key", dedupeKey);
        }
        mailer.send(mail);
        LOG.infof("Sent email to=%s subject=%s dedupe=%s", adminEmail, subject, dedupeKey);
    }
}
