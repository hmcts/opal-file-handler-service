/**
* OPAL Program
*
* MODULE      : update_dwp_court_codes_for_business_unit_bank_account.sql
*
* DESCRIPTION : Set unique DWP court codes for three NLE business unit bank accounts
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 23/11/2026    TT             1.0         PO-11785 - Set DWP court codes for three NLE business unit bank accounts
*
**/

DO $migration$
DECLARE
    updated_row_count integer;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM business_unit_bank_account
        WHERE dwp_court_code IN ('0000031714', '0000031715', '0000031716')
          AND business_unit_id NOT IN (5, 8, 9)
    ) THEN
        RAISE EXCEPTION 'One or more DWP court codes are already assigned to another business unit';
    END IF;

    UPDATE business_unit_bank_account
    SET dwp_court_code = CASE business_unit_id
        WHEN 5 THEN '0000031714'
        WHEN 8 THEN '0000031715'
        WHEN 9 THEN '0000031716'
    END
    WHERE (business_unit_id, business_unit_code) IN (
        (5, '0009'),
        (8, '0011'),
        (9, '0012')
    )
      AND dwp_court_code IS NULL;

    GET DIAGNOSTICS updated_row_count = ROW_COUNT;

    IF updated_row_count <> 3 THEN
        RAISE EXCEPTION 'Expected to update 3 business unit bank accounts, but updated %', updated_row_count;
    END IF;
END
$migration$;
