package io.flagship.server.controller;

import io.flagship.core.model.EnvironmentSnapshot;
import io.flagship.server.domain.Environment;
import io.flagship.server.domain.ExperimentEvent;
import io.flagship.server.service.ExperimentAnalyticsService;
import io.flagship.server.service.SnapshotService;
import io.flagship.server.service.SseBroadcaster;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sdk/v1")
public class SdkController {

    private final SnapshotService snapshotService;
    private final SseBroadcaster sseBroadcaster;
    private final ExperimentAnalyticsService analyticsService;

    public SdkController(
            SnapshotService snapshotService,
            SseBroadcaster sseBroadcaster,
            ExperimentAnalyticsService analyticsService
    ) {
        this.snapshotService = snapshotService;
        this.sseBroadcaster = sseBroadcaster;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<EnvironmentSnapshot> getSnapshot(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            Authentication auth
    ) {
        Environment env = (Environment) auth.getDetails();
        EnvironmentSnapshot snapshot = snapshotService.buildSnapshot(env.getProject().getId(), env.getEnvKey());
        String etag = snapshotService.computeETag(snapshot);

        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, "private, must-revalidate")
                .body(snapshot);
    }

    @GetMapping({"/sse", "/stream"})
    public SseEmitter streamUpdates(Authentication auth, HttpServletResponse response) {
        Environment env = (Environment) auth.getDetails();

        // Prevent proxy buffering and caching
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        return sseBroadcaster.subscribe(env.getProject().getId(), env.getEnvKey());
    }

    public record BatchEventsRequest(List<ExperimentEvent> events) {}

    @PostMapping("/events")
    public ResponseEntity<?> ingestEvents(@RequestBody BatchEventsRequest req, Authentication auth) {
        Environment env = (Environment) auth.getDetails();
        if (req.events() != null) {
            for (ExperimentEvent event : req.events()) {
                event.setProjectId(env.getProject().getId());
                event.setEnvironmentKey(env.getEnvKey());
            }
            analyticsService.recordEventsBatch(req.events());
        }
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "ingestedCount", req.events() != null ? req.events().size() : 0));
    }
}
