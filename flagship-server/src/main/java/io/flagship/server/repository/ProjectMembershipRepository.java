package io.flagship.server.repository;

import io.flagship.server.domain.ProjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, String> {
    List<ProjectMembership> findByProjectId(String projectId);
    List<ProjectMembership> findByUserId(String userId);
    Optional<ProjectMembership> findByProjectIdAndUserId(String projectId, String userId);
}
