-- ==============================================================================
-- Calling Service: Standalone Call Session & Follow-up Profile Schema
-- Database / Schema: calling
-- ==============================================================================

CREATE TABLE IF NOT EXISTS follow_up_profiles (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(30) NOT NULL UNIQUE,
    chosen_name VARCHAR(100),
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
    concern_summary VARCHAR(1000),
    consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    opted_out BOOLEAN NOT NULL DEFAULT FALSE,
    first_call_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    opted_out_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_follow_up_phone
    ON follow_up_profiles(phone_number);

CREATE INDEX IF NOT EXISTS idx_follow_up_opted_out
    ON follow_up_profiles(opted_out);

CREATE TABLE IF NOT EXISTS call_sessions (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(30) NOT NULL,
    call_type VARCHAR(30) NOT NULL,
    language VARCHAR(10) NOT NULL,
    twilio_call_sid VARCHAR(100),
    ultravox_call_id VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    duration_seconds INTEGER,
    error_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_call_sessions_phone
    ON call_sessions(phone_number);

CREATE INDEX IF NOT EXISTS idx_call_sessions_twilio_sid
    ON call_sessions(twilio_call_sid);

CREATE INDEX IF NOT EXISTS idx_call_sessions_status
    ON call_sessions(status);

CREATE INDEX IF NOT EXISTS idx_call_sessions_created_at
    ON call_sessions(created_at);
