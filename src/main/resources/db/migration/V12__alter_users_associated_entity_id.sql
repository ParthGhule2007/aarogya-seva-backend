-- ==============================================================================
-- Module 7 Extension: Alter associated_entity_id to VARCHAR(100)
-- Target Database: Supabase PostgreSQL (schema: public)
-- ==============================================================================

ALTER TABLE users ALTER COLUMN associated_entity_id TYPE VARCHAR(100) USING associated_entity_id::text;

-- Properly link seeded demo users if unlinked
UPDATE users SET associated_entity_id = '1' WHERE username = 'doctor.ananya' AND (associated_entity_id IS NULL OR associated_entity_id = '');
UPDATE users SET associated_entity_id = 'f1000000-0000-0000-0000-000000000001' WHERE username = 'admin' AND (associated_entity_id IS NULL OR associated_entity_id = '');
UPDATE users SET associated_entity_id = 'b1000000-0000-0000-0000-000000000001' WHERE username = 'caregiver.suresh' AND (associated_entity_id IS NULL OR associated_entity_id = '');
