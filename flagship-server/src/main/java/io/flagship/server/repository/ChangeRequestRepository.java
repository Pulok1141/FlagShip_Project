package io.flagship.server.repository;

import io.flagship.server.domain.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, String> {
    List<ChangeRequest> findByProjectId(String projectId);
    List<ChangeRequest> findByProjectIdAndStatus(String projectId, ChangeRequest.Status status);
    List<ChangeRequest> findByFlagEnvConfigId(String flagEnvConfigId);
}
