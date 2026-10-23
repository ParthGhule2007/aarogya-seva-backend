-- Migration: V19__convert_patient_vitals_to_double_precision.sql
-- Description: Convert height_cm and weight_kg to DOUBLE PRECISION to match Hibernate Double mapping

ALTER TABLE patients ALTER COLUMN height_cm TYPE DOUBLE PRECISION;
ALTER TABLE patients ALTER COLUMN weight_kg TYPE DOUBLE PRECISION;
