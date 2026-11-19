package io.flagship.server.controller;

import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;
import io.flagship.server.domain.Environment;
import io.flagship.server.domain.Flag;
import io.flagship.server.domain.FlagEnvConfig;
import io.flagship.server.domain.FlagVersion;
import io.flagship.server.repository.EnvironmentRepository;
import io.flagship.server.repository.FlagEnvConfigRepository;
import io.flagship.server.repository.FlagRepository;
import io.flagship.server.repository.FlagVersionRepository;
import io.flagship.server.service.FlagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/flags")
public class FlagController {

    private final FlagRepository flagRepository;
    private final FlagEnvConfigRepository flagEnvConfigRepository;
    private final EnvironmentRepository environmentRepository;
    private final FlagVersionRepository flagVersionRepository;
    private final FlagService flagService;

    public FlagController(
            FlagRepository flagRepository,
            FlagEnvConfigRepository flagEnvConfigRepository,
            EnvironmentRepository environmentRepository,
            FlagVersionRepository flagVersionRepository,
            FlagService flagService
    ) {
        this.flagRepository = flagRepository;
        this.flagEnvConfigRepository = flagEnvConfigRepository;
        this.environmentRepository = environmentRepository;
        this.flagVersionRepository = flagVersionRepository;
        this.flagService = flagService;
    }

    @GetMapping
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public List<Flag> listFlags(@PathVariable String projectId) {
        return flagRepository.findByProjectId(projectId);
    }

    public record CreateFlagRequest(
            Flag flag,
            String defaultVariationsJson,
            String defaultVariationId
    ) {}

    @PostMapping
    @PreAuthorize("@projectSec.hasRole(#projectId, 'EDITOR')")
    public ResponseEntity<Flag> createFlag(
            @PathVariable String projectId,
            @RequestBody CreateFlagRequest req,
            Authentication auth
    ) {
        Flag flag = req.flag();
        if (flag.getOwnerEmail() == null || flag.getOwnerEmail().isBlank()) {
            flag.setOwnerEmail(auth.getName());
        }
        Flag created = flagService.createFlag(projectId, flag, req.defaultVariationsJson(), req.defaultVariationId());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{flagKey}")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public ResponseEntity<Flag> getFlag(@PathVariable String projectId, @PathVariable String flagKey) {
        return flagRepository.findByProjectIdAndFlagKey(projectId, flagKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record UpdateConfigRequest(
            boolean enabled,
            String variationsJson,
            String targetsJson,
            String rulesJson,
            String rolloutJson,
            String defaultVariationId,
            String offVariationId,
            String reason
    ) {}

    @PutMapping("/{flagKey}/environments/{envKey}")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'EDITOR')")
    public ResponseEntity<?> updateEnvConfig(
            @PathVariable String projectId,
            @PathVariable String flagKey,
            @PathVariable String envKey,
            @RequestBody UpdateConfigRequest req,
            Authentication auth
    ) {
        Flag flag = flagRepository.findByProjectIdAndFlagKey(projectId, flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagKey));
        Environment env = environmentRepository.findByProjectIdAndEnvKey(projectId, envKey)
                .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + envKey));
        FlagEnvConfig config = flagEnvConfigRepository.findByFlagIdAndEnvironmentId(flag.getId(), env.getId())
                .orElseThrow(() -> new IllegalArgumentException("Config not found for flag in env: " + envKey));

        Map<String, Object> result = flagService.updateFlagConfig(
                config.getId(),
                req.enabled(),
                req.variationsJson(),
                req.targetsJson(),
                req.rulesJson(),
                req.rolloutJson(),
                req.defaultVariationId(),
                req.offVariationId(),
                auth.getName(),
                req.reason()
        );
        return ResponseEntity.ok(result);
    }

    public record ToggleRequest(boolean enabled, String reason) {}

    @PostMapping("/{flagKey}/environments/{envKey}/toggle")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'EDITOR')")
    public ResponseEntity<?> toggleFlag(
            @PathVariable String projectId,
            @PathVariable String flagKey,
            @PathVariable String envKey,
            @RequestBody ToggleRequest req,
            Authentication auth
    ) {
        Flag flag = flagRepository.findByProjectIdAndFlagKey(projectId, flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagKey));
        Environment env = environmentRepository.findByProjectIdAndEnvKey(projectId, envKey)
                .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + envKey));
        FlagEnvConfig config = flagEnvConfigRepository.findByFlagIdAndEnvironmentId(flag.getId(), env.getId())
                .orElseThrow(() -> new IllegalArgumentException("Config not found for flag in env: " + envKey));

        Map<String, Object> result = flagService.toggleFlag(config.getId(), req.enabled(), auth.getName(), req.reason());
        return ResponseEntity.ok(result);
    }

    public record PreviewRequest(
            String userKey,
            Map<String, Object> attributes
    ) {}

    @PostMapping("/{flagKey}/environments/{envKey}/evaluate-preview")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public ResponseEntity<EvaluationResult> evaluatePreview(
            @PathVariable String projectId,
            @PathVariable String flagKey,
            @PathVariable String envKey,
            @RequestBody PreviewRequest req
    ) {
        EvaluationContext context = EvaluationContext.builder()
                .userKey(req.userKey())
                .attributes(req.attributes())
                .build();

        EvaluationResult result = flagService.evaluatePreview(projectId, flagKey, envKey, context);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{flagKey}/environments/{envKey}/versions")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public ResponseEntity<List<FlagVersion>> getVersions(
            @PathVariable String projectId,
            @PathVariable String flagKey,
            @PathVariable String envKey
    ) {
        Flag flag = flagRepository.findByProjectIdAndFlagKey(projectId, flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagKey));
        Environment env = environmentRepository.findByProjectIdAndEnvKey(projectId, envKey)
                .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + envKey));
        FlagEnvConfig config = flagEnvConfigRepository.findByFlagIdAndEnvironmentId(flag.getId(), env.getId())
                .orElseThrow(() -> new IllegalArgumentException("Config not found for flag in env: " + envKey));

        return ResponseEntity.ok(flagVersionRepository.findByFlagEnvConfigIdOrderByVersionDesc(config.getId()));
    }

    public record RestoreRequest(String reason) {}

    @PostMapping("/{flagKey}/environments/{envKey}/restore/{version}")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'EDITOR')")
    public ResponseEntity<?> restoreVersion(
            @PathVariable String projectId,
            @PathVariable String flagKey,
            @PathVariable String envKey,
            @PathVariable long version,
            @RequestBody(required = false) RestoreRequest req,
            Authentication auth
    ) {
        Flag flag = flagRepository.findByProjectIdAndFlagKey(projectId, flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagKey));
        Environment env = environmentRepository.findByProjectIdAndEnvKey(projectId, envKey)
                .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + envKey));
        FlagEnvConfig config = flagEnvConfigRepository.findByFlagIdAndEnvironmentId(flag.getId(), env.getId())
                .orElseThrow(() -> new IllegalArgumentException("Config not found for flag in env: " + envKey));

        String reason = (req != null && req.reason() != null) ? req.reason() : "Restored from version " + version;
        Map<String, Object> result = flagService.restoreVersion(config.getId(), version, auth.getName(), reason);
        return ResponseEntity.ok(result);
    }
}
