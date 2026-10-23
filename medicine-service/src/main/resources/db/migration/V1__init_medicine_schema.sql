-- ==============================================================================
-- Migration: V1__init_medicine_schema.sql
-- Description: Independent database schema for Aarogya Seva Medicine Reminder Service
-- ==============================================================================

CREATE SCHEMA IF NOT EXISTS medicine;

-- 1. Medications Table (Patient medication reminder configuration)
CREATE TABLE IF NOT EXISTS medicine.medications (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    image_key VARCHAR(512) NOT NULL,
    dosage_amount NUMERIC(8, 2) NOT NULL,
    dosage_unit VARCHAR(50) NOT NULL,
    times_per_day INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    instructions VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_medication_status CHECK (status IN ('ACTIVE', 'PAUSED', 'DEACTIVATED')),
    CONSTRAINT chk_dosage_unit CHECK (dosage_unit IN ('TABLET', 'CAPSULE', 'SPOON', 'ML', 'DROP', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_medications_patient_id ON medicine.medications(patient_id);
CREATE INDEX IF NOT EXISTS idx_medications_status ON medicine.medications(status);
CREATE INDEX IF NOT EXISTS idx_medications_dates ON medicine.medications(start_date, end_date);

-- 2. Medication Reminder Times Table (Exact reminder times per day)
CREATE TABLE IF NOT EXISTS medicine.medication_reminder_times (
    id UUID PRIMARY KEY,
    medication_id UUID NOT NULL REFERENCES medicine.medications(id) ON DELETE CASCADE,
    reminder_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_med_reminder_time UNIQUE (medication_id, reminder_time)
);

CREATE INDEX IF NOT EXISTS idx_reminder_times_med_id ON medicine.medication_reminder_times(medication_id);

-- 3. Dose Events Table (Scheduled doses and status tracking)
CREATE TABLE IF NOT EXISTS medicine.dose_events (
    id UUID PRIMARY KEY,
    medication_id UUID NOT NULL REFERENCES medicine.medications(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL,
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    taken_at TIMESTAMPTZ NULL,
    missed_at TIMESTAMPTZ NULL,
    notification_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    notification_time TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dose_status CHECK (status IN ('PENDING', 'TAKEN', 'MISSED', 'SKIPPED')),
    CONSTRAINT uq_medication_dose UNIQUE (medication_id, scheduled_date, scheduled_time)
);

CREATE INDEX IF NOT EXISTS idx_dose_events_patient_date ON medicine.dose_events(patient_id, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_dose_events_medication_id ON medicine.dose_events(medication_id);
CREATE INDEX IF NOT EXISTS idx_dose_events_status ON medicine.dose_events(status);
CREATE INDEX IF NOT EXISTS idx_dose_events_scheduled ON medicine.dose_events(scheduled_date, scheduled_time, status);
