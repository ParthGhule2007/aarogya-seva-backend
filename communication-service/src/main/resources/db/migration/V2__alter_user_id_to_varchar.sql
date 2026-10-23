-- =========================================================================
-- Migration: Change communication_sessions.user_id from BIGINT to VARCHAR(64)
-- Preserves existing numeric data (e.g. 201 -> '201') without data loss.
-- Compatible with PostgreSQL.
-- =========================================================================

CREATE SCHEMA IF NOT EXISTS communication;
SET search_path TO communication;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_schema = 'communication'
          AND table_name = 'communication_sessions' 
          AND column_name = 'user_id' 
          AND data_type = 'bigint'
    ) THEN
        ALTER TABLE communication.communication_sessions ALTER COLUMN user_id TYPE VARCHAR(64) USING user_id::VARCHAR;
    END IF;
END $$;
