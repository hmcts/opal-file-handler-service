/**
* OPAL Program
*
* MODULE      : create_interface_files_and_enums.sql
*
* DESCRIPTION : Create table interface_files and related ENUM data types
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 30/06/2026    T McCallion    1.0         PO-3965 - Create INTERFACE_FILES table and related components for EI1
*
**/

CREATE TYPE t_interface_enum AS ENUM (
    'BTECKOH_REPORT',
    'CAPS_REPORT',
    'OPAL'
);

CREATE TYPE t_type_enum AS ENUM (
    'SOURCE'
);

CREATE TYPE t_status_enum AS ENUM (
    'DUPLICATE',
    'INGESTED',
    'SUCCESS',
    'SUCCESS_NO_TRANSACTIONS',
    'SUPERSEDED',
    'FAILED',
    'FAILED_SUPERSEDED'
);

CREATE TYPE t_domain_enum AS ENUM (
    'FINES',
    'CONFISCATION',
    'MAINTENANCE',
    'FILE_HANDLER'
);

CREATE SEQUENCE interface_file_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE interface_files (
    interface_file_id   BIGINT NOT NULL DEFAULT nextval('interface_file_id_seq'),
    source              t_interface_enum NOT NULL,
    target              t_interface_enum NOT NULL,
    type                t_type_enum NOT NULL,
    opal_domain         t_domain_enum NOT NULL,
    file_name           VARCHAR(200) NOT NULL,
    filestore_uuid      UUID,
    checksum            VARCHAR(32),
    status              t_status_enum NOT NULL,
    created_datetime    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    errors              JSON,
    CONSTRAINT interface_files_pk PRIMARY KEY (interface_file_id)
);

COMMENT ON COLUMN interface_files.interface_file_id IS 'Unique ID (PK) of this record';
COMMENT ON COLUMN interface_files.source IS 'The system from which the Interface File originated';
COMMENT ON COLUMN interface_files.target IS 'The intended recipient of the Interface File';
COMMENT ON COLUMN interface_files.type IS 'The type of file this Interface File represents (i.e. SOURCE, SOURCE_JSON or TRANSFORMED_JSON)';
COMMENT ON COLUMN interface_files.opal_domain IS 'The domain service responsible for processing this Interface File';
COMMENT ON COLUMN interface_files.file_name IS 'The file name of this Interface File';
COMMENT ON COLUMN interface_files.filestore_uuid IS 'The Azure unique identifier of the stored Interface File';
COMMENT ON COLUMN interface_files.checksum IS 'The MD5 checksum of the stored Interface File';
COMMENT ON COLUMN interface_files.status IS 'The status of this Interface File';
COMMENT ON COLUMN interface_files.created_datetime IS 'The date and time of when this Interface File record was created';
COMMENT ON COLUMN interface_files.errors IS 'A JSON array containing the errors if the Interface File process failed';

ALTER SEQUENCE interface_file_id_seq
    OWNED BY interface_files.interface_file_id;

CREATE INDEX if_created_datetime_idx
    ON interface_files (created_datetime);
