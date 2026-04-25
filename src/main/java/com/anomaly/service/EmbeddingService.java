package com.anomaly.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final WebClient webClient;

    @Value("${gemini.api.key:}")
    private String apiKey;

    public EmbeddingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
    }

    /**
     * Calls the Gemini embedding API and returns the embedding vector.
     * Returns an empty list on any failure.
     */
    public List<Double> getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Rate limit control (~20 req/s max)
            if (text.length() > 5) {
                Thread.sleep(50);
            }

            // Fix 5: Verified correct request body format for Gemini embedding API
            Map<String, Object> requestBody = Map.of(
                    "content", Map.of(
                            "parts", List.of(Map.of("text", text))
                    )
            );

            // Fix 6: Exact endpoint for gemini-embedding-001
            JsonNode response = webClient.post()
                    .uri("/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            // Fix 1: Log raw API response for debugging
            log.info("Embedding API raw response: {}", response);

            // Fix 2 & 3: Safe extraction via response.get("embedding").get("values")
            if (response != null && response.has("embedding")) {
                JsonNode embeddingNode = response.get("embedding");
                if (embeddingNode.has("values") && embeddingNode.get("values").isArray()) {
                    List<Double> embeddingValues = new ArrayList<>();
                    for (JsonNode valNode : embeddingNode.get("values")) {
                        embeddingValues.add(valNode.asDouble());
                    }
                    log.info("Embedding extracted successfully. Dimensions: {}", embeddingValues.size());
                    return embeddingValues;
                } else {
                    log.warn("Embedding node exists but 'values' field is missing or not an array. Node: {}", embeddingNode);
                }
            } else {
                log.warn("Response missing 'embedding' field. Full response: {}", response);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Embedding API thread sleep interrupted");
        } catch (WebClientResponseException e) {
            log.error("Embedding API HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Embedding API call failed: {}", e.getMessage(), e);
        }

        // Fix 4: Log when returning empty embedding
        log.warn("Empty embedding returned for input: {}", text);
        return Collections.emptyList();
    }
}
