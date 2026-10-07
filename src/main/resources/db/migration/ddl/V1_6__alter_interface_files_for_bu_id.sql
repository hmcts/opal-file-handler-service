/**
* OPAL Program
*
* MODULE      : alter_interface_files_for_bu_id.sql
*
* DESCRIPTION : Alter table interface_files
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ----------------------------------------------------------------------------
* 06/10/2026    TT          1.0         PO-10807 - Drop column business_unit_code and its index.
*                                                  Add column business_unit_id with GIN index.
*
**/

DROP INDEX if_bu_code_gin_idx;

ALTER TABLE interface_files
    DROP COLUMN business_unit_code,
    ADD COLUMN business_unit_id SMALLINT[] NULL;

CREATE INDEX if_bu_id_idx
    ON interface_files
    USING GIN (business_unit_id);
