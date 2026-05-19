package com.fiap.feedback.infrastructure.firestore;

import com.fiap.feedback.application.port.out.EvaluationRepository;
import com.fiap.feedback.domain.Evaluation;
import com.fiap.feedback.domain.ReportPeriod;
import com.fiap.feedback.domain.Urgency;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Firestore adapter for {@link EvaluationRepository}. Persists each
 * {@link Evaluation} as a document in the configured collection (default
 * {@code evaluations}) keyed by its UUID.
 *
 * <p>Field mapping:
 * <ul>
 *   <li>{@code descricao} &rarr; string</li>
 *   <li>{@code nota}      &rarr; long</li>
 *   <li>{@code urgencia}  &rarr; string ({@link Urgency#name()})</li>
 *   <li>{@code dataEnvio} &rarr; {@link Timestamp}</li>
 * </ul>
 *
 * <p>The Firestore SDK throws checked exceptions; we wrap them as runtime so
 * the use cases stay clean.</p>
 */
@ApplicationScoped
public class FirestoreEvaluationRepository implements EvaluationRepository {

    private static final Logger LOG = Logger.getLogger(FirestoreEvaluationRepository.class);

    private static final String F_DESCRICAO = "descricao";
    private static final String F_NOTA = "nota";
    private static final String F_URGENCIA = "urgencia";
    private static final String F_DATA_ENVIO = "dataEnvio";

    private final Firestore firestore;
    private final String collection;

    @Inject
    public FirestoreEvaluationRepository(
            Firestore firestore,
            @ConfigProperty(name = "feedback.firestore.collection", defaultValue = "evaluations")
            String collection) {
        this.firestore = firestore;
        this.collection = collection;
    }

    @Override
    public Evaluation save(Evaluation evaluation) {
        Map<String, Object> doc = new HashMap<>();
        doc.put(F_DESCRICAO, evaluation.descricao());
        doc.put(F_NOTA, (long) evaluation.nota());
        doc.put(F_URGENCIA, evaluation.urgencia().name());
        doc.put(F_DATA_ENVIO, Timestamp.ofTimeSecondsAndNanos(
                evaluation.dataEnvio().getEpochSecond(),
                evaluation.dataEnvio().getNano()));

        try {
            firestore.collection(collection)
                    .document(evaluation.id())
                    .set(doc)
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreOperationException("Interrupted while saving evaluation", e);
        } catch (ExecutionException e) {
            throw new FirestoreOperationException("Failed to save evaluation " + evaluation.id(), e.getCause());
        }
        return evaluation;
    }

    @Override
    public List<Evaluation> findByPeriod(ReportPeriod period) {
        Timestamp from = Timestamp.ofTimeSecondsAndNanos(period.from().getEpochSecond(), period.from().getNano());
        Timestamp toExclusive = Timestamp.ofTimeSecondsAndNanos(period.to().getEpochSecond(), period.to().getNano());

        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(collection)
                    .whereGreaterThanOrEqualTo(F_DATA_ENVIO, from)
                    .whereLessThan(F_DATA_ENVIO, toExclusive)
                    .orderBy(F_DATA_ENVIO, Query.Direction.ASCENDING)
                    .get()
                    .get()
                    .getDocuments();
            LOG.infof("Found %d evaluations in period %s — %s", docs.size(), period.from(), period.to());
            return docs.stream().map(this::toDomain).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreOperationException("Interrupted while querying evaluations", e);
        } catch (ExecutionException e) {
            throw new FirestoreOperationException("Failed to query evaluations", e.getCause());
        }
    }

    private Evaluation toDomain(QueryDocumentSnapshot doc) {
        Timestamp ts = doc.getTimestamp(F_DATA_ENVIO);
        Instant dataEnvio = ts == null
                ? Instant.EPOCH
                : Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
        return Evaluation.rehydrate(
                doc.getId(),
                doc.getString(F_DESCRICAO),
                Math.toIntExact(doc.getLong(F_NOTA)),
                Urgency.valueOf(doc.getString(F_URGENCIA)),
                dataEnvio);
    }

    public static final class FirestoreOperationException extends RuntimeException {
        public FirestoreOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
