-- ==============================================================================
-- Module 1 & 2 Extension: Seed Aerocity Demo Hospitals and Outbreak Zones
-- Target Database: Supabase PostgreSQL (schema: public)
-- All demo records marked with is_demo_data = TRUE
-- ==============================================================================

-- 1. Aerocity Demo Hospital (28.5562, 77.1000)
INSERT INTO healthcare_facilities (
    id, name, facility_type, latitude, longitude, location, address_line1, address_line2, 
    village_town, sub_district, district, state, pincode, contact_number, emergency_number, 
    is_operational, is_demo_data
) VALUES (
    'a1000000-0000-0000-0000-000000000001', 
    'Aerocity Demo Hospital', 
    'DISTRICT_HOSPITAL', 
    28.5562, 77.1000, 
    ST_SetSRID(ST_MakePoint(77.1000, 28.5562), 4326)::geography,
    'Asset Area 4, Aerocity (Demo)', 'Near IGI Airport Terminal 3', 
    'Aerocity', 'South West Delhi', 'New Delhi', 'Delhi', '110037', 
    '+91-11-26781001', '+91-11-26781002', TRUE, TRUE
) ON CONFLICT (id) DO NOTHING;

-- 2. Mahipalpur Demo CHC (28.5445, 77.1262)
INSERT INTO healthcare_facilities (
    id, name, facility_type, latitude, longitude, location, address_line1, address_line2, 
    village_town, sub_district, district, state, pincode, contact_number, emergency_number, 
    is_operational, is_demo_data
) VALUES (
    'a2000000-0000-0000-0000-000000000002', 
    'Mahipalpur Demo CHC', 
    'CHC', 
    28.5445, 77.1262, 
    ST_SetSRID(ST_MakePoint(77.1262, 28.5445), 4326)::geography,
    'Vasant Kunj Road, Mahipalpur (Demo)', 'Opposite Block Office', 
    'Mahipalpur', 'Vasant Vihar', 'New Delhi', 'Delhi', '110037', 
    '+91-11-26782001', '+91-11-26782002', TRUE, TRUE
) ON CONFLICT (id) DO NOTHING;

-- 3. Demo Patients for Aerocity (RED ZONE) and Mahipalpur (YELLOW ZONE)
INSERT INTO patients (
    id, full_name, gender, date_of_birth, age,
    contact_number, emergency_contact_name, emergency_contact_number,
    village_town, sub_district, district, state, pincode,
    blood_group, preferred_language, latitude, longitude,
    created_at, updated_at
) VALUES 
(
    'a3000000-0000-0000-0000-000000000001',
    'Rajesh Kumar (Aerocity Demo)',
    'MALE',
    '1985-06-15',
    41,
    '9811001001',
    'Sunita Kumar (Wife)',
    '9811001002',
    'Aerocity',
    'South West Delhi',
    'New Delhi',
    'Delhi',
    '110037',
    'B_POSITIVE',
    'Hindi',
    28.5562,
    77.1000,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a3000000-0000-0000-0000-000000000002',
    'Pooja Verma (Aerocity Demo)',
    'FEMALE',
    '1992-09-20',
    34,
    '9811002001',
    'Anil Verma (Brother)',
    '9811002002',
    'Aerocity',
    'South West Delhi',
    'New Delhi',
    'Delhi',
    '110037',
    'O_POSITIVE',
    'Hindi',
    28.5570,
    77.1010,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a3000000-0000-0000-0000-000000000003',
    'Vikas Sharma (Mahipalpur Demo)',
    'MALE',
    '1990-03-10',
    36,
    '9811003001',
    'Meena Sharma (Wife)',
    '9811003002',
    'Mahipalpur',
    'Vasant Vihar',
    'New Delhi',
    'Delhi',
    '110037',
    'A_POSITIVE',
    'Hindi',
    28.5445,
    77.1262,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 4. Demo Healthcare Cases for Aerocity (2 CRITICAL -> RED) and Mahipalpur (1 HIGH -> YELLOW)
INSERT INTO healthcare_cases (
    id, case_number, patient_id, status, priority, intake_channel,
    chief_complaint, notes, latitude, longitude, location,
    created_at, updated_at
) VALUES 
(
    'a4000000-0000-0000-0000-000000000001',
    'CASE-DEL-AERO-001',
    'a3000000-0000-0000-0000-000000000001',
    'TRIAGED',
    'CRITICAL',
    'FORM',
    'High fever 104F, persistent vomiting and severe abdominal pain for 4 days',
    'Demo outbreak case in Aerocity cluster',
    28.5562,
    77.1000,
    ST_SetSRID(ST_MakePoint(77.1000, 28.5562), 4326)::geography,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a4000000-0000-0000-0000-000000000002',
    'CASE-DEL-AERO-002',
    'a3000000-0000-0000-0000-000000000002',
    'TRIAGED',
    'CRITICAL',
    'FORM',
    'High fever, retro-orbital pain, severe joint pain for 3 days',
    'Demo outbreak case in Aerocity cluster',
    28.5570,
    77.1010,
    ST_SetSRID(ST_MakePoint(77.1010, 28.5570), 4326)::geography,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a4000000-0000-0000-0000-000000000003',
    'CASE-DEL-MAHI-001',
    'a3000000-0000-0000-0000-000000000003',
    'TRIAGED',
    'HIGH',
    'FORM',
    'Moderate fever and respiratory infection for 2 days',
    'Demo cluster case in Mahipalpur',
    28.5445,
    77.1262,
    ST_SetSRID(ST_MakePoint(77.1262, 28.5445), 4326)::geography,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
