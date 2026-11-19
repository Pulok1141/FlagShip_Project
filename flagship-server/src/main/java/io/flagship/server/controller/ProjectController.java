package io.flagship.server.controller;

import io.flagship.server.domain.Environment;
import io.flagship.server.domain.Project;
import io.flagship.server.domain.ProjectMembership;
import io.flagship.server.domain.ProjectRole;
import io.flagship.server.domain.User;
import io.flagship.server.repository.EnvironmentRepository;
import io.flagship.server.repository.ProjectMembershipRepository;
import io.flagship.server.repository.ProjectRepository;
import io.flagship.server.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public ProjectController(
            ProjectRepository projectRepository,
            EnvironmentRepository environmentRepository,
            ProjectMembershipRepository membershipRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Project> listProjects() {
        return projectRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        if (project.getId() == null) {
            project.setId("proj-" + UUID.randomUUID());
        }
        Project saved = projectRepository.save(project);

        // Auto-create standard environments: development, staging, production
        Environment dev = new Environment("env-" + UUID.randomUUID(), saved, "development", "Development", "flagship_dev_" + UUID.randomUUID(), false);
        Environment stage = new Environment("env-" + UUID.randomUUID(), saved, "staging", "Staging", "flagship_staging_" + UUID.randomUUID(), false);
        Environment prod = new Environment("env-" + UUID.randomUUID(), saved, "production", "Production", "flagship_prod_" + UUID.randomUUID(), true);

        environmentRepository.saveAll(List.of(dev, stage, prod));
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Project> getProject(@PathVariable String projectId) {
        return projectRepository.findById(projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{projectId}/environments")
    public List<Environment> getEnvironments(@PathVariable String projectId) {
        return environmentRepository.findByProjectId(projectId);
    }

    @GetMapping("/{projectId}/members")
    public List<ProjectMembership> getMembers(@PathVariable String projectId) {
        return membershipRepository.findByProjectId(projectId);
    }

    public record AddMemberRequest(String userEmail, ProjectRole role) {}

    @PostMapping("/{projectId}/members")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'ADMIN')")
    public ResponseEntity<?> addMember(@PathVariable String projectId, @RequestBody AddMemberRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        User user = userRepository.findByEmail(req.userEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.userEmail()));

        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, user.getId())
                .orElse(new ProjectMembership("pm-" + UUID.randomUUID(), project, user, req.role()));

        membership.setRole(req.role());
        return ResponseEntity.ok(membershipRepository.save(membership));
    }
}
