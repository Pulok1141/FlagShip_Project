package io.flagship.server.controller;

import io.flagship.server.domain.ChangeRequest;
import io.flagship.server.service.ChangeRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/change-requests")
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    public ChangeRequestController(ChangeRequestService changeRequestService) {
        this.changeRequestService = changeRequestService;
    }

    @GetMapping
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public List<ChangeRequest> listRequests(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "false") boolean pendingOnly
    ) {
        if (pendingOnly) {
            return changeRequestService.getPendingRequests(projectId);
        }
        return changeRequestService.getAllRequests(projectId);
    }

    @PostMapping("/{changeRequestId}/approve")
    @PreAuthorize("@projectSec.canApprove(#changeRequestId)")
    public ResponseEntity<ChangeRequest> approveRequest(
            @PathVariable String projectId,
            @PathVariable String changeRequestId,
            Authentication auth
    ) {
        ChangeRequest approved = changeRequestService.approveAndApply(changeRequestId, auth.getName());
        return ResponseEntity.ok(approved);
    }

    public record RejectRequest(String reason) {}

    @PostMapping("/{changeRequestId}/reject")
    @PreAuthorize("@projectSec.hasRole(#projectId, 'APPROVER')")
    public ResponseEntity<ChangeRequest> rejectRequest(
            @PathVariable String projectId,
            @PathVariable String changeRequestId,
            @RequestBody RejectRequest req,
            Authentication auth
    ) {
        ChangeRequest rejected = changeRequestService.reject(changeRequestId, auth.getName(), req.reason());
        return ResponseEntity.ok(rejected);
    }
}
