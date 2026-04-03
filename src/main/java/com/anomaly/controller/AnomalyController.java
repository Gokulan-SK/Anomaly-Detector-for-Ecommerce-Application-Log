package com.anomaly.controller;

import com.anomaly.model.AnomalyEvent;
import com.anomaly.model.LogEvent;
import com.anomaly.service.AnomalyService;
import com.anomaly.service.LogIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

/**
 * AnomalyController exposes the REST API for the anomaly detection service.
 *
 * Base path: /anomalies
 *
 * Endpoints:
 *  - POST /anomalies/ingest    — Ingest a single raw log event JSON payload
 *  - GET  /anomalies/test-logs — Read & parse request-events.json, return count
 *  - GET  /anomalies           — Retrieve all detected anomalies from DB
 *  - GET  /anomalies/{id}      — Retrieve a single anomaly by ID
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/anomalies")
public class AnomalyController {

    private static final Logger log = LoggerFactory.getLogger(AnomalyController.class);

    private final LogIngestionService logIngestionService;
    private final AnomalyService anomalyService;

    public AnomalyController(LogIngestionService logIngestionService,
                              AnomalyService anomalyService) {
        this.logIngestionService = logIngestionService;
        this.anomalyService = anomalyService;
    }

    // --------------------------------------------------
    // Log Ingestion
    // --------------------------------------------------

    /**
     * Accepts a single JSON log event and routes it through the detection pipeline.
     *
     * POST /anomalies/ingest
     */
    @PostMapping("/ingest-log")
    public ResponseEntity<String> ingestLog(@RequestBody LogEvent logEvent) {
        log.info("INGEST HIT");
        log.info("Parsed LogEvent: {}", logEvent);
        log.info("Before detection eventType: {}", logEvent != null ? logEvent.getEventType() : "null");

        logIngestionService.ingest(logEvent);
        return ResponseEntity.accepted().body("Log event accepted for processing.");
    }

    // --------------------------------------------------
    // Test / Diagnostic Endpoints
    // --------------------------------------------------

    /**
     * Reads request-events.json, parses all lines, and returns the count.
     * Used to verify that the file ingestion pipeline is correctly wired end-to-end.
     *
     * GET /anomalies/test-logs
     */
    @GetMapping("/test-logs")
    public ResponseEntity<String> testLogIngestion() {
        log.info("GET /anomalies/test-logs — triggering log file ingestion test.");

        List<LogEvent> events = logIngestionService.readAndParseLogs("request-events.json");

        String message = String.format("Parsed %d log events from request-events.json", events.size());
        log.info(message);
        return ResponseEntity.ok(message);
    }

    // --------------------------------------------------
    // Anomaly Queries
    // --------------------------------------------------

    /**
     * Returns all anomaly events stored in the database.
     *
     * GET /anomalies
     */
    @GetMapping
    public ResponseEntity<List<AnomalyEvent>> getAllAnomalies() {
        log.info("GET /anomalies — fetching all anomalies.");
        List<AnomalyEvent> anomalies = anomalyService.findAll();
        return ResponseEntity.ok(anomalies);
    }

    /**
     * Returns a single anomaly event by ID.
     *
     * GET /anomalies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnomalyEvent> getAnomalyById(@PathVariable Long id) {
        log.info("GET /anomalies/{} — fetching anomaly by id.", id);
        return anomalyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
