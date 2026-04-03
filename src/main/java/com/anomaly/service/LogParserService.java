package com.anomaly.service;

import com.anomaly.model.LogEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogParserService {

    private static final Logger log = LoggerFactory.getLogger(LogParserService.class);

    private final ObjectMapper objectMapper;

    public LogParserService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public LogEvent parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            LogEvent event = objectMapper.treeToValue(root, LogEvent.class);
            
            if (event == null) {
                return null;
            }

            String eventType = event.getEventType();

            if ("BUSINESS_EVENT".equals(eventType)) {
                String className = root.path("className").asText("");
                String methodName = root.path("methodName").asText("");
                event.setEndpoint(className + "." + methodName);
                event.setHttpStatus(200);
                event.setLatencyMs(root.path("executionTimeMs").asLong(0));
                
                if (event.getSessionId() == null) {
                    event.setSessionId(event.getCorrelationId());
                }
            } else if ("SYSTEM_EVENT".equals(eventType)) {
                if (event.getHttpStatus() == null) {
                    event.setHttpStatus(500);
                }
                event.setLatencyMs(0L);
                event.setSessionId(event.getCorrelationId());
            }

            if (event.getEndpoint() == null) {
                event.setEndpoint("UNKNOWN");
            }
            if (event.getSessionId() == null) {
                event.setSessionId(event.getCorrelationId() != null ? event.getCorrelationId() : "UNKNOWN_SESSION");
            }
            if (event.getLatencyMs() == null) {
                event.setLatencyMs(0L);
            }

            log.debug("Parsed LogEvent: eventType={}, level={}, endpoint={}",
                    event.getEventType(), event.getLevel(), event.getEndpoint());
            return event;
            
        } catch (Exception e) {
            log.warn("Skipping malformed log line — parse error: {}. Line preview: [{}]",
                    e.getMessage(),
                    rawJson.length() > 120 ? rawJson.substring(0, 120) + "..." : rawJson);
            return null;
        }
    }
}
