/**
* OPAL Program
*
* MODULE      : alter_business_unit_bank_accounts_for_bu_id.sql
*
* DESCRIPTION : Alter table business_unit_bank_accounts
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ----------------------------------------------------------------------------
* 06/10/2026    TT          1.0         PO-10807 - Add business_unit_id as the Primary Key.
*                                                  Drop business_unit_bank_account_id and its associated sequence.
**/

ALTER TABLE business_unit_bank_account
    ADD COLUMN business_unit_id SMALLINT NOT NULL,
    DROP COLUMN business_unit_bank_account_id,
    ADD CONSTRAINT business_unit_bank_account_pk
        PRIMARY KEY (business_unit_id);

COMMENT ON COLUMN business_unit_bank_account.business_unit_id IS
    'Business unit ID (primary key) of this record.';

DROP SEQUENCE IF EXISTS business_unit_bank_account_id_seq;
