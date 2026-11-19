package io.flagship.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.flagship.core.model.SnapshotJsonMapper;
import io.flagship.server.domain.FlagEnvConfig;
import io.flagship.server.domain.ScheduledChange;
import io.flagship.server.repository.FlagEnvConfigRepository;
import io.flagship.server.repository.ScheduledChangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScheduledChangeService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledChangeService.class);

    private final ScheduledChangeRepository scheduledChangeRepository;
    private final FlagEnvConfigRepository flagEnvConfigRepository;
    private final FlagService flagService;

    public ScheduledChangeService(
            ScheduledChangeRepository scheduledChangeRepository,
            FlagEnvConfigRepository flagEnvConfigRepository,
            FlagService flagService
    ) {
        this.scheduledChangeRepository = scheduledChangeRepository;
        this.flagEnvConfigRepository = flagEnvConfigRepository;
        this.flagService = flagService;
    }

    public ScheduledChange scheduleChange(
            String flagEnvConfigId,
            Instant scheduledTime,
            String proposedConfigJson,
            String authorEmail,
            String reason
    ) {
        FlagEnvConfig config = flagEnvConfigRepository.findById(flagEnvConfigId)
                .orElseThrow(() -> new IllegalArgumentException("FlagEnvConfig not found: " + flagEnvConfigId));

        ScheduledChange change = new ScheduledChange(
                "sc-" + UUID.randomUUID(),
                config,
                scheduledTime,
                proposedConfigJson,
                authorEmail,
                reason
        );
        return scheduledChangeRepository.save(change);
    }

    public List<ScheduledChange> getScheduledChanges(String flagEnvConfigId) {
        return scheduledChangeRepository.findByFlagEnvConfigId(flagEnvConfigId);
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void executeDueScheduledChanges() {
        Instant now = Instant.now();
        List<ScheduledChange> dueChanges = scheduledChangeRepository.findByStatusAndScheduledTimeLessThanEqual(
                ScheduledChange.Status.SCHEDULED, now
        );

        for (ScheduledChange change : dueChanges) {
            try {
                FlagEnvConfig config = change.getFlagEnvConfig();
                Map<String, Object> proposed = SnapshotJsonMapper.fromJson(
                        change.getProposedConfigJson(),
                        new TypeReference<Map<String, Object>>() {}
                );

                boolean enabled = Boolean.TRUE.equals(proposed.get("enabled"));
                String variationsJson = (String) proposed.get("variations");
                String targetsJson = (String) proposed.get("targets");
                String rulesJson = (String) proposed.get("rules");
                String rolloutJson = (String) proposed.get("rollout");
                String defaultVariationId = (String) proposed.get("defaultVariationId");
                String offVariationId = (String) proposed.get("offVariationId");

                flagService.applyConfigDirectly(
                        config,
                        enabled,
                        variationsJson,
                        targetsJson,
                        rulesJson,
                        rolloutJson,
                        defaultVariationId,
                        offVariationId,
                        change.getProposedConfigJson(),
                        "{}",
                        change.getAuthorEmail(),
                        "Scheduled execution: " + change.getReason()
                );

                change.setStatus(ScheduledChange.Status.EXECUTED);
                change.setExecutedAt(Instant.now());
                scheduledChangeRepository.save(change);
                log.info("Successfully executed scheduled change {}", change.getId());
            } catch (Exception e) {
                log.error("Failed to execute scheduled change {}: {}", change.getId(), e.getMessage(), e);
                change.setStatus(ScheduledChange.Status.FAILED);
                scheduledChangeRepository.save(change);
            }
        }
    }
}
