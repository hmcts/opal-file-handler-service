/**
* OPAL Program
*
* MODULE      : create_business_unit_bank_account_table.sql
*
* DESCRIPTION : Create table business_unit_bank_account and related components for EI2
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 22/07/2026    B Edwards      1.0         PO-5771 - Create BUSINESS_UNIT_BANK_ACCOUNT table and related components
*
**/

CREATE SEQUENCE business_unit_bank_account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE business_unit_bank_account (
    business_unit_bank_account_id BIGINT NOT NULL DEFAULT nextval('business_unit_bank_account_id_seq'),
    business_unit_code            VARCHAR(4) NOT NULL,
    opal_domain                   t_domain_enum NOT NULL,
    bank_sort_code                VARCHAR(6) NOT NULL,
    bank_account_number           VARCHAR(10) NOT NULL,
    dwp_court_code                VARCHAR(10),
    CONSTRAINT business_unit_bank_account_pk PRIMARY KEY (business_unit_bank_account_id),
    CONSTRAINT buba_business_unit_code_uk UNIQUE (business_unit_code)
);

COMMENT ON COLUMN business_unit_bank_account.business_unit_bank_account_id IS 'Unique ID (PK) of this record';
COMMENT ON COLUMN business_unit_bank_account.business_unit_code IS 'Business unit code';
COMMENT ON COLUMN business_unit_bank_account.opal_domain IS 'The Opal domain related to this business unit';
COMMENT ON COLUMN business_unit_bank_account.bank_sort_code IS 'The bank sort code of this business unit''s bank account';
COMMENT ON COLUMN business_unit_bank_account.bank_account_number IS 'The bank account number of this business unit''s bank account';
COMMENT ON COLUMN business_unit_bank_account.dwp_court_code IS 'DWP code from DWP/Bailiff interface files related to this business unit';

ALTER SEQUENCE business_unit_bank_account_id_seq
    OWNED BY business_unit_bank_account.business_unit_bank_account_id;


