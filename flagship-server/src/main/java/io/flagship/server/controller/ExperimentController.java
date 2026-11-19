package io.flagship.server.controller;

import io.flagship.server.service.ExperimentAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/projects/{projectId}/experiments")
public class ExperimentController {

    private final ExperimentAnalyticsService analyticsService;

    public ExperimentController(ExperimentAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{flagKey}")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public ResponseEntity<ExperimentAnalyticsService.ExperimentReport> getReport(
            @PathVariable String projectId,
            @PathVariable String flagKey,
            @RequestParam(defaultValue = "production") String envKey,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
            @RequestParam(required = false) String baseline
    ) {
        var report = analyticsService.analyzeExperiment(projectId, envKey, flagKey, since, baseline);
        return ResponseEntity.ok(report);
    }
}
