-- ==============================================================================
-- DEMO / DEVELOPMENT DATA — NOT VERIFIED REAL-WORLD HEALTHCARE DATA
--
-- CAUTION:
-- These records are synthetic/placeholder facilities for development and local testing only.
-- Do NOT present seeded facilities as verified hospitals, clinics, or healthcare providers.
-- Production healthcare facility data must be verified through official channels before use.
--
-- Identification:
-- All demo records are marked with: is_demo_data = TRUE
--
-- Replacement:
-- When verified facility data is onboarded, demo data can be cleanly cleared using:
-- DELETE FROM healthcare_facilities WHERE is_demo_data = TRUE;
-- ==============================================================================

-- Facility 1 (DEMO): Rampur Primary Health Centre (PHC)
INSERT INTO healthcare_facilities (
    id, name, facility_type, latitude, longitude, location, address_line1, address_line2, 
    village_town, sub_district, district, state, pincode, contact_number, emergency_number, 
    is_operational, is_demo_data
) VALUES (
    '11111111-1111-1111-1111-111111111111', 
    'Demo Rampur Primary Health Centre', 
    'PHC', 
    26.8467, 80.9462, 
    ST_SetSRID(ST_MakePoint(80.9462, 26.8467), 4326)::geography,
    'Main Village Road (Demo)', 'Near Panchayat Bhavan', 
    'Rampur', 'Mohanlalganj', 'Lucknow', 'Uttar Pradesh', '226301', 
    '+91-522-298101', '+91-522-298102', TRUE, TRUE
) ON CONFLICT (id) DO NOTHING;

-- Facility 2 (DEMO): Mohanlalganj Community Health Centre (CHC)
INSERT INTO healthcare_facilities (
    id, name, facility_type, latitude, longitude, location, address_line1, address_line2, 
    village_town, sub_district, district, state, pincode, contact_number, emergency_number, 
    is_operational, is_demo_data
) VALUES (
    '22222222-2222-2222-2222-222222222222', 
    'Demo Mohanlalganj Community Health Centre', 
    'CHC', 
    26.6811, 80.9984, 
    ST_SetSRID(ST_MakePoint(80.9984, 26.6811), 4326)::geography,
    'Hospital Road, NH-24B (Demo)', 'Opposite Block Development Office', 
    'Mohanlalganj', 'Mohanlalganj', 'Lucknow', 'Uttar Pradesh', '226301', 
    '+91-522-298201', '+91-522-298202', TRUE, TRUE
) ON CONFLICT (id) DO NOTHING;

-- Facility 3 (DEMO): BKT Sub-District Hospital
INSERT INTO healthcare_facilities (
    id, name, facility_type, latitude, longitude, location, address_line1, address_line2, 
    village_town, sub_district, district, state, pincode, contact_number, emergency_number, 
    is_operational, is_demo_data
) VALUES (
    '33333333-3333-3333-3333-333333333333', 
    'Demo BKT Sub-District Hospital', 
    'SUB_DISTRICT_HOSPITAL', 
    27.0090, 80.9230, 
    ST_SetSRID(ST_MakePoint(80.9230, 27.0090), 4326)::geography,
    'Sitapur Highway (Demo)', 'Sector A', 
    'Bakshi Ka Talab', 'Bakshi Ka Talab', 'Lucknow', 'Uttar Pradesh', '226201', 
    '+91-522-298301', '+91-522-298302', TRUE, TRUE
) ON CONFLICT (id) DO NOTHING;

-- Facility 4 (DEMO): Lucknow District Civil Hospital
INSERT INTO healthcare_facilities (
    id, name, facility_type, latitude, longitude, location, address_line1, address_line2, 
    village_town, sub_district, district, state, pincode, contact_number, emergency_number, 
    is_operational, is_demo_data
) VALUES (
    '44444444-4444-4444-4444-444444444444', 
    'Demo Civil Hospital', 
    'DISTRICT_HOSPITAL', 
    26.8524, 80.9413, 
    ST_SetSRID(ST_MakePoint(80.9413, 26.8524), 4326)::geography,
    'Park Road, Hazratganj (Demo)', 'Near Vidhan Sabha', 
    'Lucknow', 'Lucknow', 'Lucknow', 'Uttar Pradesh', '226001', 
    '+91-522-2239001', '+91-522-2239002', TRUE, TRUE
) ON CONFLICT (id) DO NOTHING;

-- Demo Services for Facility 1 (Demo PHC Rampur)
INSERT INTO facility_service_offerings (id, facility_id, service_code, is_available) VALUES
    ('a1111111-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'GENERAL_OPD', TRUE),
    ('a1111111-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'MATERNAL_HEALTH', TRUE),
    ('a1111111-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'DIAGNOSTIC_LAB', TRUE)
ON CONFLICT (facility_id, service_code) DO NOTHING;

-- Demo Services for Facility 2 (Demo CHC Mohanlalganj)
INSERT INTO facility_service_offerings (id, facility_id, service_code, is_available) VALUES
    ('a2222222-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'GENERAL_OPD', TRUE),
    ('a2222222-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'EMERGENCY', TRUE),
    ('a2222222-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', 'MATERNAL_HEALTH', TRUE),
    ('a2222222-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222', 'PEDIATRIC_CARE', TRUE),
    ('a2222222-0000-0000-0000-000000000005', '22222222-2222-2222-2222-222222222222', 'DIAGNOSTIC_LAB', TRUE)
ON CONFLICT (facility_id, service_code) DO NOTHING;

-- Demo Services for Facility 3 (Demo Sub-District Hospital BKT)
INSERT INTO facility_service_offerings (id, facility_id, service_code, is_available) VALUES
    ('a3333333-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 'GENERAL_OPD', TRUE),
    ('a3333333-0000-0000-0000-000000000002', '33333333-3333-3333-3333-333333333333', 'EMERGENCY', TRUE),
    ('a3333333-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333', 'MATERNAL_HEALTH', TRUE),
    ('a3333333-0000-0000-0000-000000000004', '33333333-3333-3333-3333-333333333333', 'SURGICAL_CARE', TRUE),
    ('a3333333-0000-0000-0000-000000000005', '33333333-3333-3333-3333-333333333333', 'DIAGNOSTIC_LAB', TRUE)
ON CONFLICT (facility_id, service_code) DO NOTHING;

-- Demo Services for Facility 4 (Demo District Hospital)
INSERT INTO facility_service_offerings (id, facility_id, service_code, is_available) VALUES
    ('a4444444-0000-0000-0000-000000000001', '44444444-4444-4444-4444-444444444444', 'GENERAL_OPD', TRUE),
    ('a4444444-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 'EMERGENCY', TRUE),
    ('a4444444-0000-0000-0000-000000000003', '44444444-4444-4444-4444-444444444444', 'MATERNAL_HEALTH', TRUE),
    ('a4444444-0000-0000-0000-000000000004', '44444444-4444-4444-4444-444444444444', 'PEDIATRIC_CARE', TRUE),
    ('a4444444-0000-0000-0000-000000000005', '44444444-4444-4444-4444-444444444444', 'INFECTIOUS_DISEASE', TRUE),
    ('a4444444-0000-0000-0000-000000000006', '44444444-4444-4444-4444-444444444444', 'SURGICAL_CARE', TRUE),
    ('a4444444-0000-0000-0000-000000000007', '44444444-4444-4444-4444-444444444444', 'DIAGNOSTIC_LAB', TRUE)
ON CONFLICT (facility_id, service_code) DO NOTHING;
