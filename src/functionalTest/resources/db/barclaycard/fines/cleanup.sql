-- Optional manual cleanup after Barclaycard preprocessing jobs have been removed.
-- Removes only Barclaycard-owned fixtures; no Allpay scripts/data are used.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(800080, 6427);

CREATE TEMP TABLE _barclaycard_cleanup_accounts ON COMMIT DROP AS
SELECT * FROM (VALUES
    (88008010000001::bigint,'80100001A',-500.00::numeric,'BARCLAYCARD_BU80_PREPROCESSING_V1:01'),
    (88008010000002::bigint,'80100002A',-600.00::numeric,'BARCLAYCARD_BU80_PREPROCESSING_V1:02')
) AS f(account_id,account_number,balance,marker);

DO $check$
BEGIN
    IF EXISTS (SELECT 1 FROM public.defendant_accounts a JOIN _barclaycard_cleanup_accounts f
               ON a.defendant_account_id=f.account_id
               WHERE a.business_unit_id<>80 OR a.account_number<>f.account_number
                  OR a.account_comments IS DISTINCT FROM f.marker OR a.amount_imposed<>f.balance
                  OR a.amount_paid<>0 OR a.account_balance<>f.balance
                  OR a.account_status::text<>'L' OR a.account_type::text<>'Fine')
       OR EXISTS (SELECT 1 FROM public.alternate_payment_references
                  WHERE alternate_payment_reference_id=88008010000801
                    AND (business_unit_id<>80 OR apr_text<>'BC-APR-01'
                         OR defendant_account_id<>88008010000002
                         OR category::text<>'APR' OR relationship::text<>'APR'))
       OR EXISTS (SELECT 1 FROM public.configuration_items WHERE configuration_item_id=88008000000701
                  AND (business_unit_id IS DISTINCT FROM 80 OR item_name<>'BANK_ACCOUNTS'
                       OR item_value IS DISTINCT FROM 'BARCLAYCARD_BU80_PREPROCESSING_V1'
                       OR item_values::jsonb IS DISTINCT FROM
                          '[{"sort_code":"800034","account_number":"80048528","name":"Barclaycard BU80 test bank"}]'::jsonb)) THEN
        RAISE EXCEPTION 'Barclaycard fixture ownership or state changed; preserving all fixtures.';
    END IF;
END
$check$;

DELETE FROM public.alternate_payment_references
WHERE alternate_payment_reference_id=88008010000801 AND business_unit_id=80
  AND apr_text='BC-APR-01' AND defendant_account_id=88008010000002
  AND category::text='APR' AND relationship::text='APR';
DELETE FROM public.configuration_items
WHERE configuration_item_id=88008000000701 AND business_unit_id=80
  AND item_name='BANK_ACCOUNTS' AND item_value='BARCLAYCARD_BU80_PREPROCESSING_V1';
DELETE FROM public.defendant_accounts a USING _barclaycard_cleanup_accounts f
WHERE a.defendant_account_id=f.account_id AND a.business_unit_id=80 AND a.account_comments=f.marker;
COMMIT;
