package io.flagship.server.controller;

import io.flagship.server.domain.Project;
import io.flagship.server.domain.Segment;
import io.flagship.server.repository.ProjectRepository;
import io.flagship.server.repository.SegmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/segments")
public class SegmentController {

    private final SegmentRepository segmentRepository;
    private final ProjectRepository projectRepository;

    public SegmentController(SegmentRepository segmentRepository, ProjectRepository projectRepository) {
        this.segmentRepository = segmentRepository;
        this.projectRepository = projectRepository;
    }

    @GetMapping
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public List<Segment> listSegments(@PathVariable String projectId) {
        return segmentRepository.findByProjectId(projectId);
    }

    @PostMapping
    @PreAuthorize("@projectSec.hasRole(#projectId, 'EDITOR')")
    public ResponseEntity<Segment> createSegment(@PathVariable String projectId, @RequestBody Segment segment) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        segment.setId("seg-" + UUID.randomUUID());
        segment.setProject(project);
        segment.setCreatedAt(Instant.now());
        segment.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(segmentRepository.save(segment));
    }

    @GetMapping("/{segmentKey}")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public ResponseEntity<Segment> getSegment(@PathVariable String projectId, @PathVariable String segmentKey) {
        return segmentRepository.findByProjectIdAndSegmentKey(projectId, segmentKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{segmentKey}")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'EDITOR')")
    public ResponseEntity<Segment> updateSegment(
            @PathVariable String projectId,
            @PathVariable String segmentKey,
            @RequestBody Segment update
    ) {
        Segment segment = segmentRepository.findByProjectIdAndSegmentKey(projectId, segmentKey)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + segmentKey));

        segment.setName(update.getName());
        segment.setDescription(update.getDescription());
        segment.setConditionJson(update.getConditionJson());
        segment.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(segmentRepository.save(segment));
    }

    @DeleteMapping("/{segmentKey}")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'EDITOR')")
    public ResponseEntity<Void> deleteSegment(@PathVariable String projectId, @PathVariable String segmentKey) {
        Segment segment = segmentRepository.findByProjectIdAndSegmentKey(projectId, segmentKey)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + segmentKey));
        segmentRepository.delete(segment);
        return ResponseEntity.noContent().build();
    }
}
