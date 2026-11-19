package io.flagship.server.repository;

import io.flagship.server.domain.ExperimentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ExperimentEventRepository extends JpaRepository<ExperimentEvent, String> {

    @Query("SELECT e FROM ExperimentEvent e WHERE e.projectId = :projectId AND e.environmentKey = :environmentKey AND e.flagKey = :flagKey AND e.timestamp >= :since")
    List<ExperimentEvent> findEventsForAnalysis(
            @Param("projectId") String projectId,
            @Param("environmentKey") String environmentKey,
            @Param("flagKey") String flagKey,
            @Param("since") Instant since
    );
}
