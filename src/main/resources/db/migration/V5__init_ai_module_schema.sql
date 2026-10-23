-- ==============================================================================
-- Module 4: AI Intelligence & Clinical Decision Support Schema
-- Target Database: Supabase PostgreSQL
-- ==============================================================================

CREATE TABLE IF NOT EXISTS ai_predictions (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    risk_score DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(30) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    urgency_level VARCHAR(30) NOT NULL,
    urgency_reason TEXT,
    explanation TEXT,
    possible_diseases JSONB NOT NULL DEFAULT '[]'::jsonb,
    precautions JSONB NOT NULL DEFAULT '[]'::jsonb,
    recommended_services JSONB NOT NULL DEFAULT '[]'::jsonb,
    required_facility_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_prediction_case FOREIGN KEY (case_id)
        REFERENCES healthcare_cases(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_predictions_case_id ON ai_predictions(case_id);
CREATE INDEX IF NOT EXISTS idx_ai_predictions_created_at ON ai_predictions(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_predictions_risk_level ON ai_predictions(risk_level);
CREATE INDEX IF NOT EXISTS idx_ai_predictions_urgency_level ON ai_predictions(urgency_level);
