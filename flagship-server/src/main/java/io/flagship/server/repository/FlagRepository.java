package io.flagship.server.repository;

import io.flagship.server.domain.Flag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlagRepository extends JpaRepository<Flag, String> {
    List<Flag> findByProjectId(String projectId);
    Optional<Flag> findByProjectIdAndFlagKey(String projectId, String flagKey);

    @Query("SELECT f FROM Flag f WHERE f.project.id = :projectId AND (f.lastEvaluatedAt IS NULL OR f.lastEvaluatedAt < :staleEvalCutoff OR (f.fullyRolledOutSince IS NOT NULL AND f.fullyRolledOutSince < :staleRolloutCutoff))")
    List<Flag> findStaleFlags(
            @Param("projectId") String projectId,
            @Param("staleEvalCutoff") Instant staleEvalCutoff,
            @Param("staleRolloutCutoff") Instant staleRolloutCutoff
    );
}
