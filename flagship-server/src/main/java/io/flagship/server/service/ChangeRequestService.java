package io.flagship.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.flagship.core.model.SnapshotJsonMapper;
import io.flagship.server.domain.ChangeRequest;
import io.flagship.server.domain.FlagEnvConfig;
import io.flagship.server.repository.ChangeRequestRepository;
import io.flagship.server.repository.FlagEnvConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final FlagEnvConfigRepository flagEnvConfigRepository;
    private final FlagService flagService;

    public ChangeRequestService(
            ChangeRequestRepository changeRequestRepository,
            FlagEnvConfigRepository flagEnvConfigRepository,
            FlagService flagService
    ) {
        this.changeRequestRepository = changeRequestRepository;
        this.flagEnvConfigRepository = flagEnvConfigRepository;
        this.flagService = flagService;
    }

    public List<ChangeRequest> getPendingRequests(String projectId) {
        return changeRequestRepository.findByProjectIdAndStatus(projectId, ChangeRequest.Status.PENDING_APPROVAL);
    }

    public List<ChangeRequest> getAllRequests(String projectId) {
        return changeRequestRepository.findByProjectId(projectId);
    }

    @Transactional
    public ChangeRequest approveAndApply(String changeRequestId, String approverEmail) {
        ChangeRequest cr = changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found: " + changeRequestId));

        if (cr.getStatus() != ChangeRequest.Status.PENDING_APPROVAL) {
            throw new IllegalStateException("Change request is not in PENDING_APPROVAL state: " + cr.getStatus());
        }

        // Four-Eyes Principle check: author != approver
        if (cr.getAuthorEmail().equalsIgnoreCase(approverEmail)) {
            throw new IllegalArgumentException("Four-Eyes Principle violation: Author cannot approve their own change request!");
        }

        FlagEnvConfig config = cr.getFlagEnvConfig();
        Map<String, Object> proposed = SnapshotJsonMapper.fromJson(cr.getProposedConfigJson(), new TypeReference<Map<String, Object>>() {});

        boolean enabled = Boolean.TRUE.equals(proposed.get("enabled"));
        String variationsJson = (String) proposed.get("variations");
        String targetsJson = (String) proposed.get("targets");
        String rulesJson = (String) proposed.get("rules");
        String rolloutJson = (String) proposed.get("rollout");
        String defaultVariationId = (String) proposed.get("defaultVariationId");
        String offVariationId = (String) proposed.get("offVariationId");

        // Apply configuration update directly
        flagService.applyConfigDirectly(
                config,
                enabled,
                variationsJson,
                targetsJson,
                rulesJson,
                rolloutJson,
                defaultVariationId,
                offVariationId,
                cr.getProposedConfigJson(),
                cr.getDiffJson(),
                cr.getAuthorEmail(),
                "Approved and applied by " + approverEmail + ": " + cr.getReason()
        );

        cr.setStatus(ChangeRequest.Status.APPLIED);
        cr.setApproverEmail(approverEmail);
        cr.setResolvedAt(Instant.now());
        return changeRequestRepository.save(cr);
    }

    @Transactional
    public ChangeRequest reject(String changeRequestId, String approverEmail, String rejectionReason) {
        ChangeRequest cr = changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found: " + changeRequestId));

        if (cr.getStatus() != ChangeRequest.Status.PENDING_APPROVAL) {
            throw new IllegalStateException("Change request is not pending: " + cr.getStatus());
        }

        cr.setStatus(ChangeRequest.Status.REJECTED);
        cr.setApproverEmail(approverEmail);
        cr.setRejectionReason(rejectionReason);
        cr.setResolvedAt(Instant.now());
        return changeRequestRepository.save(cr);
    }
}
