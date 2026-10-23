-- Migration: V18__add_patient_vitals_and_history.sql
-- Description: Add optional height, weight, and medical history columns to patients table for registration

ALTER TABLE patients ADD COLUMN IF NOT EXISTS height_cm NUMERIC(5,1);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS weight_kg NUMERIC(5,1);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS medical_history TEXT;
