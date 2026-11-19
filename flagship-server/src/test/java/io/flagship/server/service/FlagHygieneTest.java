package io.flagship.server.service;

import io.flagship.core.model.FlagType;
import io.flagship.server.domain.Flag;
import io.flagship.server.domain.Project;
import io.flagship.server.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlagHygieneTest {

    @Test
    @DisplayName("FR9: Identify stale flags by 30-day inactivity and 14-day 100% rollout")
    void testStaleFlagDetection() {
        Project project = new Project("p1", "proj", "Project", "Desc");
        Instant now = Instant.now();

        // 1. Unused flag (created 40 days ago, never evaluated)
        Flag unusedFlag = new Flag("f1", project, "unused-feature", "Unused Feature", "",
                FlagType.BOOLEAN, "dev1@flagship.io", "");
        unusedFlag.setCreatedAt(now.minus(40, ChronoUnit.DAYS));
        unusedFlag.setLastEvaluatedAt(null);

        // 2. Fully rolled out flag (100% single variation for 20 days)
        Flag rolledOutFlag = new Flag("f2", project, "permanent-feature", "Permanent Feature", "",
                FlagType.BOOLEAN, "dev2@flagship.io", "");
        rolledOutFlag.setCreatedAt(now.minus(50, ChronoUnit.DAYS));
        rolledOutFlag.setLastEvaluatedAt(now.minus(1, ChronoUnit.DAYS));
        rolledOutFlag.setFullyRolledOutSince(now.minus(20, ChronoUnit.DAYS));

        // 3. Active healthy flag
        Flag activeFlag = new Flag("f3", project, "active-feature", "Active Feature", "",
                FlagType.BOOLEAN, "dev3@flagship.io", "");
        activeFlag.setCreatedAt(now.minus(10, ChronoUnit.DAYS));
        activeFlag.setLastEvaluatedAt(now.minus(1, ChronoUnit.HOURS));
        activeFlag.setFullyRolledOutSince(null);

        List<Flag> flags = List.of(unusedFlag, rolledOutFlag, activeFlag);

        FlagRepository flagRepo = (FlagRepository) Proxy.newProxyInstance(
                FlagRepository.class.getClassLoader(),
                new Class<?>[]{FlagRepository.class},
                (proxy, method, args) -> {
                    if ("findByProjectId".equals(method.getName())) {
                        return flags;
                    }
                    return null;
                }
        );

        FlagHygieneService service = new FlagHygieneService(flagRepo, 30, 14);

        var reports = service.getStaleFlags("p1");

        assertThat(reports).hasSize(2);
        assertThat(reports.stream().map(FlagHygieneService.StaleFlagReport::flagKey))
                .containsExactlyInAnyOrder("unused-feature", "permanent-feature");

        var unusedReport = reports.stream().filter(r -> r.flagKey().equals("unused-feature")).findFirst().get();
        assertThat(unusedReport.ownerEmail()).isEqualTo("dev1@flagship.io");
        assertThat(unusedReport.reason()).contains("No evaluation events recorded in past 30 days");
        assertThat(unusedReport.recommendation()).contains("Archive or safely remove");

        var rolledOutReport = reports.stream().filter(r -> r.flagKey().equals("permanent-feature")).findFirst().get();
        assertThat(rolledOutReport.ownerEmail()).isEqualTo("dev2@flagship.io");
        assertThat(rolledOutReport.reason()).contains("100% fully rolled out");
        assertThat(rolledOutReport.recommendation()).contains("Clean up flag checks");
    }
}
