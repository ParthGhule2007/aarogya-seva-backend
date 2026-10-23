-- ==============================================================================
-- Fix case status and intake channel for aerocity cases
-- ==============================================================================

UPDATE healthcare_cases SET status = 'TRIAGED' WHERE status = 'IN_TRIAGE';
UPDATE healthcare_cases SET intake_channel = 'FORM' WHERE intake_channel = 'WALK_IN';
