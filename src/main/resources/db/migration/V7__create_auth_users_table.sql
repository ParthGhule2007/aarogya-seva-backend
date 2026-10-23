-- ==============================================================================
-- Module 6: Authentication & User Accounts Schema
-- Target Database: Supabase PostgreSQL (schema: public)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(30) UNIQUE,
    email VARCHAR(150) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'PATIENT',
    associated_entity_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Seed Demo Accounts for Development & Testing (Password for all: Password@123)
-- BCrypt hash: $2a$10$usFlz3RKUbzMrGkaDD76iO0msO5crUTZJs.KX2EsciVadQmMVSN6e
INSERT INTO users (id, username, phone_number, email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'patient.ramesh', '9876543210', 'ramesh.patil@ruralhealth.org', '$2a$10$usFlz3RKUbzMrGkaDD76iO0msO5crUTZJs.KX2EsciVadQmMVSN6e', 'Ramesh Patil', 'PATIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'caregiver.suresh', '9876543211', 'suresh.patil@ruralhealth.org', '$2a$10$usFlz3RKUbzMrGkaDD76iO0msO5crUTZJs.KX2EsciVadQmMVSN6e', 'Suresh Patil', 'CAREGIVER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333333', 'asha.sunita', '9876543212', 'sunita.kamble@ruralhealth.org', '$2a$10$usFlz3RKUbzMrGkaDD76iO0msO5crUTZJs.KX2EsciVadQmMVSN6e', 'Sunita Kamble (ASHA)', 'ASHA', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('44444444-4444-4444-4444-444444444444', 'doctor.ananya', '9876543213', 'dr.ananya@hospital.com', '$2a$10$usFlz3RKUbzMrGkaDD76iO0msO5crUTZJs.KX2EsciVadQmMVSN6e', 'Dr. Ananya Rao', 'DOCTOR', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('55555555-5555-5555-5555-555555555555', 'admin', '9876543214', 'admin@aarogyaseva.org', '$2a$10$usFlz3RKUbzMrGkaDD76iO0msO5crUTZJs.KX2EsciVadQmMVSN6e', 'System Administrator', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;
