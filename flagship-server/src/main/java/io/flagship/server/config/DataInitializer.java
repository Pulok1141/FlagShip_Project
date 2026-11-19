package io.flagship.server.config;

import io.flagship.core.ast.LeafConditionNode;
import io.flagship.core.ast.OperatorType;
import io.flagship.core.model.*;
import io.flagship.server.domain.*;
import io.flagship.server.repository.*;
import io.flagship.server.service.FlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final FlagRepository flagRepository;
    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final FlagService flagService;

    public DataInitializer(
            UserRepository userRepository,
            FlagRepository flagRepository,
            ProjectRepository projectRepository,
            EnvironmentRepository environmentRepository,
            PasswordEncoder passwordEncoder,
            FlagService flagService
    ) {
        this.userRepository = userRepository;
        this.flagRepository = flagRepository;
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.flagService = flagService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Ensure known users have valid BCrypt passwords matching: <role>123
        updatePasswordIfPresent("admin@flagship.io", "admin123");
        updatePasswordIfPresent("editor@flagship.io", "editor123");
        updatePasswordIfPresent("approver@flagship.io", "approver123");
        updatePasswordIfPresent("viewer@flagship.io", "viewer123");

        // Seed a sample feature flag if no flags exist
        projectRepository.findByProjectKey("default").ifPresent(project -> {
            if (flagRepository.findByProjectId(project.getId()).isEmpty()) {
                seedDemoFlags(project);
            }
        });
    }

    private void updatePasswordIfPresent(String email, String rawPassword) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
        });
    }

    private void seedDemoFlags(Project project) {
        Variation vTrue = new Variation("true-var", "True", true);
        Variation vFalse = new Variation("false-var", "False", false);
        String variationsJson = SnapshotJsonMapper.toJson(List.of(vTrue, vFalse));

        Flag flag = new Flag(
                null,
                project,
                "new-checkout-flow",
                "New One-Click Checkout Flow",
                "Gradual rollout of the redesigned checkout pipeline",
                FlagType.BOOLEAN,
                "admin@flagship.io",
                "checkout,v2,ecommerce"
        );

        flagService.createFlag(project.getId(), flag, variationsJson, "false-var");
        log.info("Seeded initial demo feature flag: new-checkout-flow");
    }
}
