-- Optional manual cleanup after BTEckoh transfer preprocessing jobs have been removed.
-- Removes only BTEckoh transfer-owned fixtures; no Allpay scripts/data are used.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(800080, 6428);

CREATE TEMP TABLE _bteckoh_cleanup_accounts ON COMMIT DROP AS
SELECT * FROM (VALUES
    (88008020000001::bigint,'80200001A',-500.00::numeric,'BTECKOH_BU80_PREPROCESSING_V1:01'),
    (88008020000002::bigint,'80200002A',-600.00::numeric,'BTECKOH_BU80_PREPROCESSING_V1:02')
) AS f(account_id,account_number,balance,marker);

DO $check$
BEGIN
    IF EXISTS (SELECT 1 FROM public.defendant_accounts a JOIN _bteckoh_cleanup_accounts f
               ON a.defendant_account_id=f.account_id
               WHERE a.business_unit_id<>80 OR a.account_number<>f.account_number
                  OR a.account_comments IS DISTINCT FROM f.marker OR a.amount_imposed<>f.balance
                  OR a.amount_paid<>0 OR a.account_balance<>f.balance
                  OR a.account_status::text<>'L' OR a.account_type::text<>'Fine')
       OR EXISTS (SELECT 1 FROM public.alternate_payment_references
                  WHERE alternate_payment_reference_id=88008020000801
                    AND (business_unit_id<>80 OR apr_text<>'BT-APR-01'
                         OR defendant_account_id<>88008020000002
                         OR category::text<>'APR' OR relationship::text<>'APR'))
       OR EXISTS (SELECT 1 FROM public.configuration_items WHERE configuration_item_id=88008020000701
                  AND (business_unit_id IS DISTINCT FROM 80 OR item_name<>'BANK_ACCOUNTS'
                       OR item_value IS DISTINCT FROM 'BTECKOH_BU80_PREPROCESSING_V1'
                       OR item_values::jsonb IS DISTINCT FROM
                          '[{"sort_code":"800102","account_number":"80341235","name":"BTEckoh transfer BU80 test bank"}]'::jsonb)) THEN
        RAISE EXCEPTION 'BTEckoh transfer fixture ownership or state changed; preserving all fixtures.';
    END IF;
END
$check$;

DELETE FROM public.alternate_payment_references
WHERE alternate_payment_reference_id=88008020000801 AND business_unit_id=80
  AND apr_text='BT-APR-01' AND defendant_account_id=88008020000002
  AND category::text='APR' AND relationship::text='APR';
DELETE FROM public.configuration_items
WHERE configuration_item_id=88008020000701 AND business_unit_id=80
  AND item_name='BANK_ACCOUNTS' AND item_value='BTECKOH_BU80_PREPROCESSING_V1';
DELETE FROM public.defendant_accounts a USING _bteckoh_cleanup_accounts f
WHERE a.defendant_account_id=f.account_id AND a.business_unit_id=80 AND a.account_comments=f.marker;
COMMIT;
