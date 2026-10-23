-- ==============================================================================
-- Migration: V1__init_sos_schema.sql
-- Description: Independent database schema for Aarogya Seva SOS Emergency Service
-- ==============================================================================

-- 1. SOS Events Table
CREATE TABLE IF NOT EXISTS sos_events (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    targets VARCHAR(100) NOT NULL,
    facility_id UUID NULL,
    message VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PROCESSING',
    idempotency_key VARCHAR(128) NULL,
    triggered_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ NULL,
    CONSTRAINT chk_sos_status CHECK (status IN ('TRIGGERED', 'PROCESSING', 'NOTIFICATIONS_SENT', 'PARTIALLY_SENT', 'FAILED', 'RESOLVED'))
);

CREATE INDEX IF NOT EXISTS idx_sos_events_patient_id ON sos_events(patient_id);
CREATE INDEX IF NOT EXISTS idx_sos_events_status ON sos_events(status);
CREATE INDEX IF NOT EXISTS idx_sos_events_triggered_at ON sos_events(triggered_at DESC);
CREATE INDEX IF NOT EXISTS idx_sos_events_idempotency_key ON sos_events(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 2. SOS Recipients Table
CREATE TABLE IF NOT EXISTS sos_recipients (
    id UUID PRIMARY KEY,
    sos_event_id UUID NOT NULL REFERENCES sos_events(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL,
    recipient_type VARCHAR(30) NOT NULL,
    distance_km NUMERIC(8, 2) NULL,
    notification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified_at TIMESTAMPTZ NULL,
    CONSTRAINT chk_recipient_type CHECK (recipient_type IN ('CAREGIVER', 'FACILITY', 'NEARBY_PEOPLE')),
    CONSTRAINT chk_recipient_notification_status CHECK (notification_status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_sos_recipients_event_id ON sos_recipients(sos_event_id);
CREATE INDEX IF NOT EXISTS idx_sos_recipients_recipient_id ON sos_recipients(recipient_id);
CREATE INDEX IF NOT EXISTS idx_sos_recipients_type ON sos_recipients(recipient_type);
CREATE INDEX IF NOT EXISTS idx_sos_recipients_status ON sos_recipients(notification_status);

-- 3. SOS Notifications Table (Audit of FCM dispatch)
CREATE TABLE IF NOT EXISTS sos_notifications (
    id UUID PRIMARY KEY,
    sos_event_id UUID NOT NULL REFERENCES sos_events(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'FCM',
    fcm_message_id VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT NULL,
    sent_at TIMESTAMPTZ NULL,
    delivered_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_sos_notifications_event_id ON sos_notifications(sos_event_id);
CREATE INDEX IF NOT EXISTS idx_sos_notifications_recipient_id ON sos_notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_sos_notifications_status ON sos_notifications(status);

-- 4. Device Tokens Table (FCM Tokens & Opt-In status)
CREATE TABLE IF NOT EXISTS sos_device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    user_type VARCHAR(30) NOT NULL,
    fcm_token VARCHAR(512) NOT NULL,
    emergency_alert_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_latitude NUMERIC(10, 7) NULL,
    last_longitude NUMERIC(10, 7) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_device_user_token UNIQUE (user_id, fcm_token),
    CONSTRAINT chk_device_user_type CHECK (user_type IN ('PATIENT', 'CAREGIVER', 'ASHA', 'FACILITY', 'DOCTOR'))
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user_id ON sos_device_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_device_tokens_token ON sos_device_tokens(fcm_token);
CREATE INDEX IF NOT EXISTS idx_device_tokens_opt_in_active ON sos_device_tokens(emergency_alert_opt_in, active) WHERE active = TRUE;
