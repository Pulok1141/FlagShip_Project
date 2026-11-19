package io.flagship.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_user", columnNames = {"project_id", "user_id"})
})
public class ProjectMembership {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProjectRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ProjectMembership() {}

    public ProjectMembership(String id, Project project, User user, ProjectRole role) {
        this.id = id;
        this.project = project;
        this.user = user;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ProjectRole getRole() { return role; }
    public void setRole(ProjectRole role) { this.role = role; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
