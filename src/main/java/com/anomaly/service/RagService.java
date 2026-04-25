package com.anomaly.service;

import com.anomaly.model.AnomalyEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingService embeddingService;

    @PersistenceContext
    private EntityManager entityManager;

    public RagService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * Retrieves the top 5 anomalies semantically related to the query.
     * Falls back to the 5 most recent anomalies if retrieval returns empty.
     */
    public List<AnomalyEvent> retrieve(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // FIX 6: Fall back to recent anomalies if query embedding fails
        List<Double> queryEmbedding = embeddingService.getEmbedding(query);
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            log.warn("Query embedding is empty. Falling back to most recent anomalies.");
            return getFallbackAnomalies();
        }

        // FIX 1: Relaxed DB query - removed strict JSON_LENGTH filter
        List<AnomalyEvent> recentAnomalies = entityManager.createQuery(
                "SELECT a FROM AnomalyEvent a WHERE a.embedding IS NOT NULL ORDER BY a.createdAt DESC",
                AnomalyEvent.class)
                .setMaxResults(100)
                .getResultList();

        // FIX 3: Log fetched count for debugging
        log.info("Fetched anomalies with embeddings: {}", recentAnomalies.size());

        // FIX 2: Skip only null/empty embeddings; log but safely continue on dimension mismatch
        List<AnomalyEvent> results = recentAnomalies.stream()
                .filter(a -> {
                    if (a.getEmbedding() == null || a.getEmbedding().isEmpty()) {
                        return false; // skip null/empty
                    }
                    if (a.getEmbedding().size() != queryEmbedding.size()) {
                        // FIX 2: Log dimension mismatch instead of silently dropping
                        log.debug("Skipping anomaly id={} due to dimension mismatch: {} vs {}",
                                a.getId(), a.getEmbedding().size(), queryEmbedding.size());
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparingDouble((AnomalyEvent a) ->
                        cosineSimilarity(queryEmbedding, a.getEmbedding())).reversed())
                .peek(a -> log.debug("Anomaly id={} similarity={}", a.getId(),
                        cosineSimilarity(queryEmbedding, a.getEmbedding())))
                .limit(5)
                .collect(Collectors.toList());

        // FIX 4: Fallback to most recent 5 if similarity-based retrieval returns empty
        if (results.isEmpty()) {
            log.warn("Similarity retrieval returned empty. Falling back to most recent anomalies.");
            return recentAnomalies.stream()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        log.info("RAG retrieval returned {} results.", results.size());
        return results;
    }

    /**
     * Returns the 5 most recent anomalies, regardless of embedding status.
     * Used as an absolute fallback when embeddings are unavailable.
     */
    private List<AnomalyEvent> getFallbackAnomalies() {
        List<AnomalyEvent> fallback = entityManager.createQuery(
                "SELECT a FROM AnomalyEvent a ORDER BY a.createdAt DESC",
                AnomalyEvent.class)
                .setMaxResults(5)
                .getResultList();
        log.info("Fallback returned {} anomalies.", fallback.size());
        return fallback;
    }

    /**
     * Computes the cosine similarity between two vectors.
     */
    private double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        int minSize = Math.min(vecA.size(), vecB.size());
        for (int i = 0; i < minSize; i++) {
            double a = vecA.get(i);
            double b = vecB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
