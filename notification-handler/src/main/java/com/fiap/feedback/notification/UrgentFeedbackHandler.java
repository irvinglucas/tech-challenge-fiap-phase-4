package com.fiap.feedback.notification;

import com.fiap.feedback.application.port.in.NotifyUrgentFeedback;
import com.fiap.feedback.domain.Urgency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.functions.CloudEventsFunction;

import io.cloudevents.CloudEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Inbound adapter for Cloud Functions Gen2 + Pub/Sub triggers.
 *
 * <p>Pub/Sub Gen2 delivers each message wrapped in a CloudEvent of type
 * {@code google.cloud.pubsub.topic.v1.messagePublished}. The event data is a
 * JSON envelope of shape:</p>
 * <pre>{@code
 * {
 *   "message": {
 *     "messageId": "...",
 *     "data": "<base64 of the original Pub/Sub message body>",
 *     "attributes": { ... }
 *   },
 *   "subscription": "..."
 * }
 * }</pre>
 *
 * <p>The decoded body is the JSON we publish from
 * {@code PubSubEventPublisher}; we feed it to the
 * {@link NotifyUrgentFeedback} use case and let the application layer take
 * care of e-mail formatting.</p>
 */
@Named("notification-handler")
@ApplicationScoped
public class UrgentFeedbackHandler implements CloudEventsFunction {

    private static final Logger LOG = Logger.getLogger(UrgentFeedbackHandler.class);

    private final NotifyUrgentFeedback useCase;
    private final ObjectMapper mapper;

    @Inject
    public UrgentFeedbackHandler(NotifyUrgentFeedback useCase, ObjectMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Override
    public void accept(CloudEvent event) throws Exception {
        if (event.getData() == null) {
            LOG.warn("Received CloudEvent with no data; skipping");
            return;
        }

        JsonNode envelope = mapper.readTree(event.getData().toBytes());
        JsonNode message = envelope.path("message");
        String messageId = textOrNull(message, "messageId");
        String dataBase64 = textOrNull(message, "data");
        if (dataBase64 == null) {
            LOG.warn("Pub/Sub envelope is missing 'message.data'; skipping");
            return;
        }

        byte[] payloadBytes = Base64.getDecoder().decode(dataBase64);
        JsonNode payload = mapper.readTree(payloadBytes);

        String evaluationId = Objects.requireNonNull(
                textOrNull(payload, "evaluationId"),
                "payload.evaluationId");
        String descricao = Objects.requireNonNull(
                textOrNull(payload, "descricao"),
                "payload.descricao");
        Urgency urgencia = Urgency.valueOf(Objects.requireNonNull(
                textOrNull(payload, "urgencia"),
                "payload.urgencia"));
        Instant dataEnvio = Instant.parse(Objects.requireNonNull(
                textOrNull(payload, "dataEnvio"),
                "payload.dataEnvio"));

        String dedupeKey = messageId != null
                ? messageId
                : event.getId();

        LOG.infof("Received urgent-feedback event: evaluationId=%s urgencia=%s dedupe=%s",
                evaluationId, urgencia, dedupeKey);

        useCase.handle(new NotifyUrgentFeedback.Command(
                evaluationId, descricao, urgencia, dataEnvio, dedupeKey));
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
