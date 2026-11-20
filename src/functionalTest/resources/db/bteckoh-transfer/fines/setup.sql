-- Independent BTEckoh transfer preprocessing fixtures. No Allpay seed is required.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(800080, 6428);

CREATE TEMP TABLE _bteckoh_accounts ON COMMIT DROP AS
SELECT * FROM (VALUES
    (88008020000001::bigint,'80200001A',-500.00::numeric,'BTECKOH_BU80_PREPROCESSING_V1:01'),
    (88008020000002::bigint,'80200002A',-600.00::numeric,'BTECKOH_BU80_PREPROCESSING_V1:02')
) AS f(account_id,account_number,balance,marker);

DO $check$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.business_units WHERE business_unit_id=80) THEN
        RAISE EXCEPTION 'BTEckoh transfer preprocessing requires migrated Fines BU80.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_accounts a JOIN _bteckoh_accounts f
               ON a.defendant_account_id=f.account_id
               WHERE a.business_unit_id<>80 OR a.account_number<>f.account_number
                  OR a.account_comments IS DISTINCT FROM f.marker OR a.amount_imposed<>f.balance
                  OR a.amount_paid<>0 OR a.account_balance<>f.balance
                  OR a.account_status::text<>'L' OR a.account_type::text<>'Fine')
       OR EXISTS (SELECT 1 FROM public.defendant_accounts a JOIN _bteckoh_accounts f
                  ON a.business_unit_id=80 AND a.account_number=f.account_number
                  WHERE a.defendant_account_id<>f.account_id)
       OR EXISTS (SELECT 1 FROM public.account_number_index i JOIN _bteckoh_accounts f
                  ON i.business_unit_id=80 AND i.account_number=f.account_number) THEN
        RAISE EXCEPTION 'BTEckoh transfer account fixture IDs, references or amounts are occupied/changed.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.alternate_payment_references
               WHERE (alternate_payment_reference_id=88008020000801
                      OR (business_unit_id=80 AND apr_text='BT-APR-01'))
                 AND (alternate_payment_reference_id<>88008020000801 OR business_unit_id<>80
                      OR apr_text<>'BT-APR-01' OR defendant_account_id<>88008020000002
                      OR category::text<>'APR' OR relationship::text<>'APR')) THEN
        RAISE EXCEPTION 'BTEckoh transfer APR fixture ID or value is occupied/changed.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.configuration_items WHERE configuration_item_id=88008020000701
               AND (business_unit_id IS DISTINCT FROM 80 OR item_name<>'BANK_ACCOUNTS'
                    OR item_value IS DISTINCT FROM 'BTECKOH_BU80_PREPROCESSING_V1'
                    OR item_values::jsonb IS DISTINCT FROM
                       '[{"sort_code":"800102","account_number":"80341235","name":"BTEckoh transfer BU80 test bank"}]'::jsonb)) THEN
        RAISE EXCEPTION 'Reserved BTEckoh transfer bank configuration is occupied or changed.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.configuration_items c,
             LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(c.item_values::jsonb)='array'
                 THEN c.item_values::jsonb ELSE '[]'::jsonb END) b
        WHERE c.item_name='BANK_ACCOUNTS' AND c.configuration_item_id<>88008020000701
          AND b->>'sort_code'='800102' AND b->>'account_number'='80341235'
    ) THEN
        RAISE EXCEPTION 'BTEckoh transfer destination bank already has another configuration.';
    END IF;
END
$check$;

-- Account lookup targets only: preprocessing does not allocate payments.
INSERT INTO public.defendant_accounts
    (defendant_account_id,business_unit_id,account_number,amount_imposed,amount_paid,
     account_balance,account_status,account_type,account_comments,version_number)
SELECT account_id,80,account_number,balance,0,balance,'L','Fine',marker,0
FROM _bteckoh_accounts
ON CONFLICT (defendant_account_id) DO NOTHING;

INSERT INTO public.alternate_payment_references
    (alternate_payment_reference_id,defendant_account_id,relationship,category,
     business_unit_id,apr_text,created_datetime,updated_datetime,version_number)
VALUES (88008020000801,88008020000002,'APR','APR',80,'BT-APR-01',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)
ON CONFLICT (alternate_payment_reference_id) DO NOTHING;

INSERT INTO public.configuration_items
    (configuration_item_id,business_unit_id,item_name,item_value,item_values)
VALUES (88008020000701,80,'BANK_ACCOUNTS','BTECKOH_BU80_PREPROCESSING_V1',
    '[{"sort_code":"800102","account_number":"80341235","name":"BTEckoh transfer BU80 test bank"}]')
ON CONFLICT (configuration_item_id) DO NOTHING;
COMMIT;
