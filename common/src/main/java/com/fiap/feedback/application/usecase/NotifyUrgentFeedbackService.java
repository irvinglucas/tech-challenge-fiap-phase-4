package com.fiap.feedback.application.usecase;

import com.fiap.feedback.application.port.in.NotifyUrgentFeedback;
import com.fiap.feedback.application.port.out.EmailSender;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Notify-urgent-feedback use case: renders the alert e-mail and dispatches it
 * via the {@link EmailSender} port.
 *
 * <p>The body contains the fields required by the Tech Challenge brief
 * (descrição, urgência, data de envio) plus the numeric rating ({@code nota})
 * that triggered the critical classification.</p>
 */
@ApplicationScoped
public class NotifyUrgentFeedbackService implements NotifyUrgentFeedback {

    private static final Logger LOG = Logger.getLogger(NotifyUrgentFeedbackService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss", new Locale("pt", "BR"))
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private final EmailSender mailer;

    @Inject
    public NotifyUrgentFeedbackService(EmailSender mailer) {
        this.mailer = mailer;
    }

    @Override
    public void handle(Command command) {
        String subject = "[FIAP Feedback] Aviso de urgência: " + command.urgencia();
        String body = renderBody(command);
        mailer.send(subject, body, command.dedupeKey());
        LOG.infof("Sent urgent-feedback email for id=%s urgencia=%s",
                command.evaluationId(), command.urgencia());
    }

    private String renderBody(Command command) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#c0392b;">Feedback urgente recebido</h2>
                    <table cellpadding="6" cellspacing="0" border="0">
                      <tr><td><b>Descrição:</b></td><td>%s</td></tr>
                      <tr><td><b>Nota:</b></td><td>%d</td></tr>
                      <tr><td><b>Urgência:</b></td><td>%s</td></tr>
                      <tr><td><b>Data de envio:</b></td><td>%s</td></tr>
                    </table>
                    <p style="color:#888; font-size:12px;">
                      ID da avaliação: %s<br/>
                      Esta mensagem foi enviada automaticamente pela plataforma FIAP Feedback.
                    </p>
                  </body>
                </html>
                """.formatted(
                escapeHtml(command.descricao()),
                command.nota(),
                command.urgencia(),
                DATE_FMT.format(command.dataEnvio()),
                command.evaluationId());
    }

    private static String escapeHtml(String s) {
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
