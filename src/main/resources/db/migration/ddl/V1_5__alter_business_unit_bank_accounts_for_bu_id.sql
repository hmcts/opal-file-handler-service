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
* 06/10/2026    TT          1.0         PO-10807 - Replace the surrogate Primary Key column (business_unit_bank_account_id)
*                                                  with business_unit_id as the Primary Key.
*                                                  Drop column business_unit_bank_account_id.
*
**/

ALTER TABLE business_unit_bank_account
    DROP CONSTRAINT business_unit_bank_account_pk,
    ADD CONSTRAINT business_unit_bank_account_pk
        PRIMARY KEY (business_unit_id),
    DROP COLUMN business_unit_bank_account_id;
