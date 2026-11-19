package io.flagship.server.service;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.engine.DefaultEvaluationEngine;
import io.flagship.core.engine.EvaluationEngine;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;
import io.flagship.core.model.FlagDefinition;
import io.flagship.core.model.SnapshotJsonMapper;
import io.flagship.server.command.JsonDiffService;
import io.flagship.server.domain.*;
import io.flagship.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class FlagService {

    private final FlagRepository flagRepository;
    private final FlagEnvConfigRepository flagEnvConfigRepository;
    private final EnvironmentRepository environmentRepository;
    private final ProjectRepository projectRepository;
    private final FlagVersionRepository flagVersionRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final JsonDiffService diffService;
    private final SnapshotService snapshotService;
    private final SseBroadcaster sseBroadcaster;
    private final EvaluationEngine evaluationEngine = new DefaultEvaluationEngine();

    public FlagService(
            FlagRepository flagRepository,
            FlagEnvConfigRepository flagEnvConfigRepository,
            EnvironmentRepository environmentRepository,
            ProjectRepository projectRepository,
            FlagVersionRepository flagVersionRepository,
            ChangeRequestRepository changeRequestRepository,
            JsonDiffService diffService,
            SnapshotService snapshotService,
            SseBroadcaster sseBroadcaster
    ) {
        this.flagRepository = flagRepository;
        this.flagEnvConfigRepository = flagEnvConfigRepository;
        this.environmentRepository = environmentRepository;
        this.projectRepository = projectRepository;
        this.flagVersionRepository = flagVersionRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.diffService = diffService;
        this.snapshotService = snapshotService;
        this.sseBroadcaster = sseBroadcaster;
    }

    @Transactional
    public Flag createFlag(String projectId, Flag flag, String defaultVariationsJson, String defaultVariationId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (flagRepository.findByProjectIdAndFlagKey(projectId, flag.getFlagKey()).isPresent()) {
            throw new IllegalArgumentException("Flag key already exists: " + flag.getFlagKey());
        }

        flag.setId("flag-" + UUID.randomUUID());
        flag.setProject(project);
        flag.setCreatedAt(Instant.now());
        flag.setUpdatedAt(Instant.now());
        Flag savedFlag = flagRepository.save(flag);

        // Initialize FlagEnvConfig for each environment in the project
        List<Environment> environments = environmentRepository.findByProjectId(projectId);
        for (Environment env : environments) {
            FlagEnvConfig envConfig = new FlagEnvConfig(
                    "fec-" + UUID.randomUUID(),
                    savedFlag,
                    env,
                    false, // disabled by default
                    defaultVariationsJson != null ? defaultVariationsJson : "[]",
                    "[]",
                    "[]",
                    "{}",
                    defaultVariationId != null ? defaultVariationId : "default",
                    defaultVariationId
            );
            flagEnvConfigRepository.save(envConfig);

            // Record initial version
            recordVersion(envConfig, envConfig.getVariationsJson(), "{}", savedFlag.getOwnerEmail(), "Initial flag creation");
        }

        return savedFlag;
    }

    @Transactional
    public Map<String, Object> updateFlagConfig(
            String flagEnvConfigId,
            boolean enabled,
            String variationsJson,
            String targetsJson,
            String rulesJson,
            String rolloutJson,
            String defaultVariationId,
            String offVariationId,
            String authorEmail,
            String reason
    ) {
        FlagEnvConfig config = flagEnvConfigRepository.findById(flagEnvConfigId)
                .orElseThrow(() -> new IllegalArgumentException("FlagEnvConfig not found: " + flagEnvConfigId));

        Environment env = config.getEnvironment();
        Project project = env.getProject();

        Map<String, Object> proposedMap = new LinkedHashMap<>();
        proposedMap.put("enabled", enabled);
        proposedMap.put("variations", variationsJson);
        proposedMap.put("targets", targetsJson);
        proposedMap.put("rules", rulesJson);
        proposedMap.put("rollout", rolloutJson);
        proposedMap.put("defaultVariationId", defaultVariationId);
        proposedMap.put("offVariationId", offVariationId);
        String proposedJson = SnapshotJsonMapper.toJson(proposedMap);

        Map<String, Object> currentMap = new LinkedHashMap<>();
        currentMap.put("enabled", config.isEnabled());
        currentMap.put("variations", config.getVariationsJson());
        currentMap.put("targets", config.getTargetsJson());
        currentMap.put("rules", config.getRulesJson());
        currentMap.put("rollout", config.getRolloutJson());
        currentMap.put("defaultVariationId", config.getDefaultVariationId());
        currentMap.put("offVariationId", config.getOffVariationId());
        String currentJson = SnapshotJsonMapper.toJson(currentMap);

        String diffJson = diffService.computeDiff(currentJson, proposedJson);

        // If environment requires approval (e.g. Production), create a Change Request!
        if (env.isRequireApproval()) {
            ChangeRequest cr = new ChangeRequest(
                    "cr-" + UUID.randomUUID(),
                    project,
                    env,
                    config,
                    proposedJson,
                    currentJson,
                    diffJson,
                    authorEmail,
                    reason != null ? reason : "Configuration update"
            );
            changeRequestRepository.save(cr);
            return Map.of("status", "PENDING_APPROVAL", "changeRequestId", cr.getId(), "diff", diffJson);
        }

        // Direct apply for environments not requiring approval
        applyConfigDirectly(config, enabled, variationsJson, targetsJson, rulesJson, rolloutJson,
                defaultVariationId, offVariationId, proposedJson, diffJson, authorEmail, reason);

        return Map.of("status", "APPLIED", "version", config.getVersion(), "diff", diffJson);
    }

    @Transactional
    public void applyConfigDirectly(
            FlagEnvConfig config,
            boolean enabled,
            String variationsJson,
            String targetsJson,
            String rulesJson,
            String rolloutJson,
            String defaultVariationId,
            String offVariationId,
            String snapshotJson,
            String diffJson,
            String authorEmail,
            String reason
    ) {
        config.setEnabled(enabled);
        config.setVariationsJson(variationsJson);
        config.setTargetsJson(targetsJson);
        config.setRulesJson(rulesJson);
        config.setRolloutJson(rolloutJson);
        config.setDefaultVariationId(defaultVariationId);
        config.setOffVariationId(offVariationId);
        config.setVersion(config.getVersion() + 1);
        config.setUpdatedAt(Instant.now());
        flagEnvConfigRepository.save(config);

        recordVersion(config, snapshotJson, diffJson, authorEmail, reason != null ? reason : "Applied configuration update");

        // Broadcast to connected SSE clients
        FlagDefinition updatedFlag = snapshotService.mapToFlagDefinition(config);
        sseBroadcaster.broadcastFlagUpdate(
                config.getEnvironment().getProject().getId(),
                config.getEnvironment().getEnvKey(),
                config.getFlag().getFlagKey(),
                updatedFlag,
                config.getVersion()
        );
    }

    @Transactional
    public Map<String, Object> toggleFlag(String flagEnvConfigId, boolean enabled, String authorEmail, String reason) {
        FlagEnvConfig config = flagEnvConfigRepository.findById(flagEnvConfigId)
                .orElseThrow(() -> new IllegalArgumentException("FlagEnvConfig not found: " + flagEnvConfigId));

        return updateFlagConfig(
                flagEnvConfigId,
                enabled,
                config.getVariationsJson(),
                config.getTargetsJson(),
                config.getRulesJson(),
                config.getRolloutJson(),
                config.getDefaultVariationId(),
                config.getOffVariationId(),
                authorEmail,
                reason != null ? reason : (enabled ? "Enable feature flag" : "Trip kill switch / disable feature flag")
        );
    }

    @Transactional
    public Map<String, Object> restoreVersion(String flagEnvConfigId, long targetVersion, String authorEmail, String reason) {
        FlagVersion fv = flagVersionRepository.findByFlagEnvConfigIdAndVersion(flagEnvConfigId, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + targetVersion));

        FlagEnvConfig config = fv.getFlagEnvConfig();
        Map<?, ?> parsed = SnapshotJsonMapper.fromJson(fv.getSnapshotJson(), Map.class);

        boolean enabled = Boolean.TRUE.equals(parsed.get("enabled"));
        String variationsJson = (String) parsed.get("variations");
        String targetsJson = (String) parsed.get("targets");
        String rulesJson = (String) parsed.get("rules");
        String rolloutJson = (String) parsed.get("rollout");
        String defaultVariationId = (String) parsed.get("defaultVariationId");
        String offVariationId = (String) parsed.get("offVariationId");

        return updateFlagConfig(
                flagEnvConfigId,
                enabled,
                variationsJson,
                targetsJson,
                rulesJson,
                rolloutJson,
                defaultVariationId,
                offVariationId,
                authorEmail,
                reason != null ? reason : "Restored from version " + targetVersion
        );
    }

    @Transactional(readOnly = true)
    public EvaluationResult evaluatePreview(
            String projectId,
            String flagKey,
            String envKey,
            EvaluationContext context
    ) {
        var snapshot = snapshotService.buildSnapshot(projectId, envKey);
        var compiledSnapshot = new CompiledSnapshot(snapshot);
        return evaluationEngine.evaluate(flagKey, context, compiledSnapshot);
    }

    private void recordVersion(FlagEnvConfig config, String snapshotJson, String diffJson, String authorEmail, String reason) {
        FlagVersion version = new FlagVersion(
                "fv-" + UUID.randomUUID(),
                config,
                config.getVersion(),
                snapshotJson,
                diffJson,
                authorEmail != null ? authorEmail : "system@flagship.io",
                reason != null ? reason : "Updated"
        );
        flagVersionRepository.save(version);
    }
}
