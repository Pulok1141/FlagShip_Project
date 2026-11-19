package io.flagship.server.repository;

import io.flagship.server.domain.ScheduledChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ScheduledChangeRepository extends JpaRepository<ScheduledChange, String> {
    List<ScheduledChange> findByFlagEnvConfigId(String flagEnvConfigId);
    List<ScheduledChange> findByStatusAndScheduledTimeLessThanEqual(ScheduledChange.Status status, Instant cutoff);
}
