-- ==============================================================================
-- Migration: V2__upgrade_medicine_service.sql
-- Description: Backward-compatible schema upgrade for Aarogya Seva Medicine Service.
--              Adds user-defined medication names, food timing instructions,
--              flexible image storage, extended statuses, caregiver directory,
--              and permanent audit escalation logging without cascade deletion.
-- ==============================================================================

-- 1. Upgrade medicine.medications table
-- Add user-defined display name with safe backward-compatible default for existing V1 rows
ALTER TABLE medicine.medications 
    ADD COLUMN IF NOT EXISTS name VARCHAR(255) DEFAULT 'Prescribed Medicine';

UPDATE medicine.medications SET name = 'Prescribed Medicine' WHERE name IS NULL;
ALTER TABLE medicine.medications ALTER COLUMN name SET NOT NULL;

-- Add optional extracted name from packaging or prescription label
ALTER TABLE medicine.medications 
    ADD COLUMN IF NOT EXISTS extracted_name VARCHAR(255) NULL;

-- Relax image_key constraint to support Mode C (Name-only medicines without photo)
ALTER TABLE medicine.medications ALTER COLUMN image_key DROP NOT NULL;

-- Add structured food timing instruction
ALTER TABLE medicine.medications 
    ADD COLUMN IF NOT EXISTS food_instruction VARCHAR(50) NOT NULL DEFAULT 'NONE';

-- Safely expand status check constraint
ALTER TABLE medicine.medications 
    DROP CONSTRAINT IF EXISTS chk_medication_status;

ALTER TABLE medicine.medications 
    ADD CONSTRAINT chk_medication_status 
    CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'DISCONTINUED', 'DEACTIVATED'));

-- Add food instruction check constraint
ALTER TABLE medicine.medications 
    DROP CONSTRAINT IF EXISTS chk_food_instruction;

ALTER TABLE medicine.medications 
    ADD CONSTRAINT chk_food_instruction 
    CHECK (food_instruction IN ('BEFORE_FOOD', 'AFTER_FOOD', 'WITH_FOOD', 'EMPTY_STOMACH', 'NONE'));

-- 2. Create Caregivers Table (Isolated within medicine schema)
CREATE TABLE IF NOT EXISTS medicine.caregivers (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    phone_number VARCHAR(30) NULL,
    email VARCHAR(150) NULL,
    preferred_channel VARCHAR(30) NOT NULL DEFAULT 'WHATSAPP',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_caregiver_channel CHECK (preferred_channel IN ('SMS', 'WHATSAPP', 'EMAIL', 'PUSH'))
);

CREATE INDEX IF NOT EXISTS idx_caregivers_patient_id ON medicine.caregivers(patient_id);

-- 3. Create Immutable Caregiver Escalation Audit Log Table (NO CASCADE DELETE)
CREATE TABLE IF NOT EXISTS medicine.caregiver_escalation_logs (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    medication_id UUID NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    streak_start_dose_id UUID NOT NULL,
    triggering_dose_id UUID NOT NULL,
    consecutive_missed_count INTEGER NOT NULL,
    channel VARCHAR(30) NOT NULL,
    recipient VARCHAR(150) NOT NULL,
    delivery_status VARCHAR(50) NOT NULL,
    message_content VARCHAR(1000) NOT NULL,
    provider_reference VARCHAR(255) NULL,
    delivery_attempted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_delivery_status CHECK (delivery_status IN (
        'DETECTED', 'ATTEMPTED', 'SIMULATED_DISPATCH', 
        'GATEWAY_ACCEPTED', 'CARRIER_DELIVERED', 'GATEWAY_FAILED'
    ))
);

-- Deduplication unique index ensuring exactly ONE alert per missed streak
CREATE UNIQUE INDEX IF NOT EXISTS uq_escalation_streak 
ON medicine.caregiver_escalation_logs(medication_id, streak_start_dose_id);

CREATE INDEX IF NOT EXISTS idx_escalation_patient ON medicine.caregiver_escalation_logs(patient_id);
