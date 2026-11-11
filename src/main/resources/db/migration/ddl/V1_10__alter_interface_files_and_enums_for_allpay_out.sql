/**
* OPAL Program
*
* MODULE      : alter_interface_files_and_enums_for_allpay_out.sql
*
* DESCRIPTION : Add column domain_interface_job_id column to interface_files and add values to Enums for Allpay Out
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    --------------------------------------------------------
* 11/11/2026    TMc            1.0         PO-11164 - Alter INTERFACE_FILES table and ENUMS for EI7
*
**/

ALTER TYPE t_type_enum
    ADD VALUE IF NOT EXISTS 'TRANSFORMED';

ALTER TYPE t_interface_enum
    ADD VALUE IF NOT EXISTS 'ALLPAY_OUT';

ALTER TABLE interface_files
    ADD COLUMN domain_interface_job_id BIGINT;

COMMENT ON COLUMN interface_files.domain_interface_job_id IS 'Primary key of the related Allpay Out interface job in the domains database';
