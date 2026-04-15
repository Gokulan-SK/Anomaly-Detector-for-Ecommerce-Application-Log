package com.anomaly.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

/**
 * AnomalyEvent is the core JPA entity representing a detected anomaly.
 * It is persisted to the H2 database via Spring Data JPA.
 *
 * Fields are intentionally left minimal — no detection logic is implemented yet.
 */
@Entity
@Table(name = "anomaly_events")
public class AnomalyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The type/category of the anomaly.
     * e.g. "REPEATED_AUTH_FAILURE", "SLOW_QUERY", "UNEXPECTED_ERROR_SPIKE"
     */
    @Column(name = "anomaly_type", nullable = false)
    private String anomalyType;

    /**
     * The application layer where the anomaly originated.
     * e.g. "UI", "API", "DB", "AUTH"
     */
    @Column(name = "source_layer", nullable = false)
    private String sourceLayer;

    /**
     * Trace/correlation ID linking this anomaly to a specific request chain.
     */
    @Column(name = "correlation_id")
    private String correlationId;

    /**
     * Session ID of the user session during which the anomaly occurred.
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * Severity level of the anomaly.
     * e.g. "LOW", "MEDIUM", "HIGH", "CRITICAL"
     */
    @Column(name = "severity", nullable = false)
    private String severity;

    /**
     * Severity score (0-100)
     */
    @Column(name = "score")
    private Integer score;

    /**
     * Human-readable description of the anomaly.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Timestamp when this anomaly record was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Username associated with the session/request that triggered this anomaly.
     * Populated from the log's sessionId context when available.
     * Defaults to "system" when no user context exists.
     * No foreign key — decoupled from the users table intentionally.
     */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * Embedding vector representing the anomaly context.
     * Stored as a JSON array in the database.
     */
    @Convert(converter = DoubleListConverter.class)
    @Column(name = "embedding", columnDefinition = "JSON")
    private List<Double> embedding;

    // -------------------------
    // Lifecycle Hooks
    // -------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // -------------------------
    // Constructors
    // -------------------------

    public AnomalyEvent() {
    }

    public AnomalyEvent(String anomalyType, String sourceLayer, String correlationId,
                        String sessionId, String severity, String description) {
        this.anomalyType = anomalyType;
        this.sourceLayer = sourceLayer;
        this.correlationId = correlationId;
        this.sessionId = sessionId;
        this.severity = severity;
        this.description = description;
    }

    // -------------------------
    // Getters & Setters
    // -------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public String getSourceLayer() { return sourceLayer; }
    public void setSourceLayer(String sourceLayer) { this.sourceLayer = sourceLayer; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }

    @Override
    public String toString() {
        return "AnomalyEvent{" +
                "id=" + id +
                ", anomalyType='" + anomalyType + '\'' +
                ", sourceLayer='" + sourceLayer + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", severity='" + severity + '\'' +
                ", username='" + username + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
