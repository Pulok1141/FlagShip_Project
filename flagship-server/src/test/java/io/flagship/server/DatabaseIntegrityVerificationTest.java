package io.flagship.server;

import io.flagship.core.model.FlagType;
import io.flagship.server.domain.*;
import io.flagship.server.repository.*;
import io.flagship.server.service.ChangeRequestService;
import io.flagship.server.service.FlagHygieneService;
import io.flagship.server.service.FlagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DatabaseIntegrityVerificationTest {

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private EnvironmentRepository envRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private FlagRepository flagRepo;

    @Autowired
    private FlagEnvConfigRepository fecRepo;

    @Autowired
    private FlagVersionRepository fvRepo;

    @Autowired
    private ChangeRequestRepository crRepo;

    @Autowired
    private ExperimentEventRepository eventRepo;

    @Autowired
    private SegmentRepository segmentRepo;

    @Autowired
    private FlagService flagService;

    @Autowired
    private ChangeRequestService changeRequestService;

    @Autowired
    private FlagHygieneService flagHygieneService;

    @Test
    @DisplayName("Comprehensive Database Integrity: CRUD, Transactions, 4-Eyes Governance, Event Ingestion & Cascade Cleanup")
    void testFullDatabaseLifecycleAndIntegrity() {
        // 1. Verify Seed Users and Default Project from Flyway V1 migration
        assertThat(userRepo.findByEmail("admin@flagship.io")).isPresent();
        assertThat(userRepo.findByEmail("approver@flagship.io")).isPresent();
        assertThat(userRepo.findByEmail("editor@flagship.io")).isPresent();
        assertThat(userRepo.findByEmail("viewer@flagship.io")).isPresent();

        Project defaultProj = projectRepo.findByProjectKey("default").orElseThrow();
        List<Environment> defaultEnvs = envRepo.findByProjectId(defaultProj.getId());
        assertThat(defaultEnvs).hasSize(3); // dev, staging, prod

        // 2. Create a new custom Project
        String testProjKey = "db-test-proj-" + UUID.randomUUID().toString().substring(0, 8);
        Project newProj = new Project("proj-" + UUID.randomUUID(), testProjKey, "DB Test Project", "Database validation project");
        newProj = projectRepo.save(newProj);
        assertThat(projectRepo.findById(newProj.getId())).isPresent();

        // 3. Create Environments for this project
        Environment devEnv = new Environment("env-" + UUID.randomUUID(), newProj, "development", "Development", "key-dev-" + UUID.randomUUID(), false);
        Environment prodEnv = new Environment("env-" + UUID.randomUUID(), newProj, "production", "Production", "key-prod-" + UUID.randomUUID(), true);
        devEnv = envRepo.save(devEnv);
        prodEnv = envRepo.save(prodEnv);

        List<Environment> projEnvs = envRepo.findByProjectId(newProj.getId());
        assertThat(projEnvs).hasSize(2);

        // 4. Create Flags under the new project
        String variationsJson = """
                [
                  {"id": "var-v1", "name": "V1", "value": "old-checkout"},
                  {"id": "var-v2", "name": "V2", "value": "new-checkout"}
                ]
                """;

        Flag flag = new Flag(
                null,
                newProj,
                "feature-checkout-v2",
                "Checkout V2 Feature",
                "Multi-variation gradual rollout",
                FlagType.STRING,
                "editor@flagship.io",
                "billing,v2"
        );

        Flag savedFlag = flagService.createFlag(newProj.getId(), flag, variationsJson, "var-v1");
        assertThat(savedFlag.getId()).isNotNull();

        // Verify FlagEnvConfig created for both dev and prod
        List<FlagEnvConfig> configs = fecRepo.findByEnvironmentId(devEnv.getId());
        assertThat(configs).hasSize(1);
        FlagEnvConfig devConfig = configs.get(0);
        assertThat(devConfig.getVersion()).isEqualTo(1L);

        FlagEnvConfig prodConfig = fecRepo.findByEnvironmentId(prodEnv.getId()).get(0);
        assertThat(prodConfig.getVersion()).isEqualTo(1L);

        // 5. Direct update in Development (requireApproval = false)
        Map<String, Object> devUpdateResult = flagService.updateFlagConfig(
                devConfig.getId(),
                true,
                variationsJson,
                "[]",
                "[]",
                "{}",
                "var-v2",
                "var-v1",
                "editor@flagship.io",
                "Enable in dev"
        );
        assertThat(devUpdateResult.get("status")).isEqualTo("APPLIED");
        FlagEnvConfig updatedDevConfig = fecRepo.findById(devConfig.getId()).orElseThrow();
        assertThat(updatedDevConfig.isEnabled()).isTrue();
        assertThat(updatedDevConfig.getVersion()).isEqualTo(2L);

        // Verify FlagVersion audit record was recorded in DB
        List<FlagVersion> devVersions = fvRepo.findByFlagEnvConfigIdOrderByVersionDesc(devConfig.getId());
        assertThat(devVersions).hasSize(2); // v1 and v2

        // 6. Update in Production (requireApproval = true) -> Creates Change Request
        Map<String, Object> prodUpdateResult = flagService.updateFlagConfig(
                prodConfig.getId(),
                true,
                variationsJson,
                "[]",
                "[]",
                "{}",
                "var-v2",
                "var-v1",
                "editor@flagship.io",
                "Rollout to prod"
        );
        assertThat(prodUpdateResult.get("status")).isEqualTo("PENDING_APPROVAL");
        String crId = (String) prodUpdateResult.get("changeRequestId");
        assertThat(crId).isNotNull();

        // 7. Verify 4-Eyes Principle: Author cannot self-approve
        assertThatThrownBy(() -> changeRequestService.approveAndApply(crId, "editor@flagship.io"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Four-Eyes Principle violation");

        // 8. Approver approves -> Change Request APPLIED in DB
        ChangeRequest approvedCr = changeRequestService.approveAndApply(crId, "approver@flagship.io");
        assertThat(approvedCr.getStatus()).isEqualTo(ChangeRequest.Status.APPLIED);
        assertThat(approvedCr.getApproverEmail()).isEqualTo("approver@flagship.io");

        FlagEnvConfig updatedProdConfig = fecRepo.findById(prodConfig.getId()).orElseThrow();
        assertThat(updatedProdConfig.isEnabled()).isTrue();
        assertThat(updatedProdConfig.getVersion()).isEqualTo(2L);

        // 9. Experiment Events Ingestion & Aggregation Query
        List<ExperimentEvent> batchEvents = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            batchEvents.add(new ExperimentEvent(
                    null,
                    newProj.getId(),
                    "production",
                    "feature-checkout-v2",
                    "var-v2",
                    "usr-bench-" + i,
                    ExperimentEvent.EventType.EXPOSURE,
                    null,
                    1.0,
                    Instant.now()
            ));
            if (i % 5 == 0) {
                batchEvents.add(new ExperimentEvent(
                        null,
                        newProj.getId(),
                        "production",
                        "feature-checkout-v2",
                        "var-v2",
                        "usr-bench-" + i,
                        ExperimentEvent.EventType.GOAL,
                        "checkout_completed",
                        49.99,
                        Instant.now()
                ));
            }
        }
        eventRepo.saveAll(batchEvents);

        List<ExperimentEvent> queriedEvents = eventRepo.findEventsForAnalysis(
                newProj.getId(),
                "production",
                "feature-checkout-v2",
                Instant.now().minus(1, ChronoUnit.DAYS)
        );
        assertThat(queriedEvents).hasSize(60); // 50 exposures + 10 conversions

        // 10. Flag Hygiene Query Test
        List<FlagHygieneService.StaleFlagReport> hygieneReport = flagHygieneService.getStaleFlags(newProj.getId());
        assertThat(hygieneReport).isNotNull();

        // 11. Segments CRUD
        Segment segment = new Segment(
                null,
                newProj,
                "beta-testers",
                "Beta Testers Segment",
                "Early access users",
                "{\"operator\":\"EQUALS\",\"attribute\":\"tier\",\"values\":[\"beta\"]}"
        );
        Segment savedSegment = segmentRepo.save(segment);
        assertThat(segmentRepo.findById(savedSegment.getId())).isPresent();

        // 12. Clean up test records in order
        crRepo.deleteAll(crRepo.findByProjectId(newProj.getId()));
        fvRepo.deleteAll(fvRepo.findByFlagEnvConfigIdOrderByVersionDesc(devConfig.getId()));
        fvRepo.deleteAll(fvRepo.findByFlagEnvConfigIdOrderByVersionDesc(prodConfig.getId()));
        fecRepo.delete(devConfig);
        fecRepo.delete(prodConfig);
        flagRepo.delete(savedFlag);
        segmentRepo.delete(savedSegment);
        eventRepo.deleteAll(queriedEvents);
        envRepo.deleteAll(List.of(devEnv, prodEnv));
        projectRepo.delete(newProj);

        assertThat(projectRepo.findById(newProj.getId())).isEmpty();
        assertThat(envRepo.findByProjectId(newProj.getId())).isEmpty();
        assertThat(flagRepo.findByProjectId(newProj.getId())).isEmpty();
        assertThat(fecRepo.findById(devConfig.getId())).isEmpty();
        assertThat(fecRepo.findById(prodConfig.getId())).isEmpty();
        assertThat(segmentRepo.findByProjectId(newProj.getId())).isEmpty();

        System.out.println("DATABASE INTEGRITY VERIFICATION: 100% SUCCESSFUL!");
    }
}
