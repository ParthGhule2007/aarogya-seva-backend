-- ==============================================================================
-- Module 1: Healthcare Facility & PostGIS Geospatial Schema
-- Target Database: Supabase PostgreSQL (PostGIS enabled)
-- ==============================================================================

-- Enable PostGIS spatial extension (standard in Supabase PostgreSQL)
CREATE EXTENSION IF NOT EXISTS postgis;

-- Master table for healthcare facilities
CREATE TABLE IF NOT EXISTS healthcare_facilities (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    facility_type VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    village_town VARCHAR(100) NOT NULL,
    sub_district VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    emergency_number VARCHAR(30),
    is_operational BOOLEAN NOT NULL DEFAULT TRUE,
    is_demo_data BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Trigger function to automatically maintain the PostGIS geography location from latitude/longitude
CREATE OR REPLACE FUNCTION update_healthcare_facility_location()
RETURNS TRIGGER AS $$
BEGIN
    NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_facility_location ON healthcare_facilities;
CREATE TRIGGER trg_update_facility_location
BEFORE INSERT OR UPDATE OF latitude, longitude ON healthcare_facilities
FOR EACH ROW
EXECUTE FUNCTION update_healthcare_facility_location();

-- PostGIS GiST Spatial Index for high-performance radius searches
CREATE INDEX IF NOT EXISTS idx_facilities_location_gist ON healthcare_facilities USING GIST (location);

-- Attribute indexes
CREATE INDEX IF NOT EXISTS idx_facilities_district_state ON healthcare_facilities(district, state);
CREATE INDEX IF NOT EXISTS idx_facilities_type ON healthcare_facilities(facility_type);
CREATE INDEX IF NOT EXISTS idx_facilities_operational ON healthcare_facilities(is_operational);
CREATE INDEX IF NOT EXISTS idx_facilities_demo_data ON healthcare_facilities(is_demo_data);

-- Master table for medical service capabilities offered by facilities
CREATE TABLE IF NOT EXISTS facility_service_offerings (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL,
    service_code VARCHAR(50) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_facility_offering_facility FOREIGN KEY (facility_id) 
        REFERENCES healthcare_facilities(id) ON DELETE CASCADE,
    CONSTRAINT uq_facility_service UNIQUE (facility_id, service_code)
);

CREATE INDEX IF NOT EXISTS idx_service_offerings_code ON facility_service_offerings(service_code, is_available);
