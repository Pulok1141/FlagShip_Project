package io.flagship.server.service;

import io.flagship.server.domain.Flag;
import io.flagship.server.repository.FlagRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Flag Hygiene Engine detecting stale feature flags (FR9).
 * Identifies flags unused >= 30 days OR fully rolled out >= 14 days.
 */
@Service
public class FlagHygieneService {

    private final FlagRepository flagRepository;
    private final int staleDaysUnused;
    private final int staleDaysFullyRolledOut;

    public FlagHygieneService(
            FlagRepository flagRepository,
            @Value("${flagship.hygiene.stale-days-unused:30}") int staleDaysUnused,
            @Value("${flagship.hygiene.stale-days-fully-rolled-out:14}") int staleDaysFullyRolledOut
    ) {
        this.flagRepository = flagRepository;
        this.staleDaysUnused = staleDaysUnused;
        this.staleDaysFullyRolledOut = staleDaysFullyRolledOut;
    }

    public record StaleFlagReport(
            String flagKey,
            String name,
            String ownerEmail,
            Instant createdAt,
            Instant lastEvaluatedAt,
            Instant fullyRolledOutSince,
            String reason,
            String recommendation
    ) {}

    @Transactional(readOnly = true)
    public List<StaleFlagReport> getStaleFlags(String projectId) {
        Instant now = Instant.now();
        Instant unusedCutoff = now.minus(staleDaysUnused, ChronoUnit.DAYS);
        Instant rolloutCutoff = now.minus(staleDaysFullyRolledOut, ChronoUnit.DAYS);

        List<Flag> flags = flagRepository.findByProjectId(projectId);
        List<StaleFlagReport> staleReports = new ArrayList<>();

        for (Flag flag : flags) {
            boolean isUnused = (flag.getLastEvaluatedAt() == null && flag.getCreatedAt().isBefore(unusedCutoff))
                    || (flag.getLastEvaluatedAt() != null && flag.getLastEvaluatedAt().isBefore(unusedCutoff));

            boolean isPermanentlyRolledOut = flag.getFullyRolledOutSince() != null
                    && flag.getFullyRolledOutSince().isBefore(rolloutCutoff);

            if (isUnused) {
                staleReports.add(new StaleFlagReport(
                        flag.getFlagKey(),
                        flag.getName(),
                        flag.getOwnerEmail(),
                        flag.getCreatedAt(),
                        flag.getLastEvaluatedAt(),
                        flag.getFullyRolledOutSince(),
                        "No evaluation events recorded in past " + staleDaysUnused + " days",
                        "Archive or safely remove flag from codebase"
                ));
            } else if (isPermanentlyRolledOut) {
                staleReports.add(new StaleFlagReport(
                        flag.getFlagKey(),
                        flag.getName(),
                        flag.getOwnerEmail(),
                        flag.getCreatedAt(),
                        flag.getLastEvaluatedAt(),
                        flag.getFullyRolledOutSince(),
                        "100% fully rolled out for more than " + staleDaysFullyRolledOut + " days",
                        "Clean up flag checks and make variation permanent"
                ));
            }
        }

        return staleReports;
    }
}
