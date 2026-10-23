-- ==============================================================================
-- Module 2: Patient & Healthcare Case Management Schema
-- Target Database: Supabase PostgreSQL (PostGIS enabled)
-- ==============================================================================

-- 1. Patients Table
CREATE TABLE IF NOT EXISTS patients (
    id UUID PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    date_of_birth DATE,
    age INTEGER,
    contact_number VARCHAR(30) NOT NULL,
    emergency_contact_name VARCHAR(150),
    emergency_contact_number VARCHAR(30),
    village_town VARCHAR(100) NOT NULL,
    sub_district VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    blood_group VARCHAR(10),
    preferred_language VARCHAR(50) NOT NULL DEFAULT 'Hindi',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patients_contact ON patients(contact_number);
CREATE INDEX IF NOT EXISTS idx_patients_district_state ON patients(district, state);

-- 2. Healthcare Cases Table
CREATE TABLE IF NOT EXISTS healthcare_cases (
    id UUID PRIMARY KEY,
    case_number VARCHAR(50) NOT NULL UNIQUE,
    patient_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    priority VARCHAR(20) NOT NULL DEFAULT 'LOW',
    intake_channel VARCHAR(30) NOT NULL,
    chief_complaint VARCHAR(255),
    notes TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_case_patient FOREIGN KEY (patient_id) 
        REFERENCES patients(id) ON DELETE CASCADE
);

-- Trigger to maintain location geography for healthcare cases
CREATE OR REPLACE FUNCTION update_healthcare_case_location()
RETURNS TRIGGER AS $$
BEGIN
    NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_case_location ON healthcare_cases;
CREATE TRIGGER trg_update_case_location
BEFORE INSERT OR UPDATE OF latitude, longitude ON healthcare_cases
FOR EACH ROW
EXECUTE FUNCTION update_healthcare_case_location();

CREATE INDEX IF NOT EXISTS idx_cases_patient ON healthcare_cases(patient_id);
CREATE INDEX IF NOT EXISTS idx_cases_status ON healthcare_cases(status);
CREATE INDEX IF NOT EXISTS idx_cases_number ON healthcare_cases(case_number);
CREATE INDEX IF NOT EXISTS idx_cases_created ON healthcare_cases(created_at);
CREATE INDEX IF NOT EXISTS idx_cases_location_gist ON healthcare_cases USING GIST (location);

-- 3. Case Symptoms Table
CREATE TABLE IF NOT EXISTS case_symptoms (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    symptom_name VARCHAR(150) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MILD',
    duration_days INTEGER NOT NULL DEFAULT 1,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_symptom_case FOREIGN KEY (case_id) 
        REFERENCES healthcare_cases(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_symptoms_case ON case_symptoms(case_id);
CREATE INDEX IF NOT EXISTS idx_symptoms_name ON case_symptoms(symptom_name);
