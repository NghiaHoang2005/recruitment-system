-- Migration: Add admin audit logs
-- Date: 2026-06-05
-- Purpose: Persist admin moderation actions for Phase 8 audit logging

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id UUID PRIMARY KEY,
    admin_user_id UUID REFERENCES users(id),
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at
    ON admin_audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin_user_id
    ON admin_audit_logs (admin_user_id);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_target
    ON admin_audit_logs (target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action
    ON admin_audit_logs (action);
