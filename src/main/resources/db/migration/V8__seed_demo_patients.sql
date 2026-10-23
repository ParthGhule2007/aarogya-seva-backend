-- ==============================================================================
-- Module 2 Extension: Seed Demo Patients for Screening & Triage Flow
-- Target Database: Supabase PostgreSQL (schema: public)
-- ==============================================================================

INSERT INTO patients (
    id, full_name, gender, date_of_birth, age,
    contact_number, emergency_contact_name, emergency_contact_number,
    village_town, sub_district, district, state, pincode,
    blood_group, preferred_language, latitude, longitude,
    created_at, updated_at
) VALUES 
(
    'b1000000-0000-0000-0000-000000000001',
    'Ramesh Patil',
    'MALE',
    '1978-04-12',
    48,
    '9822012345',
    'Suresh Patil (Son)',
    '9822099999',
    'Palasdeo',
    'Indapur',
    'Pune',
    'Maharashtra',
    '413132',
    'O_POSITIVE',
    'Marathi',
    18.2562,
    74.9048,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'b2000000-0000-0000-0000-000000000002',
    'Shantabai Shinde',
    'FEMALE',
    '1964-08-20',
    62,
    '9822054321',
    'Anil Shinde (Son)',
    '9822088888',
    'Shelgaon',
    'Indapur',
    'Pune',
    'Maharashtra',
    '413130',
    'B_POSITIVE',
    'Marathi',
    18.2890,
    74.9520,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'b3000000-0000-0000-0000-000000000003',
    'Ganesh Jadhav',
    'MALE',
    '1991-01-10',
    35,
    '9822077889',
    'Kavita Jadhav (Wife)',
    '9822077777',
    'Bhigwan',
    'Indapur',
    'Pune',
    'Maharashtra',
    '413130',
    'A_POSITIVE',
    'Hindi',
    18.2917,
    74.8083,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
