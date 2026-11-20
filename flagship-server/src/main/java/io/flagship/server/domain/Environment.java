package io.flagship.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "environments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_env", columnNames = {"project_id", "env_key"})
})
public class Environment {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "env_key", nullable = false, length = 64)
    private String envKey; // development, staging, production

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "api_key", nullable = false, unique = true, length = 128)
    private String apiKey;

    @Column(name = "require_approval", nullable = false)
    private boolean requireApproval = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Environment() {}

    public Environment(String id, Project project, String envKey, String name, String apiKey, boolean requireApproval) {
        this.id = id;
        this.project = project;
        this.envKey = envKey;
        this.name = name;
        this.apiKey = apiKey;
        this.requireApproval = requireApproval;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = "env-" + java.util.UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "flagship_" + java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getEnvKey() { return envKey; }
    public void setEnvKey(String envKey) { this.envKey = envKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public boolean isRequireApproval() { return requireApproval; }
    public void setRequireApproval(boolean requireApproval) { this.requireApproval = requireApproval; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
