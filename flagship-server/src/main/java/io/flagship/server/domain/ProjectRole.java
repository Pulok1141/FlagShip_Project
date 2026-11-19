package io.flagship.server.domain;

/**
 * Per-project RBAC roles (FR10).
 * Precedence: VIEWER < EDITOR < APPROVER < ADMIN
 */
public enum ProjectRole {
    VIEWER,
    EDITOR,
    APPROVER,
    ADMIN;

    public boolean canEdit() {
        return this == EDITOR || this == APPROVER || this == ADMIN;
    }

    public boolean canApprove() {
        return this == APPROVER || this == ADMIN;
    }

    public boolean canAdminister() {
        return this == ADMIN;
    }
}
