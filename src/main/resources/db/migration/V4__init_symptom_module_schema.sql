-- ==============================================================================
-- Module 3: Symptom Intake & Management Schema
-- Target Database: Supabase PostgreSQL
-- ==============================================================================

CREATE TABLE IF NOT EXISTS symptoms (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    symptom_name VARCHAR(150) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MILD',
    duration_days INTEGER NOT NULL DEFAULT 1,
    description TEXT,
    body_site VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_symptoms_case FOREIGN KEY (case_id)
        REFERENCES healthcare_cases(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_symptoms_case_id ON symptoms(case_id);
CREATE INDEX IF NOT EXISTS idx_symptoms_name ON symptoms(symptom_name);
CREATE INDEX IF NOT EXISTS idx_symptoms_severity ON symptoms(severity);
CREATE INDEX IF NOT EXISTS idx_symptoms_created_at ON symptoms(created_at);
