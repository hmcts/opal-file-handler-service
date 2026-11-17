BEGIN;
DELETE FROM interface_files WHERE interface_job_id IN
 (SELECT interface_job_id FROM interface_jobs WHERE business_unit_id = 7243);
DELETE FROM interface_jobs WHERE business_unit_id = 7243;
DELETE FROM business_units WHERE business_unit_id = 7243;
COMMIT;
