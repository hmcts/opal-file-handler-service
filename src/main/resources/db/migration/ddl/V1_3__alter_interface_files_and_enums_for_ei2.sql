/**
* OPAL Program
*
* MODULE      : alter_interface_files_and_enums_for_ei2.sql
*
* DESCRIPTION : Alter existing ENUM types and create payment type ENUM for EI2 interface files
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 23/07/2026    B Edwards      1.0         PO-5769 - Amend ENUM types for EI2 (NatWest/MMH/DWP) and PO-5768 - Amend interface files table to include business unit code, payment type and related interface file id.
*
**/

ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'NATWEST';
ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'ALLPAY';
ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'ALLPAY_DD';
ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'BARCLAYCARD';
ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'BTECKOH';
ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'DWP';
ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'CDER';
ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'JACOBS';
ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'MARSTON';

ALTER TYPE t_type_enum ADD VALUE IF NOT EXISTS 'SOURCE_JSON';
ALTER TYPE t_type_enum ADD VALUE IF NOT EXISTS 'TRANSFORMED_JSON';


CREATE TYPE t_payment_type_enum AS ENUM ( 'CASH','CHEQUE' );

COMMENT ON TYPE t_payment_type_enum IS
	'The payment type related to the interface file';

ALTER TABLE interface_files
	ADD COLUMN business_unit_code VARCHAR(4)[],
	ADD COLUMN payment_type t_payment_type_enum,
	ADD COLUMN related_interface_file_id BIGINT;

COMMENT ON COLUMN interface_files.business_unit_code IS
    'An array of business unit codes related to the interface file';
COMMENT ON COLUMN interface_files.payment_type IS
    'The payment type related to the interface file';
COMMENT ON COLUMN interface_files.related_interface_file_id IS
    'References the parent interface file interface_file_id';

ALTER TABLE interface_files
	ADD CONSTRAINT if_related_interface_file_id_fk
		FOREIGN KEY (related_interface_file_id)
			REFERENCES interface_files (interface_file_id);

COMMENT ON CONSTRAINT if_related_interface_file_id_fk ON interface_files IS
    'Foreign Key constraint referencing interface_files.interface_file_id';

CREATE INDEX if_related_interface_file_id_idx
	ON interface_files (related_interface_file_id);

CREATE INDEX if_status_idx
	ON interface_files (status);


