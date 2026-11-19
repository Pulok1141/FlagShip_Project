package io.flagship.server.repository;

import io.flagship.server.domain.FlagVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlagVersionRepository extends JpaRepository<FlagVersion, String> {
    List<FlagVersion> findByFlagEnvConfigIdOrderByVersionDesc(String flagEnvConfigId);
    Optional<FlagVersion> findByFlagEnvConfigIdAndVersion(String flagEnvConfigId, long version);
}
