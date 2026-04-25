package com.anomaly.service;

import com.anomaly.model.AnomalyEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RagAnswerService {

    private static final Logger log = LoggerFactory.getLogger(RagAnswerService.class);

    private static final int MAX_RETRIES = 2;
    private static final long MIN_CALL_INTERVAL = 12000L; // 12s between calls
    private static final long RETRY_DELAY_MS = 15000L; // 15s on 429

    private static final AtomicLong lastCallTime = new AtomicLong(0);

    private final RagService ragService;
    private final WebClient webClient;

    @Value("${gemini.api.key:}")
    private String apiKey;

    public RagAnswerService(RagService ragService, WebClient.Builder webClientBuilder) {
        this.ragService = ragService;
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
    }

    /**
     * Answers a user query by providing RAG context to Gemini 2.0 flash.
     * Enforces a strict 12-second inter-call delay and retries up to 2 times on
     * 429.
     */
    public String answer(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "Invalid question.";
        }

        log.info("Gemini API Key loaded: {}", apiKey != null && !apiKey.isBlank() ? "[SET]" : "[MISSING]");
        if (apiKey == null || apiKey.isBlank()) {
            return "AI service is not configured properly.";
        }

        List<AnomalyEvent> anomalies = ragService.retrieve(question);
        if (anomalies.isEmpty()) {
            return "No relevant anomalies found to answer your question.";
        }

        // Build context string
        StringBuilder contextBuilder = new StringBuilder();
        for (AnomalyEvent anomaly : anomalies) {
            contextBuilder.append("- Type: ").append(anomaly.getAnomalyType())
                    .append(" | Severity: ").append(anomaly.getSeverity())
                    .append(" | Endpoint: ").append(anomaly.getSourceLayer())
                    .append(" | Score: ").append(anomaly.getScore())
                    .append(" | Username: ").append(anomaly.getUsername())
                    .append(" | Session: ").append(anomaly.getSessionId())
                    .append(" | Detected At: ").append(anomaly.getCreatedAt())
                    .append(" | Description: ").append(anomaly.getDescription())
                    .append("\n");
        }
        String contextStr = contextBuilder.toString();
        if (contextStr.length() > 2000) {
            contextStr = contextStr.substring(0, 2000) + "...";
        }
        // Keep a final reference for use in the lambda/inner scope if needed
        final String context = contextStr;

        String prompt = "You are an anomaly analysis assistant.\n\n" +
                "Use ONLY the provided context to answer the question.\n" +
                "If the context is insufficient, clearly say so.\n\n" +
                "Context:\n" + context + "\n\nQuestion:\n" + question;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))));

        // Attempt loop — initial call + up to MAX_RETRIES on 429
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            if (attempt > 0) {
                // Retry path: flat 15s wait, skip the 12s inter-call check
                try {
                    log.warn("Rate limit hit, retrying after 15s delay... attempt {}", attempt);
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", ie);
                }
            }

            try {
                JsonNode response;

                // Strict rate-limit guard: only one thread proceeds at a time
                synchronized (RagAnswerService.class) {
                    long now = System.currentTimeMillis();
                    long waitTime = MIN_CALL_INTERVAL - (now - lastCallTime.get());
                    if (waitTime > 0) {
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during rate-limit delay", ie);
                        }
                    }
                    lastCallTime.set(System.currentTimeMillis());

                    log.info("Gemini call allowed. Proceeding with API request.");

                    response = webClient.post()
                            .uri("/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                            .header("Content-Type", "application/json")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block();
                }

                log.info("Gemini raw response: {}", response);

                // Detect safety-filtered / empty response:
                // Gemini omits "candidates" entirely when the output is blocked.
                if (response != null && !response.has("candidates")) {
                    // Log promptFeedback block reason if present
                    JsonNode feedback = response.path("promptFeedback");
                    if (!feedback.isMissingNode()) {
                        log.warn("Gemini blocked response. promptFeedback: {}", feedback);
                    } else {
                        log.warn("Gemini returned no candidates and no promptFeedback. Full response: {}", response);
                    }
                    return "The AI could not generate a response for this query. Here are the relevant anomalies:\n"
                            + context;
                }

                // Null-safe parsing of candidates
                if (response != null &&
                        response.has("candidates") &&
                        response.get("candidates").isArray() &&
                        response.get("candidates").size() > 0) {

                    JsonNode candidate = response.get("candidates").get(0);

                    // Check finishReason — SAFETY or OTHER means no usable content
                    String finishReason = candidate.path("finishReason").asText("");
                    if ("SAFETY".equals(finishReason) || "OTHER".equals(finishReason)) {
                        log.warn("Gemini candidate blocked. finishReason={}", finishReason);
                        return "The AI could not generate a response for this query. Here are the relevant anomalies:\n"
                                + context;
                    }

                    JsonNode parts = candidate.path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText("No answer generated.");
                    }
                }

                return "AI response could not be generated, but anomalies were found.";

            } catch (WebClientResponseException.TooManyRequests e) {
                if (attempt == MAX_RETRIES) {
                    break; // exhausted — fall through to fallback
                }
                // Loop will retry with the 15s delay at the top of the next iteration

            } catch (WebClientResponseException e) {
                log.error("Gemini API HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
                log.error("Gemini API failure", e);
                throw e; // let non-429 HTTP errors propagate as-is

            } catch (RuntimeException e) {
                // Covers InterruptedException rethrown above and other runtime issues
                throw e;
            }
        }

        log.error("Gemini API retry limit reached. Returning fallback response.");
        return "Rate limit reached. Showing recent anomalies instead:\n" + context;
    }
}
