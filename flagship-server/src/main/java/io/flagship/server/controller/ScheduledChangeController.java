package io.flagship.server.controller;

import io.flagship.server.domain.ScheduledChange;
import io.flagship.server.service.ScheduledChangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/flags/env-configs/{flagEnvConfigId}/scheduled")
public class ScheduledChangeController {

    private final ScheduledChangeService scheduledChangeService;

    public ScheduledChangeController(ScheduledChangeService scheduledChangeService) {
        this.scheduledChangeService = scheduledChangeService;
    }

    @GetMapping
    public List<ScheduledChange> getScheduledChanges(@PathVariable String flagEnvConfigId) {
        return scheduledChangeService.getScheduledChanges(flagEnvConfigId);
    }

    public record ScheduleRequest(
            Instant scheduledTime,
            String proposedConfigJson,
            String reason
    ) {}

    @PostMapping
    public ResponseEntity<ScheduledChange> scheduleChange(
            @PathVariable String flagEnvConfigId,
            @RequestBody ScheduleRequest req,
            Authentication auth
    ) {
        ScheduledChange scheduled = scheduledChangeService.scheduleChange(
                flagEnvConfigId,
                req.scheduledTime(),
                req.proposedConfigJson(),
                auth.getName(),
                req.reason()
        );
        return ResponseEntity.ok(scheduled);
    }
}
