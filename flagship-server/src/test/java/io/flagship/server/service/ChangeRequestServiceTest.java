package io.flagship.server.service;

import io.flagship.server.domain.*;
import io.flagship.server.repository.ChangeRequestRepository;
import io.flagship.server.repository.FlagEnvConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeRequestServiceTest {

    @Test
    @DisplayName("FR7: Four-Eyes Principle strictly prevents author from approving their own request")
    void testFourEyesPrincipleViolation() {
        Project project = new Project("p1", "proj", "Project", "");
        Environment env = new Environment("e1", project, "production", "Production", "key", true);
        Flag flag = new Flag();
        FlagEnvConfig fec = new FlagEnvConfig();
        fec.setEnvironment(env);
        fec.setFlag(flag);

        ChangeRequest cr = new ChangeRequest(
                "cr-1", project, env, fec,
                "{\"enabled\": true}", "{\"enabled\": false}", "{}",
                "author@flagship.io", "Turn on feature"
        );

        ChangeRequestRepository crRepo = (ChangeRequestRepository) Proxy.newProxyInstance(
                ChangeRequestRepository.class.getClassLoader(),
                new Class<?>[]{ChangeRequestRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(cr);
                    }
                    return null;
                }
        );

        FlagEnvConfigRepository fecRepo = (FlagEnvConfigRepository) Proxy.newProxyInstance(
                FlagEnvConfigRepository.class.getClassLoader(),
                new Class<?>[]{FlagEnvConfigRepository.class},
                (proxy, method, args) -> null
        );

        ChangeRequestService service = new ChangeRequestService(crRepo, fecRepo, null);

        // Author attempts to self-approve
        assertThatThrownBy(() -> service.approveAndApply("cr-1", "author@flagship.io"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Four-Eyes Principle violation");
    }

    @Test
    @DisplayName("FR7: Second person approver successfully approves and applies change")
    void testSecondPersonApproval() {
        Project project = new Project("p1", "proj", "Project", "");
        Environment env = new Environment("e1", project, "production", "Production", "key", true);
        Flag flag = new Flag();
        FlagEnvConfig fec = new FlagEnvConfig();
        fec.setEnvironment(env);
        fec.setFlag(flag);

        ChangeRequest cr = new ChangeRequest(
                "cr-2", project, env, fec,
                "{\"enabled\": true, \"variations\": \"[]\", \"defaultVariationId\": \"v1\"}",
                "{\"enabled\": false}",
                "{\"enabled\": {\"old\": false, \"new\": true}}",
                "author@flagship.io", "Turn on feature"
        );

        ChangeRequestRepository crRepo = (ChangeRequestRepository) Proxy.newProxyInstance(
                ChangeRequestRepository.class.getClassLoader(),
                new Class<?>[]{ChangeRequestRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(cr);
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return null;
                }
        );

        FlagEnvConfigRepository fecRepo = (FlagEnvConfigRepository) Proxy.newProxyInstance(
                FlagEnvConfigRepository.class.getClassLoader(),
                new Class<?>[]{FlagEnvConfigRepository.class},
                (proxy, method, args) -> null
        );

        AtomicBoolean applied = new AtomicBoolean(false);
        FlagService flagService = new FlagService(null, null, null, null, null, null, null, null, null) {
            @Override
            public void applyConfigDirectly(FlagEnvConfig targetConfig, boolean enabled, String variationsJson,
                                            String targetsJson, String rulesJson, String rolloutJson,
                                            String defaultVariationId, String offVariationId,
                                            String newConfigSnapshot, String diffJson, String authorEmail, String reason) {
                applied.set(true);
            }
        };

        ChangeRequestService service = new ChangeRequestService(crRepo, fecRepo, flagService);

        ChangeRequest approved = service.approveAndApply("cr-2", "approver@flagship.io");

        assertThat(approved.getStatus()).isEqualTo(ChangeRequest.Status.APPLIED);
        assertThat(approved.getApproverEmail()).isEqualTo("approver@flagship.io");
        assertThat(approved.getResolvedAt()).isNotNull();
        assertThat(applied.get()).isTrue();
    }
}
