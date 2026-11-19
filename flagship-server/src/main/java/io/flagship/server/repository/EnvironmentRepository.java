package io.flagship.server.repository;

import io.flagship.server.domain.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, String> {
    List<Environment> findByProjectId(String projectId);
    Optional<Environment> findByProjectIdAndEnvKey(String projectId, String envKey);
    Optional<Environment> findByApiKey(String apiKey);
}
