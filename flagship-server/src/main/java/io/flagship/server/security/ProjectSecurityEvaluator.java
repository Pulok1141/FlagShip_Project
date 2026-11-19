package io.flagship.server.security;

import io.flagship.server.domain.ChangeRequest;
import io.flagship.server.domain.ProjectMembership;
import io.flagship.server.domain.ProjectRole;
import io.flagship.server.domain.User;
import io.flagship.server.repository.ChangeRequestRepository;
import io.flagship.server.repository.ProjectMembershipRepository;
import io.flagship.server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Custom SpEL security evaluator for per-project RBAC and Four-Eyes Principle enforcement (FR7, FR10).
 */
@Service("projectSec")
public class ProjectSecurityEvaluator {

    private final ProjectMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final ChangeRequestRepository changeRequestRepository;

    public ProjectSecurityEvaluator(
            ProjectMembershipRepository membershipRepository,
            UserRepository userRepository,
            ChangeRequestRepository changeRequestRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.changeRequestRepository = changeRequestRepository;
    }

    public boolean hasRole(String projectId, String requiredRoleStr) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String userId = auth.getName();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            // Check if authenticated by email
            userOpt = userRepository.findByEmail(userId);
        }
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();

        // Global admin has access to all projects
        if ("GLOBAL_ADMIN".equalsIgnoreCase(user.getGlobalRole())) {
            return true;
        }

        ProjectRole requiredRole = ProjectRole.valueOf(requiredRoleStr.toUpperCase());
        Optional<ProjectMembership> membershipOpt = membershipRepository.findByProjectIdAndUserId(projectId, user.getId());
        if (membershipOpt.isEmpty()) {
            return false;
        }

        ProjectRole userRole = membershipOpt.get().getRole();

        // Check role hierarchy: ADMIN > APPROVER > EDITOR > VIEWER
        return switch (requiredRole) {
            case VIEWER -> true;
            case EDITOR -> userRole.canEdit();
            case APPROVER -> userRole.canApprove();
            case ADMIN -> userRole.canAdminister();
        };
    }

    /**
     * Enforce Four-Eyes Principle for production approvals (FR7):
     * The approver MUST have APPROVER or ADMIN role AND cannot be the change request author!
     */
    public boolean canApprove(String changeRequestId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Optional<ChangeRequest> crOpt = changeRequestRepository.findById(changeRequestId);
        if (crOpt.isEmpty()) {
            return false;
        }
        ChangeRequest cr = crOpt.get();

        String userId = auth.getName();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(userId);
        }
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();

        // Four-eyes principle: Author CANNOT approve their own change request!
        if (user.getEmail().equalsIgnoreCase(cr.getAuthorEmail())) {
            return false;
        }

        // Must have APPROVER or ADMIN role in the project
        return hasRole(cr.getProject().getId(), "APPROVER");
    }
}
