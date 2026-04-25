package com.anomaly.controller;

import com.anomaly.model.AnomalyEvent;
import com.anomaly.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "http://localhost:5173") // Allows React to call
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(reportService.getSummary());
    }

    @GetMapping("/trend")
    public ResponseEntity<List<Map<String, Object>>> getTrend(@RequestParam(defaultValue = "minute") String interval) {
        return ResponseEntity.ok(reportService.getTrend(interval));
    }

    @GetMapping("/critical")
    public ResponseEntity<List<AnomalyEvent>> getRecentCritical() {
        return ResponseEntity.ok(reportService.getRecentCritical());
    }

    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Long>> getEndpointDistribution() {
        return ResponseEntity.ok(reportService.getEndpointDistribution());
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportReport() {
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("generatedAt", Instant.now().toString());
        exportData.put("summary", reportService.getSummary());
        exportData.put("criticalRecent", reportService.getRecentCritical());
        exportData.put("endpoints", reportService.getEndpointDistribution());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "anomaly-report.json");

        return new ResponseEntity<>(exportData, headers, HttpStatus.OK);
    }
}
