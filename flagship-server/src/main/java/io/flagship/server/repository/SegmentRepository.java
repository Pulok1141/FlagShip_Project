package io.flagship.server.repository;

import io.flagship.server.domain.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, String> {
    List<Segment> findByProjectId(String projectId);
    Optional<Segment> findByProjectIdAndSegmentKey(String projectId, String segmentKey);
}
