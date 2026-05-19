package com.fiap.feedback.application.port.out;

/**
 * Output port for outbound e-mail.
 *
 * <p>Current adapter is Gmail SMTP via {@code quarkus-mailer}; replacing it
 * (e.g. with SES, Mailgun, or Azure Communication Services) requires only
 * a new implementation in the infrastructure layer.</p>
 */
public interface EmailSender {

    /**
     * Sends an HTML e-mail to the configured administrator address.
     *
     * @param subject  e-mail subject
     * @param htmlBody pre-rendered HTML body
     * @param dedupeKey idempotency key — adapters use this as the SMTP
     *                  {@code Message-ID} so re-deliveries (e.g. Pub/Sub
     *                  retries) don't produce duplicate inboxes
     */
    void send(String subject, String htmlBody, String dedupeKey);
}
