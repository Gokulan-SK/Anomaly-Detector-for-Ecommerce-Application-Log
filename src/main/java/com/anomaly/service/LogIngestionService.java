package com.anomaly.service;

import com.anomaly.model.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LogIngestionService is the entry point for raw log data entering the pipeline.
 */
@Service
public class LogIngestionService {

    private static final Logger log = LoggerFactory.getLogger(LogIngestionService.class);

    private static final String LOG_DIR = "../Ecommerce App/logs/";

    private final LogParserService logParserService;
    private final AnomalyDetectorService anomalyDetectorService;
    private final AnomalyService anomalyService;

    public LogIngestionService(LogParserService logParserService,
                               AnomalyDetectorService anomalyDetectorService,
                               AnomalyService anomalyService) {
        this.logParserService = logParserService;
        this.anomalyDetectorService = anomalyDetectorService;
        this.anomalyService = anomalyService;
    }

    public List<LogEvent> readAndParseLogs(String fileName) {
        Path filePath = resolveLogPath(fileName);
        log.info("Reading log file: {}", filePath.toAbsolutePath());

        if (!Files.exists(filePath)) {
            log.error("Log file not found: {}", filePath.toAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)) {
            List<LogEvent> events = lines
                    .map(logParserService::parse)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Finished reading [{}] — parsed {} valid events.", fileName, events.size());
            
            // Process each event for anomalies
            for (LogEvent event : events) {
                ingest(event);
            }
            
            return events;

        } catch (IOException e) {
            log.error("Failed to read log file [{}]: {}", fileName, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void ingest(String rawJson) {
        log.debug("LogIngestionService.ingest(String) called.");
        LogEvent event = logParserService.parse(rawJson);
        if (event != null) {
            ingest(event);
        }
    }

    public void ingest(LogEvent logEvent) {
        log.debug("LogIngestionService.ingest(LogEvent) called — event: {}", logEvent);
        anomalyDetectorService.detect(logEvent).ifPresent(anomalyService::save);
    }

    private Path resolveLogPath(String fileName) {
        return Paths.get(LOG_DIR + fileName).toAbsolutePath().normalize();
    }
}
