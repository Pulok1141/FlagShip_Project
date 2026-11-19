package io.flagship.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scheduled_changes")
public class ScheduledChange {

    public enum Status {
        SCHEDULED,
        EXECUTED,
        FAILED,
        CANCELLED
    }

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_env_config_id", nullable = false)
    private FlagEnvConfig flagEnvConfig;

    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.SCHEDULED;

    @Column(name = "proposed_config_json", nullable = false, columnDefinition = "TEXT")
    private String proposedConfigJson;

    @Column(name = "author_email", nullable = false)
    private String authorEmail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "executed_at")
    private Instant executedAt;

    public ScheduledChange() {}

    public ScheduledChange(String id, FlagEnvConfig flagEnvConfig, Instant scheduledTime,
                           String proposedConfigJson, String authorEmail, String reason) {
        this.id = id;
        this.flagEnvConfig = flagEnvConfig;
        this.scheduledTime = scheduledTime;
        this.proposedConfigJson = proposedConfigJson;
        this.authorEmail = authorEmail;
        this.reason = reason;
        this.status = Status.SCHEDULED;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public FlagEnvConfig getFlagEnvConfig() { return flagEnvConfig; }
    public void setFlagEnvConfig(FlagEnvConfig flagEnvConfig) { this.flagEnvConfig = flagEnvConfig; }

    public Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Instant scheduledTime) { this.scheduledTime = scheduledTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getProposedConfigJson() { return proposedConfigJson; }
    public void setProposedConfigJson(String proposedConfigJson) { this.proposedConfigJson = proposedConfigJson; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
}
