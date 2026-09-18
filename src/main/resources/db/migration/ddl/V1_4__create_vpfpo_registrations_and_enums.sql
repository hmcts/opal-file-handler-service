/**
* OPAL Program
*
* MODULE      : create_vpfpo_registrations_and_enums.sql
*
* DESCRIPTION : Create table vpfpo_registrations and related ENUM data types
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 18/09/2026    TMc            1.0         PO-10374 - File Handler - Create vpfpo_registrations table
*
**/

ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'VPFPO';

CREATE TYPE t_vpfpo_registration_status_enum AS ENUM (
    'CREATED',
    'SUBMITTED',
    'EXTRACTION_FAILED'
);

CREATE SEQUENCE vpfpo_registration_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE vpfpo_registrations (
    vpfpo_registration_id       BIGINT NOT NULL DEFAULT nextval('vpfpo_registration_id_seq'),
    interface_file_id           BIGINT NOT NULL,
    registration_status         t_vpfpo_registration_status_enum NOT NULL,
    acknowledged                BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledgement_file_name   VARCHAR(200),
    lja_code                    SMALLINT,
    cto_code                    INTEGER,
    ticket_number               VARCHAR(16),
    business_unit_code          VARCHAR(4),
    defendant_account_id        BIGINT,
    errors                      JSON,
    retry_count                 SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT vpfpo_registrations_pk PRIMARY KEY (vpfpo_registration_id),
    CONSTRAINT vr_interface_file_id_fk FOREIGN KEY (interface_file_id)
        REFERENCES interface_files (interface_file_id)
);

COMMENT ON COLUMN vpfpo_registrations.vpfpo_registration_id IS 'Primary key.';
COMMENT ON COLUMN vpfpo_registrations.interface_file_id IS 'Foreign key referencing the source JSON VPFPO interface file on interface_files.';
COMMENT ON COLUMN vpfpo_registrations.registration_status IS 'Status of the individual registration.';
COMMENT ON COLUMN vpfpo_registrations.acknowledged IS 'Indicates whether the registration has been acknowledged.';
COMMENT ON COLUMN vpfpo_registrations.acknowledgement_file_name IS 'Name of the acknowledgement file in external interface blob storage.';
COMMENT ON COLUMN vpfpo_registrations.lja_code IS 'PSA/LJA code.';
COMMENT ON COLUMN vpfpo_registrations.cto_code IS 'Central Ticket Office initiating the registration.';
COMMENT ON COLUMN vpfpo_registrations.ticket_number IS 'Fixed penalty notice ticket number.';
COMMENT ON COLUMN vpfpo_registrations.business_unit_code IS 'Business Unit Code (Accounting Division) related to the PSA/LJA code.';
COMMENT ON COLUMN vpfpo_registrations.defendant_account_id IS 'Primary key value of the created Defendant Account in the Opal domain service.';
COMMENT ON COLUMN vpfpo_registrations.errors IS 'JSON array containing processing errors.';
COMMENT ON COLUMN vpfpo_registrations.retry_count IS 'Number of retries made to process the registration.';

ALTER SEQUENCE vpfpo_registration_id_seq OWNED BY vpfpo_registrations.vpfpo_registration_id;

CREATE INDEX vr_interface_file_id_idx ON vpfpo_registrations (interface_file_id);
