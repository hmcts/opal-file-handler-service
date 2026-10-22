/**
* OPAL Program
*
* MODULE      : alter_interface_files_for_ei4.sql
*
* DESCRIPTION : Add EI4 upload metadata, Variant Banking source support, and search indexes
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 01/09/2026    TMc            1.0         PO-8692 - Alter INTERFACE_FILES for Interface File Viewer and Variant Banking
* 25/09/2026    TT             1.1         PO-8692 - Added created_by as NOT NULL, temporarily defaulting to -1 (opal-system-user)
*                                                    to populate existing NLE records; the default is then removed for future inserts.
*
**/

ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'VARIANT_BANKING';

ALTER TABLE interface_files
    ADD COLUMN created_by BIGINT NOT NULL DEFAULT -1;

ALTER TABLE interface_files
    ALTER COLUMN created_by DROP DEFAULT;

COMMENT ON COLUMN interface_files.created_by IS
    'ID of the user who manually uploaded the interface file or the system user ID for external banking interface files';

CREATE INDEX if_bu_code_gin_idx
    ON interface_files USING gin (business_unit_code);

CREATE INDEX if_domain_idx
    ON interface_files (opal_domain);

CREATE INDEX if_source_idx
    ON interface_files (source);

CREATE INDEX if_type_idx
    ON interface_files (type);