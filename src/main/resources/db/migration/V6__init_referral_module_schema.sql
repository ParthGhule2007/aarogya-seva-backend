-- ==============================================================================
-- Module 5: Referral Request & Care Navigation Schema
-- Target Database: Supabase PostgreSQL
-- ==============================================================================

CREATE TABLE IF NOT EXISTS referral_requests (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    facility_id UUID NOT NULL,
    ai_prediction_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    patient_message TEXT,
    facility_response TEXT,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP WITH TIME ZONE,
    accepted_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_referral_case FOREIGN KEY (case_id)
        REFERENCES healthcare_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_referral_patient FOREIGN KEY (patient_id)
        REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_referral_facility FOREIGN KEY (facility_id)
        REFERENCES healthcare_facilities(id) ON DELETE CASCADE,
    CONSTRAINT fk_referral_ai_prediction FOREIGN KEY (ai_prediction_id)
        REFERENCES ai_predictions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_referral_case_id ON referral_requests(case_id);
CREATE INDEX IF NOT EXISTS idx_referral_patient_id ON referral_requests(patient_id);
CREATE INDEX IF NOT EXISTS idx_referral_facility_id ON referral_requests(facility_id);
CREATE INDEX IF NOT EXISTS idx_referral_status ON referral_requests(status);
CREATE INDEX IF NOT EXISTS idx_referral_requested_at ON referral_requests(requested_at);
CREATE INDEX IF NOT EXISTS idx_referral_created_at ON referral_requests(created_at);
