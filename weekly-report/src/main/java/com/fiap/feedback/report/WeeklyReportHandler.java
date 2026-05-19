package com.fiap.feedback.report;

import com.fiap.feedback.application.port.in.GenerateWeeklyReport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

import java.io.BufferedWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inbound HTTP adapter for the {@link GenerateWeeklyReport} use case.
 *
 * <p>Invoked by Cloud Scheduler with an OIDC-authenticated request. The HTTP
 * method/body are ignored; the use case derives the 7-day window from the
 * injected clock and dispatches the report e-mail.</p>
 *
 * <p>Returns a small JSON summary so the scheduler logs (and the demo video)
 * show the totals at a glance.</p>
 */
@Named("weekly-report")
@ApplicationScoped
public class WeeklyReportHandler implements HttpFunction {

    private static final Logger LOG = Logger.getLogger(WeeklyReportHandler.class);

    private final GenerateWeeklyReport useCase;
    private final ObjectMapper mapper;

    @Inject
    public WeeklyReportHandler(GenerateWeeklyReport useCase, ObjectMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        LOG.infof("Weekly report invoked: method=%s path=%s",
                request.getMethod(), request.getUri());

        GenerateWeeklyReport.Summary summary;
        try {
            summary = useCase.handle();
        } catch (RuntimeException e) {
            LOG.error("Weekly report failed", e);
            response.setStatusCode(500);
            response.setContentType("application/json");
            try (BufferedWriter w = response.getWriter()) {
                w.write("{\"status\":\"error\",\"message\":\""
                        + escape(e.getMessage()) + "\"}");
            }
            return;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("periodFrom", summary.periodFrom().toString());
        body.put("periodTo", summary.periodTo().toString());
        body.put("totalEvaluations", summary.entries().size());
        body.put("countPerUrgency", summary.countPerUrgency());
        body.put("averageNota", summary.averageNota());

        response.setStatusCode(200);
        response.setContentType("application/json");
        try (BufferedWriter w = response.getWriter()) {
            w.write(mapper.writeValueAsString(body));
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
