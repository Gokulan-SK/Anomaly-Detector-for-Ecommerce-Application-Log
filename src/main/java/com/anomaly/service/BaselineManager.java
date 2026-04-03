package com.anomaly.service;

import com.anomaly.model.LogEvent;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class BaselineManager {

    private final ConcurrentHashMap<String, EndpointStats> endpointStatsMap = new ConcurrentHashMap<>();

    public void update(LogEvent event) {
        if (event.getTimestamp() == null) {
            return;
        }
        String endpoint = event.getEndpoint() != null ? event.getEndpoint() : "UNKNOWN_ENDPOINT";
        EndpointStats stats = endpointStatsMap.computeIfAbsent(endpoint, k -> new EndpointStats());
        boolean isError = event.getHttpStatus() != null && event.getHttpStatus() >= 400;
        stats.addEvent(event.getTimestamp(), event.getLatencyMs(), isError);
    }

    public EndpointStats getStats(String endpoint) {
        return endpointStatsMap.get(endpoint);
    }

    public double getBaselineLatency(String endpoint) {
        EndpointStats stats = endpointStatsMap.get(endpoint);
        return stats != null ? stats.getLatencyAverage() : 0.0;
    }

    public double getBaselineRate(String endpoint) {
        EndpointStats stats = endpointStatsMap.get(endpoint);
        return stats != null ? stats.getRate() : 0.0;
    }

    public double getBaselineErrorRate(String endpoint) {
        EndpointStats stats = endpointStatsMap.get(endpoint);
        return stats != null ? stats.getErrorRate() : 0.0;
    }

    public boolean isBaselineReady(String endpoint) {
        EndpointStats stats = endpointStatsMap.get(endpoint);
        return stats != null && stats.isBaselineReady();
    }
}
