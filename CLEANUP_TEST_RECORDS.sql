-- ==============================================================================
-- CLEANUP SCRIPT FOR TEST / DEMO RECORDS (Aarogya Seva)
-- DO NOT EXECUTE AUTOMATICALLY. Review carefully and run manually when needed.
-- ==============================================================================

BEGIN;

-- 1. Remove test consultation sessions created during E2E verification
DELETE FROM consultations 
WHERE user_id IN (
    SELECT id::text FROM users WHERE username LIKE '%.demo' OR username LIKE '%test%'
)
OR doctor_id IN (
    SELECT id FROM doctors WHERE name LIKE '%Demo%' OR name LIKE '%Test%'
);

-- 2. Remove test SOS emergency alerts
DELETE FROM sos_alerts 
WHERE patient_id IN (
    SELECT id::text FROM patients WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%'
)
OR patient_id IN (
    SELECT id::text FROM users WHERE username LIKE '%.demo'
);

-- 3. Remove test referrals & clinical symptoms
DELETE FROM referrals 
WHERE patient_id IN (
    SELECT id FROM patients WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%'
);

DELETE FROM case_symptoms 
WHERE case_id IN (
    SELECT id FROM cases WHERE patient_id IN (
        SELECT id FROM patients WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%'
    )
);

DELETE FROM cases 
WHERE patient_id IN (
    SELECT id FROM patients WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%'
);

-- 4. Remove test medication schedules and caregiver escalations
DELETE FROM caregiver_escalations 
WHERE patient_id IN (
    SELECT id FROM patients WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%'
);

DELETE FROM caregivers 
WHERE patient_id IN (
    SELECT id FROM patients WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%'
);

DELETE FROM medication_events 
WHERE patient_id IN (
    SELECT id FROM patients WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%'
);

DELETE FROM medications 
WHERE patient_id IN (
    SELECT id FROM patients WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%'
);

-- 5. Remove test doctor records created in doctor-service
DELETE FROM doctors 
WHERE name LIKE '%Demo%' OR email LIKE '%demo%' OR user_id IN (
    SELECT id::text FROM users WHERE username LIKE '%.demo'
);

-- 6. Remove test patient domain records
DELETE FROM patients 
WHERE full_name LIKE '%Demo%' OR phone_number LIKE '%99999%' OR user_id IN (
    SELECT id FROM users WHERE username LIKE '%.demo'
);

-- 7. Remove test auth user accounts
DELETE FROM users 
WHERE username LIKE '%.demo' OR username LIKE 'test.%' OR email LIKE '%@demo.com';

-- Verify counts before committing:
-- SELECT count(*) FROM users;
-- SELECT count(*) FROM patients;
-- SELECT count(*) FROM doctors;

COMMIT;
-- ROLLBACK; -- Uncomment if previewing before commit
