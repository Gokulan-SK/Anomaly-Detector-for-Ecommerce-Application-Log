package com.anomaly.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * LogEvent represents a raw log entry ingested from an external source.
 * This is a plain Java object (NOT a JPA entity) — it is never persisted directly.
 *
 * Fields are mapped to the exact JSON keys produced by the e-commerce log generator.
 * Fields that may not appear in every log file are declared Optional-friendly (nullable).
 *
 * JSON key quirks handled via @JsonProperty:
 *  - "@timestamp"  → timestamp
 *  - "log.level"   → level
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) ensures forward compatibility
 * if new fields are added to the e-commerce log format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEvent {

    /** ISO-8601 timestamp from the log line, e.g. "2026-04-01T18:02:02.875+05:30" */
    @JsonProperty("@timestamp")
    private OffsetDateTime timestamp;

    /** Log severity level, e.g. "INFO", "WARN", "ERROR" */
    @JsonProperty("log.level")
    private String level;

    /** Logger name, e.g. "REQUEST_LOGGER", "SYSTEM_LOGGER", "BUSINESS_LOGGER" */
    @JsonProperty("logger_name")
    private String loggerName;

    /** Category of event: "REQUEST", "SYSTEM_EVENT", "BUSINESS_EVENT" */
    private String eventType;

    /** REST endpoint that was called, e.g. "/products", "/orders/checkout" */
    private String endpoint;

    /** HTTP response status code, e.g. 200, 404, 500 */
    private Integer httpStatus;

    /** HTTP method: "GET", "POST", "PUT", "DELETE" */
    private String httpMethod;

    /** End-to-end request latency in milliseconds (request events) */
    private Long latencyMs;

    /** Distributed trace / correlation ID linking a full request chain */
    private String correlationId;

    /** User session ID (present when a session exists) */
    private String sessionId;

    /** Fully-qualified exception class name (system/error events) */
    private String exceptionClass;

    /** Human-readable exception or log message */
    private String message;

    /** Whether the log generator flagged this entry as anomalous */
    private Boolean anomalyFlag;

    /** Originating application name */
    private String application;

    /** Deployment environment, e.g. "development", "production" */
    private String environment;

    // -------------------------
    // Constructors
    // -------------------------

    public LogEvent() {
    }

    // -------------------------
    // Getters & Setters
    // -------------------------

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getLoggerName() { return loggerName; }
    public void setLoggerName(String loggerName) { this.loggerName = loggerName; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getExceptionClass() { return exceptionClass; }
    public void setExceptionClass(String exceptionClass) { this.exceptionClass = exceptionClass; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getAnomalyFlag() { return anomalyFlag; }
    public void setAnomalyFlag(Boolean anomalyFlag) { this.anomalyFlag = anomalyFlag; }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    @Override
    public String toString() {
        return "LogEvent{" +
                "timestamp=" + timestamp +
                ", level='" + level + '\'' +
                ", eventType='" + eventType + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", httpStatus=" + httpStatus +
                ", latencyMs=" + latencyMs +
                ", correlationId='" + correlationId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", exceptionClass='" + exceptionClass + '\'' +
                ", anomalyFlag=" + anomalyFlag +
                '}';
    }
}
