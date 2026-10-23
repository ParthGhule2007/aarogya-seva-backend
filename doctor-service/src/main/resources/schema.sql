-- =========================================================================
-- PostgreSQL DDL for Doctor Service
-- Schema: doctor
-- =========================================================================

CREATE SCHEMA IF NOT EXISTS doctor;
SET search_path TO doctor;

-- Create doctors table in doctor schema
CREATE TABLE IF NOT EXISTS doctor.doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    license_number VARCHAR(50) NOT NULL,
    experience_years INTEGER,
    qualification VARCHAR(100),
    hospital_name VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    availability_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT uq_doctor_license_number UNIQUE (license_number),
    CONSTRAINT chk_doctor_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_doctor_availability_status CHECK (availability_status IN ('AVAILABLE', 'BUSY', 'OFFLINE')),
    CONSTRAINT chk_experience_years CHECK (experience_years >= 0)
);

-- Idempotent column type migration if existing table had bigint user_id
ALTER TABLE IF EXISTS doctor.doctors ALTER COLUMN user_id TYPE VARCHAR(100);


-- Performance and Query Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_doctor_user_id ON doctor.doctors (user_id);
CREATE INDEX IF NOT EXISTS idx_doctor_status_avail ON doctor.doctors (status, availability_status);
CREATE INDEX IF NOT EXISTS idx_doctor_specialization ON doctor.doctors (specialization);

