-- Migration: V2__clean_demo_and_test_sos_events.sql
-- Description: Purge leftover synthetic, demo, and test SOS alerts so only genuine live alerts remain.

DELETE FROM sos_notifications 
WHERE sos_event_id IN (
    SELECT id FROM sos_events 
    WHERE message ILIKE '%test%' 
       OR message ILIKE '%demo%' 
       OR message ILIKE '%verification%' 
       OR message ILIKE '%anonymous%'
       OR patient_id = 'b1000000-0000-0000-0000-000000000001'
);

DELETE FROM sos_recipients 
WHERE sos_event_id IN (
    SELECT id FROM sos_events 
    WHERE message ILIKE '%test%' 
       OR message ILIKE '%demo%' 
       OR message ILIKE '%verification%' 
       OR message ILIKE '%anonymous%'
       OR patient_id = 'b1000000-0000-0000-0000-000000000001'
);

DELETE FROM sos_events 
WHERE message ILIKE '%test%' 
   OR message ILIKE '%demo%' 
   OR message ILIKE '%verification%' 
   OR message ILIKE '%anonymous%'
   OR patient_id = 'b1000000-0000-0000-0000-000000000001';
