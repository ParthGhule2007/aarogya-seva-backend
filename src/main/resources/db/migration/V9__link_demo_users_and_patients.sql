-- ==============================================================================
-- Module 7 Extension: Link Demo Users to Demo Patients
-- Target Database: Supabase PostgreSQL (schema: public)
-- ==============================================================================

UPDATE users
SET associated_entity_id = 'b1000000-0000-0000-0000-000000000001'
WHERE username = 'patient.ramesh' AND (associated_entity_id IS NULL OR associated_entity_id != 'b1000000-0000-0000-0000-000000000001');
