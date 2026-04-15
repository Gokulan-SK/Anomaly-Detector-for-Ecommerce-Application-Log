package com.anomaly.service;

import com.anomaly.model.AnomalyEvent;
import com.anomaly.model.LogEvent;
import com.anomaly.repository.AnomalyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnomalyDetectorService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectorService.class);

    private final BaselineManager baselineManager;
    private final AnomalyRepository anomalyRepository;
    private final Map<String, Deque<LogEvent>> sessionEvents = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OffsetDateTime>> recentAnomalies = new ConcurrentHashMap<>();

    public AnomalyDetectorService(BaselineManager baselineManager, AnomalyRepository anomalyRepository) {
        this.baselineManager = baselineManager;
        this.anomalyRepository = anomalyRepository;
    }

    public Optional<AnomalyEvent> detect(LogEvent logEvent) {
        if ("SYSTEM_EVENT".equals(logEvent.getEventType())) {
            String desc = (logEvent.getExceptionClass() != null && !logEvent.getExceptionClass().isBlank())
                    ? logEvent.getExceptionClass()
                    : logEvent.getMessage();

            AnomalyEvent anomaly = new AnomalyEvent();
            anomaly.setAnomalyType("SYSTEM");
            anomaly.setDescription(desc);
            String rawEndpoint = logEvent.getEndpoint();
            if (rawEndpoint == null || rawEndpoint.trim().isEmpty()) {
                rawEndpoint = "UNKNOWN_ENDPOINT";
            }
            anomaly.setSourceLayer(rawEndpoint);
            anomaly.setSessionId(logEvent.getSessionId());
            anomaly.setCorrelationId(logEvent.getCorrelationId());
            anomaly.setSeverity("CRITICAL");
            if (anomaly != null)
                anomalyRepository.save(anomaly);
            return Optional.of(anomaly);
        }

        baselineManager.update(logEvent);

        String key = logEvent.getSessionId();
        if (key == null || key.trim().isEmpty()) {
            key = logEvent.getCorrelationId();
        }
        if (key == null || key.trim().isEmpty()) {
            key = "anonymous";
            log.debug("Using fallback session key: anonymous");
        }

        Deque<LogEvent> currentDeque = sessionEvents.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (currentDeque) {
            currentDeque.addLast(logEvent);
            while (currentDeque.size() > 100) {
                currentDeque.removeFirst();
            }
            if (logEvent.getTimestamp() != null) {
                OffsetDateTime windowStart = logEvent.getTimestamp().minusSeconds(5);
                while (!currentDeque.isEmpty() && currentDeque.peekFirst().getTimestamp() != null &&
                        currentDeque.peekFirst().getTimestamp().isBefore(windowStart)) {
                    currentDeque.removeFirst();
                }
            }
        }

        if (checkDuplicateAndSkip(key, "SEQUENTIAL", logEvent.getTimestamp())) {
            boolean hasCheckout = false;
            boolean hasPaymentAfter = false;
            synchronized (currentDeque) {
                for (LogEvent e : currentDeque) {
                    if (e.getEndpoint() != null) {
                        if (e.getEndpoint().contains("/orders/checkout")) {
                            hasCheckout = true;
                            hasPaymentAfter = false;
                        } else if (hasCheckout && e.getEndpoint().contains("/payments/")) {
                            hasPaymentAfter = true;
                        }
                    }
                }
            }

            boolean isCurrentCheckout = logEvent.getEndpoint() != null
                    && logEvent.getEndpoint().contains("/orders/checkout");
            if (hasCheckout && !hasPaymentAfter && !isCurrentCheckout) {
                markAnomaly(key, "SEQUENTIAL", logEvent.getTimestamp());
                AnomalyEvent anomaly = buildAnomaly("SEQUENTIAL", "Checkout without payment", logEvent);
                if (anomaly != null)
                    anomalyRepository.save(anomaly);
                return Optional.of(anomaly);
            }
        }

        String endpoint = logEvent.getEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            endpoint = "UNKNOWN_ENDPOINT";
            log.debug("Using fallback endpoint: UNKNOWN_ENDPOINT");
        }
        boolean baselineReady = baselineManager.isBaselineReady(endpoint);
        EndpointStats stats = baselineManager.getStats(endpoint);
        boolean hasTraffic = stats != null && stats.hasSufficientTraffic();

        // 1. Session VOLUME
        if (checkDuplicateAndSkip(key, "VOLUME", logEvent.getTimestamp()) && logEvent.getTimestamp() != null) {
            OffsetDateTime burstThreshold = logEvent.getTimestamp().minusSeconds(3);
            long sessionBurstCount = 0;
            synchronized (currentDeque) {
                sessionBurstCount = currentDeque.stream()
                        .filter(e -> e.getTimestamp() != null && e.getTimestamp().isAfter(burstThreshold))
                        .count();
            }

            double sessionRequestRate = sessionBurstCount / 3.0; // Over last 3 seconds
            double globalRate = stats != null ? stats.getRate() : 0.0;

            if (sessionBurstCount > 30
                    || (hasTraffic && sessionRequestRate > 3.0 * globalRate && sessionRequestRate > 5.0)) {
                markAnomaly(key, "VOLUME", logEvent.getTimestamp());
                AnomalyEvent anomaly = buildAnomaly("VOLUME", "High request burst (possible bot)", logEvent);
                if (anomaly != null)
                    anomalyRepository.save(anomaly);
                return Optional.of(anomaly);
            }
        }

        // 2. Session ERROR
        if (checkDuplicateAndSkip(key, "ERROR", logEvent.getTimestamp())) {
            int errCount = 0;
            synchronized (currentDeque) {
                for (LogEvent e : currentDeque) {
                    if (e.getHttpStatus() != null && e.getHttpStatus() >= 400) {
                        errCount++;
                    }
                }
            }
            double sessionErrorRate = currentDeque.isEmpty() ? 0.0 : (double) errCount / currentDeque.size();
            double globalErrorRate = stats != null ? stats.getErrorRate() : 0.0;

            if (baselineReady && hasTraffic && sessionErrorRate > 2.0 * globalErrorRate) {
                markAnomaly(key, "ERROR", logEvent.getTimestamp());
                AnomalyEvent anomaly = buildAnomaly("ERROR", "Client/endpoint error spike", logEvent);
                if (anomaly != null)
                    anomalyRepository.save(anomaly);
                return Optional.of(anomaly);
            }
        }

        // 3. Session LATENCY
        if (checkDuplicateAndSkip(key, "LATENCY", logEvent.getTimestamp())) {
            Long currentLatency = logEvent.getLatencyMs();
            if (currentLatency != null && baselineReady && hasTraffic && stats != null) {
                double globalAverageLatency = stats.getLatencyAverage();
                if (currentLatency > 2.5 * globalAverageLatency && currentLatency > 100) {
                    markAnomaly(key, "LATENCY", logEvent.getTimestamp());
                    AnomalyEvent anomaly = buildAnomaly("LATENCY", "High response time detected", logEvent);
                    if (anomaly != null)
                        anomalyRepository.save(anomaly);
                    return Optional.of(anomaly);
                }
            }
        }

        // 4. Global Anomalies (Per-Endpoint)
        String globalKey = "global_" + endpoint;
        if (baselineReady && hasTraffic && stats != null) {
            if (checkDuplicateAndSkip(globalKey, "GLOBAL_VOLUME", logEvent.getTimestamp())) {
                double globalRate = stats.getRate();
                double longTermRate = stats.getLongTermRate();
                if (longTermRate > 0.0 && globalRate > 3.0 * longTermRate && globalRate > 0.0) {
                    markAnomaly(globalKey, "GLOBAL_VOLUME", logEvent.getTimestamp());
                    AnomalyEvent anomaly = buildAnomaly("GLOBAL_VOLUME", "Global traffic spike detected", logEvent);
                    if (anomaly != null)
                        anomalyRepository.save(anomaly);
                    return Optional.of(anomaly);
                }
            }

            if (checkDuplicateAndSkip(globalKey, "GLOBAL_ERROR", logEvent.getTimestamp())) {
                double globalErrorRate = stats.getErrorRate();
                double longTermErrorRate = stats.getLongTermErrorRate();
                if (longTermErrorRate > 0.0 && globalErrorRate > 2.0 * longTermErrorRate && globalErrorRate > 0.0) {
                    markAnomaly(globalKey, "GLOBAL_ERROR", logEvent.getTimestamp());
                    AnomalyEvent anomaly = buildAnomaly("GLOBAL_ERROR", "Global error spike detected", logEvent);
                    if (anomaly != null)
                        anomalyRepository.save(anomaly);
                    return Optional.of(anomaly);
                }
            }

            if (checkDuplicateAndSkip(globalKey, "GLOBAL_LATENCY", logEvent.getTimestamp())) {
                double globalAverageLatency = stats.getLatencyAverage();
                double longTermLatency = stats.getLongTermLatencyAverage();
                if (longTermLatency > 0.0 && globalAverageLatency > 2.0 * longTermLatency
                        && globalAverageLatency > 0.0) {
                    markAnomaly(globalKey, "GLOBAL_LATENCY", logEvent.getTimestamp());
                    AnomalyEvent anomaly = buildAnomaly("GLOBAL_LATENCY", "Global latency degradation detected",
                            logEvent);
                    if (anomaly != null)
                        anomalyRepository.save(anomaly);
                    return Optional.of(anomaly);
                }
            }
        }

        return Optional.empty();
    }

    private boolean checkDuplicateAndSkip(String key, String type, OffsetDateTime now) {
        if (now == null)
            return true;
        Map<String, OffsetDateTime> typedAnomalies = recentAnomalies.computeIfAbsent(key,
                k -> new ConcurrentHashMap<>());
        OffsetDateTime lastTrigger = typedAnomalies.get(type);
        if (lastTrigger == null) {
            return true;
        }
        return now.isAfter(lastTrigger.plusSeconds(5));
    }

    private void markAnomaly(String key, String type, OffsetDateTime now) {
        if (now != null) {
            recentAnomalies.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(type, now);
        }
    }

    private AnomalyEvent buildAnomaly(String type, String reason, LogEvent logEvent) {
        AnomalyEvent anomaly = new AnomalyEvent();
        anomaly.setAnomalyType(type);
        anomaly.setDescription(reason);
        String rawEndpoint = logEvent.getEndpoint();
        if (rawEndpoint == null || rawEndpoint.trim().isEmpty()) {
            rawEndpoint = "UNKNOWN_ENDPOINT";
        }
        anomaly.setSourceLayer(rawEndpoint);
        anomaly.setSessionId(logEvent.getSessionId());
        anomaly.setCorrelationId(logEvent.getCorrelationId());

        int score = 0;
        try {
            String endpoint = logEvent.getEndpoint();
            if (endpoint == null || endpoint.trim().isEmpty()) {
                endpoint = "UNKNOWN_ENDPOINT";
            }
            EndpointStats stats = baselineManager.getStats(endpoint);
            boolean hasStats = (stats != null);

            String key = logEvent.getSessionId();
            if (key == null || key.trim().isEmpty()) {
                key = logEvent.getCorrelationId();
            }
            if (key == null || key.trim().isEmpty()) {
                key = "anonymous";
            }
            Deque<LogEvent> currentDeque = sessionEvents.get(key);

            if ("LATENCY".equals(type)) {
                long currentLatency = logEvent.getLatencyMs() == null ? 0 : logEvent.getLatencyMs();
                double baselineLatency = hasStats ? stats.getLatencyAverage() : 0.0;
                double ratio = baselineLatency > 0 ? (double) currentLatency / baselineLatency : 1.0;
                score = (int) Math.min(100, ratio * 20);

            } else if ("ERROR".equals(type)) {
                double sessionErrorRate = 0.0;
                if (currentDeque != null && !currentDeque.isEmpty()) {
                    int errCount = 0;
                    synchronized (currentDeque) {
                        for (LogEvent e : currentDeque) {
                            if (e.getHttpStatus() != null && e.getHttpStatus() >= 400) {
                                errCount++;
                            }
                        }
                    }
                    sessionErrorRate = (double) errCount / currentDeque.size();
                }
                score = (int) Math.min(100, sessionErrorRate * 100);

            } else if ("VOLUME".equals(type)) {
                long sessionBurstCount = 0;
                if (currentDeque != null && logEvent.getTimestamp() != null) {
                    OffsetDateTime burstThreshold = logEvent.getTimestamp().minusSeconds(3);
                    synchronized (currentDeque) {
                        sessionBurstCount = currentDeque.stream()
                                .filter(e -> e.getTimestamp() != null && e.getTimestamp().isAfter(burstThreshold))
                                .count();
                    }
                }
                double sessionRequestRate = sessionBurstCount / 3.0;
                score = (int) Math.min(100, (sessionBurstCount * 2) + (sessionRequestRate * 5));

            } else if (type.startsWith("GLOBAL_")) {
                double baseScore = 70.0;
                double deviation = 1.0;

                if (hasStats) {
                    if ("GLOBAL_VOLUME".equals(type)) {
                        double currentMetric = stats.getRate();
                        double baselineMetric = stats.getLongTermRate();
                        deviation = baselineMetric > 0 ? currentMetric / baselineMetric : 1.0;
                    } else if ("GLOBAL_ERROR".equals(type)) {
                        double currentMetric = stats.getErrorRate();
                        double baselineMetric = stats.getLongTermErrorRate();
                        deviation = baselineMetric > 0 ? currentMetric / baselineMetric : 1.0;
                    } else if ("GLOBAL_LATENCY".equals(type)) {
                        double currentMetric = stats.getLatencyAverage();
                        double baselineMetric = stats.getLongTermLatencyAverage();
                        deviation = baselineMetric > 0 ? currentMetric / baselineMetric : 1.0;
                    }
                }
                score = (int) Math.min(100, baseScore + (deviation * 10));

            } else if ("SYSTEM".equals(type) || "SEQUENTIAL".equals(type)) {
                score = 100;
            }
        } catch (Exception e) {
            score = 0;
        }

        score = Math.max(0, Math.min(100, score));
        anomaly.setScore(score);

        if (score <= 30) {
            anomaly.setSeverity("LOW");
        } else if (score <= 60) {
            anomaly.setSeverity("MEDIUM");
        } else if (score <= 80) {
            anomaly.setSeverity("HIGH");
        } else {
            anomaly.setSeverity("CRITICAL");
        }

        return anomaly;
    }
}
