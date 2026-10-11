-- EcoTrack Schema Migration V7
-- Phase 7: Admin Governance, Municipal Telemetry & Dispute Resolution

-- 1. Add Role Column to app_users table
ALTER TABLE app_users
    ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'CITIZEN'
    CHECK (role IN ('CITIZEN', 'TRANSPORTER', 'RECYCLER', 'MUNICIPAL', 'ADMIN'));

CREATE INDEX app_users_role_idx ON app_users(role);

-- 2. Admin & Municipal Disputes Table
CREATE TABLE admin_disputes (
    id               UUID PRIMARY KEY,
    reporter_id      UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    dispute_type     VARCHAR(50) NOT NULL CHECK (dispute_type IN (
        'pickup_dispute',
        'marketplace_transaction',
        'fraudulent_report',
        'weighbridge_mismatch',
        'other'
    )),
    reference_id     VARCHAR(100) NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'open' CHECK (status IN (
        'open',
        'investigating',
        'resolved',
        'dismissed'
    )),
    resolution_notes TEXT,
    resolved_by      UUID REFERENCES app_users(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ
);

CREATE INDEX admin_disputes_status_idx ON admin_disputes(status, created_at DESC);
CREATE INDEX admin_disputes_reporter_idx ON admin_disputes(reporter_id);

-- 3. System Administrative Audit Logs Table
CREATE TABLE admin_audit_logs (
    id               UUID PRIMARY KEY,
    actor_id         UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    action           VARCHAR(100) NOT NULL,
    target_type      VARCHAR(50) NOT NULL,
    target_id        VARCHAR(100) NOT NULL,
    details          TEXT NOT NULL DEFAULT '{}',
    ip_address       VARCHAR(50),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX admin_audit_logs_actor_idx ON admin_audit_logs(actor_id, created_at DESC);
CREATE INDEX admin_audit_logs_target_idx ON admin_audit_logs(target_type, target_id);
