-- Fines resolves the DWP destination bank independently of File Handler's court-code lookup.
-- Keep these owned lookup fixtures between scenarios, like the other Fines preprocessing seeds.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(770077, 6436);

DO $check$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.business_units WHERE business_unit_id = 77) THEN
        RAISE EXCEPTION 'DWP preprocessing requires migrated Fines BU77.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_accounts
               WHERE defendant_account_id = 88007740000001
                 AND (business_unit_id IS DISTINCT FROM 77 OR account_number IS DISTINCT FROM '77400001A'
                      OR account_comments IS DISTINCT FROM 'DWP_BU77_PREPROCESSING_V1:01'
                      OR amount_imposed IS DISTINCT FROM -100.00 OR amount_paid IS DISTINCT FROM 0
                      OR account_balance IS DISTINCT FROM -100.00
                      OR account_status::text IS DISTINCT FROM 'L' OR account_type::text IS DISTINCT FROM 'Fine'))
       OR EXISTS (SELECT 1 FROM public.defendant_accounts
                  WHERE business_unit_id = 77
                    AND ((account_number = '77400001A' AND defendant_account_id <> 88007740000001)
                         OR account_number = 'DWP-APR-01'))
       OR EXISTS (SELECT 1 FROM public.account_number_index
                  WHERE business_unit_id = 77 AND account_number IN ('77400001A', 'DWP-APR-01')) THEN
        RAISE EXCEPTION 'DWP account fixture ID or lookup references are occupied/changed.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.alternate_payment_references
               WHERE (alternate_payment_reference_id = 88007740000801
                      OR (business_unit_id = 77 AND apr_text = 'DWP-APR-01'))
                 AND (alternate_payment_reference_id <> 88007740000801
                      OR business_unit_id IS DISTINCT FROM 77 OR apr_text IS DISTINCT FROM 'DWP-APR-01'
                      OR defendant_account_id IS DISTINCT FROM 88007740000001
                      OR category::text IS DISTINCT FROM 'APR' OR relationship::text IS DISTINCT FROM 'APR')) THEN
        RAISE EXCEPTION 'DWP APR fixture ID or value is occupied/changed.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.configuration_items
               WHERE configuration_item_id = 88007740000701
                 AND (business_unit_id IS DISTINCT FROM 77 OR item_name <> 'BANK_ACCOUNTS'
                      OR item_value IS DISTINCT FROM 'DWP_BU77_PREPROCESSING_V1'
                      OR item_values::jsonb IS DISTINCT FROM
                         '[{"sort_code":"010101","account_number":"12341234","name":"HMCTS DWP"}]'::jsonb)) THEN
        RAISE EXCEPTION 'Reserved DWP bank configuration is occupied or changed.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.configuration_items c,
             LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(c.item_values::jsonb) = 'array'
                 THEN c.item_values::jsonb ELSE '[]'::jsonb END) b
        WHERE c.item_name = 'BANK_ACCOUNTS' AND c.configuration_item_id <> 88007740000701
          AND b->>'sort_code' = '010101' AND b->>'account_number' = '12341234'
    ) THEN
        RAISE EXCEPTION 'The DWP destination bank already has another Fines configuration.';
    END IF;
END
$check$;

-- Lookup target only: preprocessing does not allocate payments.
INSERT INTO public.defendant_accounts
    (defendant_account_id, business_unit_id, account_number, amount_imposed, amount_paid,
     account_balance, account_status, account_type, account_comments, version_number)
VALUES (88007740000001, 77, '77400001A', -100.00, 0, -100.00, 'L', 'Fine',
    'DWP_BU77_PREPROCESSING_V1:01', 0)
ON CONFLICT (defendant_account_id) DO NOTHING;

INSERT INTO public.alternate_payment_references
    (alternate_payment_reference_id, defendant_account_id, relationship, category,
     business_unit_id, apr_text, created_datetime, updated_datetime, version_number)
VALUES (88007740000801, 88007740000001, 'APR', 'APR', 77, 'DWP-APR-01',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (alternate_payment_reference_id) DO NOTHING;

INSERT INTO public.configuration_items
    (configuration_item_id, business_unit_id, item_name, item_value, item_values)
VALUES (88007740000701, 77, 'BANK_ACCOUNTS', 'DWP_BU77_PREPROCESSING_V1',
    '[{"sort_code":"010101","account_number":"12341234","name":"HMCTS DWP"}]'::json)
ON CONFLICT (configuration_item_id) DO NOTHING;
COMMIT;
