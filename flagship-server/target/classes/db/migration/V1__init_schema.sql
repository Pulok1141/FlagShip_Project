-- FlagShip Database Schema (PostgreSQL 16 & H2 Compatible)

CREATE TABLE users (
    id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    global_role VARCHAR(32) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id VARCHAR(64) PRIMARY KEY,
    project_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_memberships (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL, -- VIEWER, EDITOR, APPROVER, ADMIN
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_project_user UNIQUE (project_id, user_id)
);

CREATE TABLE environments (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    env_key VARCHAR(64) NOT NULL, -- development, staging, production
    name VARCHAR(128) NOT NULL,
    api_key VARCHAR(128) NOT NULL UNIQUE,
    require_approval BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_project_env UNIQUE (project_id, env_key)
);

CREATE TABLE flags (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    flag_key VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    flag_type VARCHAR(32) NOT NULL, -- BOOLEAN, STRING, NUMBER, JSON
    owner_email VARCHAR(255) NOT NULL,
    tags TEXT, -- comma-separated
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_evaluated_at TIMESTAMP WITH TIME ZONE,
    fully_rolled_out_since TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_project_flag UNIQUE (project_id, flag_key)
);

CREATE TABLE flag_env_configs (
    id VARCHAR(64) PRIMARY KEY,
    flag_id VARCHAR(64) NOT NULL REFERENCES flags(id) ON DELETE CASCADE,
    environment_id VARCHAR(64) NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    variations_json TEXT NOT NULL,
    targets_json TEXT NOT NULL DEFAULT '[]',
    rules_json TEXT NOT NULL DEFAULT '[]',
    rollout_json TEXT NOT NULL DEFAULT '{}',
    default_variation_id VARCHAR(64) NOT NULL,
    off_variation_id VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flag_env UNIQUE (flag_id, environment_id)
);

CREATE TABLE segments (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    segment_key VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    condition_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_project_segment UNIQUE (project_id, segment_key)
);

CREATE TABLE change_requests (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    environment_id VARCHAR(64) NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    flag_env_config_id VARCHAR(64) NOT NULL REFERENCES flag_env_configs(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL, -- PENDING_APPROVAL, APPROVED, REJECTED, APPLIED, CANCELLED
    proposed_config_json TEXT NOT NULL,
    current_config_json TEXT NOT NULL,
    diff_json TEXT NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    approver_email VARCHAR(255),
    reason TEXT NOT NULL,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE flag_versions (
    id VARCHAR(64) PRIMARY KEY,
    flag_env_config_id VARCHAR(64) NOT NULL REFERENCES flag_env_configs(id) ON DELETE CASCADE,
    version BIGINT NOT NULL,
    snapshot_json TEXT NOT NULL,
    diff_json TEXT,
    author_email VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scheduled_changes (
    id VARCHAR(64) PRIMARY KEY,
    flag_env_config_id VARCHAR(64) NOT NULL REFERENCES flag_env_configs(id) ON DELETE CASCADE,
    scheduled_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, EXECUTED, FAILED, CANCELLED
    proposed_config_json TEXT NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    executed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE experiment_events (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    environment_key VARCHAR(64) NOT NULL,
    flag_key VARCHAR(128) NOT NULL,
    variation_id VARCHAR(64) NOT NULL,
    user_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(32) NOT NULL, -- EXPOSURE, GOAL
    metric_name VARCHAR(128),
    metric_value DOUBLE PRECISION DEFAULT 1.0,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exp_event_query ON experiment_events(project_id, environment_key, flag_key, event_type);
CREATE INDEX idx_flag_last_eval ON flags(last_evaluated_at);
CREATE INDEX idx_flag_fully_rolled ON flags(fully_rolled_out_since);
CREATE INDEX idx_sched_time ON scheduled_changes(scheduled_time, status);

-- Seed default users with exact BCrypt passwords (passwords: admin123, editor123, approver123, viewer123)
INSERT INTO users (id, email, password_hash, full_name, global_role) VALUES
('usr-admin', 'admin@flagship.io', '$2a$10$EqU4RspP8/DoFfNaZljJD.1xpLzJAOdQH7d6Cc9.vOOLgL2Qnp7Am', 'System Administrator', 'GLOBAL_ADMIN'),
('usr-editor', 'editor@flagship.io', '$2a$10$0tapDNCCCo7ogdHVdPLfFOoG28gD39ssz0Ztr8h/Akc9MpzOOzJyC', 'Feature Flag Editor', 'USER'),
('usr-approver', 'approver@flagship.io', '$2a$10$qWhL6GDu.IiD2bw49O6hu.US9wBbC.3DG1xWkquIVVVRd4HvA8Z3a', 'Production Approver', 'USER'),
('usr-viewer', 'viewer@flagship.io', '$2a$10$UNCA9/GLLCOHIphkKgXSfOyjG7sMpFBWzQjU6i2.JSwCQ8yYVbZGK', 'Read-Only Viewer', 'USER');

-- Seed default project
INSERT INTO projects (id, project_key, name, description) VALUES
('proj-default', 'default', 'Default Project', 'Primary demonstration project for FlagShip');

-- Seed project memberships
INSERT INTO project_memberships (id, project_id, user_id, role) VALUES
('pm-admin', 'proj-default', 'usr-admin', 'ADMIN'),
('pm-editor', 'proj-default', 'usr-editor', 'EDITOR'),
('pm-approver', 'proj-default', 'usr-approver', 'APPROVER'),
('pm-viewer', 'proj-default', 'usr-viewer', 'VIEWER');

-- Seed environments
INSERT INTO environments (id, project_id, env_key, name, api_key, require_approval) VALUES
('env-dev', 'proj-default', 'development', 'Development', 'flagship_dev_key_12345', FALSE),
('env-stage', 'proj-default', 'staging', 'Staging', 'flagship_staging_key_12345', FALSE),
('env-prod', 'proj-default', 'production', 'Production', 'flagship_prod_key_12345', TRUE);
