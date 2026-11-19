package io.flagship.server.repository;

import io.flagship.server.domain.FlagEnvConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlagEnvConfigRepository extends JpaRepository<FlagEnvConfig, String> {
    Optional<FlagEnvConfig> findByFlagIdAndEnvironmentId(String flagId, String environmentId);

    @Query("SELECT fc FROM FlagEnvConfig fc WHERE fc.environment.project.id = :projectId AND fc.environment.envKey = :envKey")
    List<FlagEnvConfig> findByProjectIdAndEnvKey(@Param("projectId") String projectId, @Param("envKey") String envKey);

    @Query("SELECT fc FROM FlagEnvConfig fc WHERE fc.environment.id = :environmentId")
    List<FlagEnvConfig> findByEnvironmentId(@Param("environmentId") String environmentId);
}
