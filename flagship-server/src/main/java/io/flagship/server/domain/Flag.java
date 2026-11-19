package io.flagship.server.domain;

import io.flagship.core.model.FlagType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flags", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_flag", columnNames = {"project_id", "flag_key"})
})
public class Flag {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "flag_key", nullable = false, length = 128)
    private String flagKey;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 32)
    private FlagType flagType;

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "last_evaluated_at")
    private Instant lastEvaluatedAt;

    @Column(name = "fully_rolled_out_since")
    private Instant fullyRolledOutSince;

    @OneToMany(mappedBy = "flag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlagEnvConfig> envConfigs = new ArrayList<>();

    public Flag() {}

    public Flag(String id, Project project, String flagKey, String name, String description,
                FlagType flagType, String ownerEmail, String tags) {
        this.id = id;
        this.project = project;
        this.flagKey = flagKey;
        this.name = name;
        this.description = description;
        this.flagType = flagType;
        this.ownerEmail = ownerEmail;
        this.tags = tags;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public FlagType getFlagType() { return flagType; }
    public void setFlagType(FlagType flagType) { this.flagType = flagType; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(Instant lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }

    public Instant getFullyRolledOutSince() { return fullyRolledOutSince; }
    public void setFullyRolledOutSince(Instant fullyRolledOutSince) { this.fullyRolledOutSince = fullyRolledOutSince; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<FlagEnvConfig> getEnvConfigs() { return envConfigs; }
    public void setEnvConfigs(List<FlagEnvConfig> envConfigs) { this.envConfigs = envConfigs; }
}
