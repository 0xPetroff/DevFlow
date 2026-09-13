CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER',
    avatar_color    VARCHAR(7),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role     CHECK (role IN ('ADMIN', 'DEVELOPER', 'VIEWER')),
    -- Emails are normalised to lower case on write so uniqueness is genuinely case-insensitive.
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);

CREATE TABLE projects (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_key     VARCHAR(10)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    repository_url  VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    owner_id        UUID         NOT NULL,
    -- Monotonic per-project counter backing the DEVF-42 style issue keys.
    issue_sequence  INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_projects_key    UNIQUE (project_key),
    CONSTRAINT fk_projects_owner  FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_projects_key    CHECK (project_key ~ '^[A-Z][A-Z0-9]{1,9}$'),
    CONSTRAINT ck_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_projects_sequence CHECK (issue_sequence >= 0)
);

CREATE TABLE project_members (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'DEVELOPER',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_members       UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_members_proj  FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_project_members_role  CHECK (role IN ('ADMIN', 'DEVELOPER', 'VIEWER'))
);

CREATE TABLE labels (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL,
    name        VARCHAR(40) NOT NULL,
    color       VARCHAR(7)  NOT NULL DEFAULT '#6366f1',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_labels_project_name UNIQUE (project_id, name),
    CONSTRAINT fk_labels_project      FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT ck_labels_color        CHECK (color ~ '^#[0-9a-fA-F]{6}$'),
    CONSTRAINT ck_labels_name         CHECK (length(btrim(name)) > 0)
);

CREATE TABLE issues (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID         NOT NULL,
    issue_number    INTEGER      NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    priority        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    type            VARCHAR(20)  NOT NULL DEFAULT 'TASK',
    assignee_id     UUID,
    creator_id      UUID         NOT NULL,
    due_date        DATE,
    -- Sparse ordering key: dragging a card averages its neighbours instead of renumbering the column.
    board_position  DOUBLE PRECISION NOT NULL DEFAULT 1000,
    estimate_points SMALLINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_issues_project_number UNIQUE (project_id, issue_number),
    CONSTRAINT fk_issues_project        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_issues_assignee       FOREIGN KEY (assignee_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_issues_creator        FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_issues_status         CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    CONSTRAINT ck_issues_priority       CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_issues_type           CHECK (type IN ('TASK', 'BUG', 'FEATURE', 'IMPROVEMENT')),
    CONSTRAINT ck_issues_number         CHECK (issue_number > 0),
    CONSTRAINT ck_issues_title          CHECK (length(btrim(title)) > 0),
    CONSTRAINT ck_issues_estimate       CHECK (estimate_points IS NULL OR estimate_points BETWEEN 0 AND 100)
);

CREATE TABLE issue_labels (
    issue_id UUID NOT NULL,
    label_id UUID NOT NULL,
    CONSTRAINT pk_issue_labels       PRIMARY KEY (issue_id, label_id),
    CONSTRAINT fk_issue_labels_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_labels_label FOREIGN KEY (label_id) REFERENCES labels (id) ON DELETE CASCADE
);

CREATE TABLE comments (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id    UUID        NOT NULL,
    author_id   UUID        NOT NULL,
    body        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_comments_issue  FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_comments_body   CHECK (length(btrim(body)) > 0)
);

CREATE TABLE environments (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        UUID        NOT NULL,
    name              VARCHAR(60) NOT NULL,
    type              VARCHAR(20) NOT NULL,
    url               VARCHAR(500),
    requires_approval BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_environments_project_name UNIQUE (project_id, name),
    CONSTRAINT fk_environments_project      FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT ck_environments_type         CHECK (type IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION'))
);

CREATE TABLE deployments (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id         UUID         NOT NULL,
    environment_id     UUID         NOT NULL,
    release_version    VARCHAR(60)  NOT NULL,
    commit_hash        VARCHAR(40)  NOT NULL,
    commit_message     VARCHAR(500),
    branch             VARCHAR(200) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- Null when a CI service account triggered the run; triggered_by_label always carries a display name.
    triggered_by_id    UUID,
    triggered_by_label VARCHAR(120) NOT NULL,
    pipeline_url       VARCHAR(500),
    failure_reason     TEXT,
    queued_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_at         TIMESTAMPTZ,
    finished_at        TIMESTAMPTZ,
    duration_seconds   INTEGER,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_deployments_project     FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_deployments_environment FOREIGN KEY (environment_id) REFERENCES environments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_deployments_user        FOREIGN KEY (triggered_by_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_deployments_status      CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_deployments_commit      CHECK (commit_hash ~ '^[0-9a-fA-F]{7,40}$'),
    CONSTRAINT ck_deployments_finish      CHECK (finished_at IS NULL OR started_at IS NOT NULL),
    CONSTRAINT ck_deployments_order       CHECK (finished_at IS NULL OR finished_at >= started_at),
    CONSTRAINT ck_deployments_duration    CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

CREATE TABLE api_keys (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    -- Public, indexed lookup handle. The secret itself is only ever stored as key_hash (SHA-256 hex).
    key_prefix    VARCHAR(16)  NOT NULL,
    key_hash      VARCHAR(64)  NOT NULL,
    project_id    UUID,
    scopes        VARCHAR(255) NOT NULL DEFAULT 'deployment:write',
    created_by_id UUID,
    last_used_at  TIMESTAMPTZ,
    expires_at    TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_api_keys_prefix  UNIQUE (key_prefix),
    CONSTRAINT uq_api_keys_hash    UNIQUE (key_hash),
    CONSTRAINT fk_api_keys_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_api_keys_creator FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE refresh_tokens (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL,
    token_hash     VARCHAR(64) NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    replaced_by_id UUID,
    user_agent     VARCHAR(255),
    ip_address     VARCHAR(45),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_hash    UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id) ON DELETE SET NULL
);

CREATE TABLE audit_logs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID,
    actor_label VARCHAR(120) NOT NULL,
    action      VARCHAR(40)  NOT NULL,
    entity_type VARCHAR(40)  NOT NULL,
    entity_id   UUID,
    project_id  UUID,
    summary     VARCHAR(500) NOT NULL,
    metadata    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_logs_actor   FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
);

CREATE INDEX idx_projects_owner            ON projects (owner_id);
CREATE INDEX idx_projects_status           ON projects (status);
CREATE INDEX idx_project_members_user      ON project_members (user_id);
CREATE INDEX idx_project_members_project   ON project_members (project_id);
CREATE INDEX idx_labels_project            ON labels (project_id);
CREATE INDEX idx_issues_project_status     ON issues (project_id, status);
CREATE INDEX idx_issues_assignee_status    ON issues (assignee_id, status) WHERE assignee_id IS NOT NULL;
CREATE INDEX idx_issues_creator            ON issues (creator_id);
CREATE INDEX idx_issues_board              ON issues (project_id, status, board_position);
CREATE INDEX idx_issues_due_date           ON issues (due_date) WHERE due_date IS NOT NULL;
CREATE INDEX idx_issue_labels_label        ON issue_labels (label_id);
CREATE INDEX idx_comments_issue            ON comments (issue_id, created_at);
CREATE INDEX idx_comments_author           ON comments (author_id);
CREATE INDEX idx_environments_project      ON environments (project_id);
CREATE INDEX idx_deployments_project_time  ON deployments (project_id, queued_at DESC);
CREATE INDEX idx_deployments_environment   ON deployments (environment_id, queued_at DESC);
CREATE INDEX idx_deployments_status        ON deployments (status);
CREATE INDEX idx_deployments_user          ON deployments (triggered_by_id);
CREATE INDEX idx_api_keys_project          ON api_keys (project_id);
CREATE INDEX idx_api_keys_creator          ON api_keys (created_by_id);
CREATE INDEX idx_refresh_tokens_user       ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expiry     ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_replaced   ON refresh_tokens (replaced_by_id);
CREATE INDEX idx_audit_logs_created        ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_project_time   ON audit_logs (project_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor          ON audit_logs (actor_id);
CREATE INDEX idx_audit_logs_entity         ON audit_logs (entity_type, entity_id);

-- Guarantees updated_at is correct even for writes that bypass the JPA auditing listener.
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at        BEFORE UPDATE ON users        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_projects_updated_at     BEFORE UPDATE ON projects     FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_issues_updated_at       BEFORE UPDATE ON issues       FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_comments_updated_at     BEFORE UPDATE ON comments     FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_environments_updated_at BEFORE UPDATE ON environments FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_deployments_updated_at  BEFORE UPDATE ON deployments  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
