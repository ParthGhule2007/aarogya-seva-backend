-- Migration: V25__alter_users_associated_entity_id_varchar.sql
-- Description: Convert users.associated_entity_id from UUID to VARCHAR(100) to support non-UUID doctor IDs and facility/patient UUIDs

ALTER TABLE users ALTER COLUMN associated_entity_id TYPE VARCHAR(100) USING associated_entity_id::text;
