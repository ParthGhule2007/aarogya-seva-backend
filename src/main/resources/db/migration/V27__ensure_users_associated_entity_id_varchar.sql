-- Migration: V27__ensure_users_associated_entity_id_varchar.sql
-- Description: Ensure users.associated_entity_id is VARCHAR(100) to match JPA User entity

ALTER TABLE users ALTER COLUMN associated_entity_id TYPE VARCHAR(100) USING associated_entity_id::text;
