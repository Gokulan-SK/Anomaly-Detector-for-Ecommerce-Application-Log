package com.anomaly.service;

import com.anomaly.model.AnomalyEvent;
import com.anomaly.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAnomalies", reportRepository.count());
        summary.put("severityCounts", mapQueryResults(reportRepository.countBySeverity()));
        summary.put("typeCounts", mapQueryResults(reportRepository.countByAnomalyType()));
        summary.put("uniqueEndpoints", reportRepository.countDistinctEndpoints());
        return summary;
    }

    public List<AnomalyEvent> getRecentCritical() {
        return reportRepository.findTop10BySeverityOrderByCreatedAtDesc("CRITICAL");
    }

    public Map<String, Long> getEndpointDistribution() {
        return mapQueryResults(reportRepository.countBySourceLayer());
    }

    public List<Map<String, Object>> getTrend(String interval) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        List<AnomalyEvent> recentAnomalies = reportRepository.findByCreatedAtAfter(oneHourAgo);

        DateTimeFormatter formatter;
        if ("hour".equalsIgnoreCase(interval)) {
            formatter = DateTimeFormatter.ofPattern("HH:00").withZone(ZoneId.systemDefault());
        } else {
            // default to minute
            formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
        }

        Map<String, Long> trendMap = recentAnomalies.stream()
                .filter(a -> a.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        a -> formatter.format(a.getCreatedAt()),
                        TreeMap::new,
                        Collectors.counting()
                ));

        List<Map<String, Object>> trendList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : trendMap.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", entry.getKey());
            point.put("count", entry.getValue());
            trendList.add(point);
        }
        return trendList;
    }

    private Map<String, Long> mapQueryResults(List<Object[]> results) {
        Map<String, Long> map = new HashMap<>();
        if (results == null) return map;
        for (Object[] row : results) {
            String key = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            map.put(key, count);
        }
        return map;
    }
}
