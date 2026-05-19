package com.fiap.feedback.application.usecase;

import com.fiap.feedback.application.port.in.GenerateWeeklyReport;
import com.fiap.feedback.application.port.out.Clock;
import com.fiap.feedback.application.port.out.EmailSender;
import com.fiap.feedback.application.port.out.EvaluationRepository;
import com.fiap.feedback.domain.Evaluation;
import com.fiap.feedback.domain.ReportPeriod;
import com.fiap.feedback.domain.Urgency;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Weekly-report use case. Computes the 7-day window ending at "now", asks the
 * repository for all evaluations in that window, aggregates per-day and
 * per-urgency counts plus the overall average rating, and emails the report.
 */
@ApplicationScoped
public class GenerateWeeklyReportService implements GenerateWeeklyReport {

    private static final Logger LOG = Logger.getLogger(GenerateWeeklyReportService.class);

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy", new Locale("pt", "BR")).withZone(SAO_PAULO);
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm", new Locale("pt", "BR")).withZone(SAO_PAULO);

    private final EvaluationRepository repository;
    private final EmailSender mailer;
    private final Clock clock;

    @Inject
    public GenerateWeeklyReportService(EvaluationRepository repository,
                                       EmailSender mailer,
                                       Clock clock) {
        this.repository = repository;
        this.mailer = mailer;
        this.clock = clock;
    }

    @Override
    public Summary handle() {
        ReportPeriod period = ReportPeriod.lastSevenDaysEndingAt(clock.now());
        List<Evaluation> evaluations = repository.findByPeriod(period);

        List<Entry> entries = evaluations.stream()
                .map(e -> new Entry(e.descricao(), e.urgencia(), e.dataEnvio()))
                .toList();

        Map<LocalDate, Long> countPerDay = new TreeMap<>(evaluations.stream()
                .collect(Collectors.groupingBy(
                        e -> e.dataEnvio().atZone(SAO_PAULO).toLocalDate(),
                        Collectors.counting())));

        Map<Urgency, Long> countPerUrgency = new EnumMap<>(Urgency.class);
        for (Urgency u : Urgency.values()) {
            countPerUrgency.put(u, 0L);
        }
        evaluations.forEach(e -> countPerUrgency.merge(e.urgencia(), 1L, Long::sum));

        double avg = evaluations.stream()
                .mapToInt(Evaluation::nota)
                .average()
                .orElse(0.0);

        Summary summary = new Summary(period.from(), period.to(),
                entries, countPerDay, countPerUrgency, avg);

        String subject = "[FIAP Feedback] Relatório semanal "
                + DATE_FMT.format(period.from()) + " - " + DATE_FMT.format(period.to());
        mailer.send(subject, renderHtml(summary),
                "weekly-report-" + period.to().getEpochSecond());

        LOG.infof("Weekly report generated: %d evaluations, average=%.2f",
                evaluations.size(), avg);
        return summary;
    }

    private String renderHtml(Summary s) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<html><body style='font-family:Arial,sans-serif;'>");
        sb.append("<h2>Relatório semanal de feedbacks</h2>");
        sb.append("<p><b>Período:</b> ")
                .append(DATETIME_FMT.format(s.periodFrom())).append(" — ")
                .append(DATETIME_FMT.format(s.periodTo())).append("</p>");
        sb.append("<p><b>Total de avaliações:</b> ").append(s.entries().size())
                .append(" — <b>Média:</b> ")
                .append(String.format(Locale.ROOT, "%.2f", s.averageNota()))
                .append("</p>");

        sb.append("<h3>Avaliações por dia</h3><ul>");
        s.countPerDay().forEach((day, count) ->
                sb.append("<li>").append(DATE_FMT.format(day.atStartOfDay(SAO_PAULO).toInstant()))
                        .append(": ").append(count).append("</li>"));
        sb.append("</ul>");

        sb.append("<h3>Avaliações por urgência</h3><ul>");
        s.countPerUrgency().forEach((u, count) ->
                sb.append("<li>").append(u).append(": ").append(count).append("</li>"));
        sb.append("</ul>");

        sb.append("<h3>Detalhamento</h3>")
                .append("<table cellpadding='6' cellspacing='0' border='1'>")
                .append("<tr><th>Descrição</th><th>Urgência</th><th>Data de envio</th></tr>");
        s.entries().forEach(e -> sb.append("<tr><td>")
                .append(escapeHtml(e.descricao())).append("</td><td>")
                .append(e.urgencia()).append("</td><td>")
                .append(DATETIME_FMT.format(e.dataEnvio())).append("</td></tr>"));
        sb.append("</table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
