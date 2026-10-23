-- V17: Support for Google OAuth2 / Google Sign-In
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(50) DEFAULT 'LOCAL' NOT NULL,
    ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255) UNIQUE;

-- Allow NULL password_hash for federated Google OAuth users
ALTER TABLE users 
    ALTER COLUMN password_hash DROP NOT NULL;
