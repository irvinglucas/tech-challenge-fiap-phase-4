package com.fiap.feedback.infrastructure.pubsub;

import com.fiap.feedback.application.port.out.EventPublisher;
import com.fiap.feedback.domain.Evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import io.quarkiverse.googlecloudservices.pubsub.QuarkusPubSub;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Pub/Sub adapter for {@link EventPublisher}. Publishes a JSON payload
 * representing the {@link Evaluation} to the topic configured by
 * {@code feedback.pubsub.urgent-topic} (default {@code urgent-feedback}).
 *
 * <p>A single {@link Publisher} is cached per topic name and properly
 * shut down at application stop.</p>
 */
@ApplicationScoped
public class PubSubEventPublisher implements EventPublisher {

    private static final Logger LOG = Logger.getLogger(PubSubEventPublisher.class);

    private final QuarkusPubSub pubsub;
    private final ObjectMapper mapper;
    private final String topic;
    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    @Inject
    public PubSubEventPublisher(
            QuarkusPubSub pubsub,
            ObjectMapper mapper,
            @ConfigProperty(name = "feedback.pubsub.urgent-topic", defaultValue = "urgent-feedback")
            String topic) {
        this.pubsub = pubsub;
        this.mapper = mapper;
        this.topic = topic;
    }

    @Override
    public void publishUrgent(Evaluation evaluation) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("evaluationId", evaluation.id());
        payload.put("descricao", evaluation.descricao());
        payload.put("nota", evaluation.nota());
        payload.put("urgencia", evaluation.urgencia().name());
        payload.put("dataEnvio", evaluation.dataEnvio().toString());

        byte[] data;
        try {
            data = mapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new PubSubOperationException("Failed to serialize urgent-feedback payload", e);
        }

        Publisher publisher = publishers.computeIfAbsent(topic, this::buildPublisher);
        PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(data))
                .putAttributes("evaluationId", evaluation.id())
                .putAttributes("urgencia", evaluation.urgencia().name())
                .build();

        try {
            ApiFuture<String> future = publisher.publish(message);
            String messageId = future.get();
            LOG.infof("Published urgent-feedback message id=%s for evaluation=%s",
                    messageId, evaluation.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PubSubOperationException("Interrupted while publishing", e);
        } catch (ExecutionException e) {
            throw new PubSubOperationException("Failed to publish urgent-feedback event", e.getCause());
        }
    }

    private Publisher buildPublisher(String topicName) {
        try {
            return pubsub.publisher(topicName);
        } catch (IOException e) {
            throw new PubSubOperationException("Failed to build Pub/Sub publisher for " + topicName, e);
        }
    }

    @PreDestroy
    void shutdown() {
        publishers.values().forEach(p -> {
            try {
                p.shutdown();
            } catch (Exception ignored) {
                // best-effort during shutdown
            }
        });
    }

    public static final class PubSubOperationException extends RuntimeException {
        public PubSubOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
