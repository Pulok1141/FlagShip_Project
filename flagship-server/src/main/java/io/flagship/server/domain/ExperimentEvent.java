package io.flagship.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "experiment_events")
public class ExperimentEvent {

    public enum EventType {
        EXPOSURE,
        GOAL
    }

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "project_id", nullable = false, length = 64)
    private String projectId;

    @Column(name = "environment_key", nullable = false, length = 64)
    private String environmentKey;

    @Column(name = "flag_key", nullable = false, length = 128)
    private String flagKey;

    @Column(name = "variation_id", nullable = false, length = 64)
    private String variationId;

    @Column(name = "user_key", nullable = false)
    private String userKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @Column(name = "metric_name", length = 128)
    private String metricName;

    @Column(name = "metric_value")
    private Double metricValue = 1.0;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public ExperimentEvent() {}

    public ExperimentEvent(String id, String projectId, String environmentKey, String flagKey,
                           String variationId, String userKey, EventType eventType,
                           String metricName, Double metricValue, Instant timestamp) {
        this.id = id;
        this.projectId = projectId;
        this.environmentKey = environmentKey;
        this.flagKey = flagKey;
        this.variationId = variationId;
        this.userKey = userKey;
        this.eventType = eventType;
        this.metricName = metricName;
        this.metricValue = metricValue != null ? metricValue : 1.0;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = "evt-" + java.util.UUID.randomUUID();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (metricValue == null) {
            metricValue = 1.0;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getEnvironmentKey() { return environmentKey; }
    public void setEnvironmentKey(String environmentKey) { this.environmentKey = environmentKey; }

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }

    public String getVariationId() { return variationId; }
    public void setVariationId(String variationId) { this.variationId = variationId; }

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public Double getMetricValue() { return metricValue; }
    public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
