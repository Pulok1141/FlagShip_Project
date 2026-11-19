package io.flagship.server.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "project_key", nullable = false, unique = true, length = 64)
    private String projectKey;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Environment> environments = new ArrayList<>();

    public Project() {}

    public Project(String id, String projectKey, String name, String description) {
        this.id = id;
        this.projectKey = projectKey;
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<Environment> getEnvironments() { return environments; }
    public void setEnvironments(List<Environment> environments) { this.environments = environments; }
}
