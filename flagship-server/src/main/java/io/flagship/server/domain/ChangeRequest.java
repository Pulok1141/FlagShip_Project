package io.flagship.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "change_requests")
public class ChangeRequest {

    public enum Status {
        PENDING_APPROVAL,
        APPROVED,
        REJECTED,
        APPLIED,
        CANCELLED
    }

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_env_config_id", nullable = false)
    private FlagEnvConfig flagEnvConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.PENDING_APPROVAL;

    @Column(name = "proposed_config_json", nullable = false, columnDefinition = "TEXT")
    private String proposedConfigJson;

    @Column(name = "current_config_json", nullable = false, columnDefinition = "TEXT")
    private String currentConfigJson;

    @Column(name = "diff_json", nullable = false, columnDefinition = "TEXT")
    private String diffJson;

    @Column(name = "author_email", nullable = false)
    private String authorEmail;

    @Column(name = "approver_email")
    private String approverEmail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public ChangeRequest() {}

    public ChangeRequest(String id, Project project, Environment environment, FlagEnvConfig flagEnvConfig,
                         String proposedConfigJson, String currentConfigJson, String diffJson,
                         String authorEmail, String reason) {
        this.id = id;
        this.project = project;
        this.environment = environment;
        this.flagEnvConfig = flagEnvConfig;
        this.proposedConfigJson = proposedConfigJson;
        this.currentConfigJson = currentConfigJson;
        this.diffJson = diffJson;
        this.authorEmail = authorEmail;
        this.reason = reason;
        this.status = Status.PENDING_APPROVAL;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }

    public FlagEnvConfig getFlagEnvConfig() { return flagEnvConfig; }
    public void setFlagEnvConfig(FlagEnvConfig flagEnvConfig) { this.flagEnvConfig = flagEnvConfig; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getProposedConfigJson() { return proposedConfigJson; }
    public void setProposedConfigJson(String proposedConfigJson) { this.proposedConfigJson = proposedConfigJson; }

    public String getCurrentConfigJson() { return currentConfigJson; }
    public void setCurrentConfigJson(String currentConfigJson) { this.currentConfigJson = currentConfigJson; }

    public String getDiffJson() { return diffJson; }
    public void setDiffJson(String diffJson) { this.diffJson = diffJson; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getApproverEmail() { return approverEmail; }
    public void setApproverEmail(String approverEmail) { this.approverEmail = approverEmail; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
