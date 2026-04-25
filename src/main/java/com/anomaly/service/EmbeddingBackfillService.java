package com.anomaly.service;

import com.anomaly.model.AnomalyEvent;
import com.anomaly.repository.AnomalyRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmbeddingBackfillService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingBackfillService.class);

    private final EmbeddingService embeddingService;
    private final AnomalyRepository anomalyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EmbeddingBackfillService(EmbeddingService embeddingService, AnomalyRepository anomalyRepository) {
        this.embeddingService = embeddingService;
        this.anomalyRepository = anomalyRepository;
    }

    /**
     * Automatically triggered at startup to backfill embeddings for all
     * existing records where embedding IS NULL.
     */
    @PostConstruct
    public void runBackfill() {
        log.info("Backfilling embeddings...");
        backfill();
    }

    /**
     * Backfills missing embeddings in small batches. Loops until no NULL
     * embeddings remain.
     */
    @Transactional
    public void backfill() {
        int batchSize = 20;
        int totalProcessed = 0;

        log.info("Starting embedding backfill process...");

        while (true) {
            try {
                // Fetch up to batchSize anomalies where embedding is null.
                // Uses EntityManager directly to avoid modifying AnomalyRepository.
                List<AnomalyEvent> batch = entityManager.createQuery(
                        "SELECT a FROM AnomalyEvent a WHERE a.embedding IS NULL", AnomalyEvent.class)
                        .setMaxResults(batchSize)
                        .getResultList();

                // No more NULL embeddings — backfill complete
                if (batch.isEmpty()) {
                    break;
                }

                for (AnomalyEvent anomaly : batch) {
                    if (anomaly.getEmbedding() != null && !anomaly.getEmbedding().isEmpty()) {
                        continue; // Skip if already populated
                    }

                    String textToEmbed = formatText(anomaly);
                    List<Double> embedding = embeddingService.getEmbedding(textToEmbed);

                    if (embedding != null && !embedding.isEmpty()) {
                        anomaly.setEmbedding(embedding);
                        anomalyRepository.save(anomaly);
                        totalProcessed++;
                    } else {
                        // Skip — do NOT store [0.0] placeholder as it pollutes retrieval
                        log.warn("Embedding API returned empty for anomaly id={}. Skipping.", anomaly.getId());
                    }
                }

                entityManager.flush();
                entityManager.clear();

                log.info("Processed batch. Total backfilled so far: {}", totalProcessed);

            } catch (Exception e) {
                log.error("Error during embedding backfill: {}", e.getMessage());
                // Break to prevent infinite retry on systemic API failure
                break;
            }
        }

        log.info("Embedding backfill complete. Total records backfilled: {}", totalProcessed);
    }

    private String formatText(AnomalyEvent anomaly) {
        return "Type: " + anomaly.getAnomalyType() +
               ". Severity: " + anomaly.getSeverity() +
               ". Description: " + anomaly.getDescription();
    }
}
