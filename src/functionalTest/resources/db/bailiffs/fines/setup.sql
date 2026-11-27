-- Fines resolves bailiff SOURCE_JSON destinations using BANK_ACCOUNTS, separately from
-- File Handler's business_unit_bank_account lookup. Keep this seed between scenarios.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(800080, 7244);

DO $check$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.business_units WHERE business_unit_id = 80) THEN
        RAISE EXCEPTION 'Bailiff preprocessing requires migrated Fines BU80.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.configuration_items
               WHERE configuration_item_id = 88008030000701
                 AND (business_unit_id IS DISTINCT FROM 80 OR item_name <> 'BANK_ACCOUNTS'
                      OR item_value IS DISTINCT FROM 'BAILIFF_BU80_PREPROCESSING_V1'
                      OR item_values::jsonb IS DISTINCT FROM
                         ('[{"sort_code":"800036","account_number":"80048530","name":"HMCTS CDER"},'
                         || '{"sort_code":"800037","account_number":"80048531","name":"HMCTS Jacobs"},'
                         || '{"sort_code":"800038","account_number":"80048532","name":"HMCTS Marston"}]')::jsonb)) THEN
        RAISE EXCEPTION 'Reserved bailiff bank configuration is occupied or changed.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.configuration_items c,
             LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(c.item_values::jsonb) = 'array'
                 THEN c.item_values::jsonb ELSE '[]'::jsonb END) b
        WHERE c.item_name = 'BANK_ACCOUNTS' AND c.configuration_item_id <> 88008030000701
          AND (b->>'sort_code', b->>'account_number') IN
              (('800036', '80048530'), ('800037', '80048531'), ('800038', '80048532'))
    ) THEN
        RAISE EXCEPTION 'A bailiff destination bank already has another Fines configuration.';
    END IF;
END
$check$;

INSERT INTO public.configuration_items
    (configuration_item_id, business_unit_id, item_name, item_value, item_values)
VALUES (88008030000701, 80, 'BANK_ACCOUNTS', 'BAILIFF_BU80_PREPROCESSING_V1',
    ('[{"sort_code":"800036","account_number":"80048530","name":"HMCTS CDER"},'
    || '{"sort_code":"800037","account_number":"80048531","name":"HMCTS Jacobs"},'
    || '{"sort_code":"800038","account_number":"80048532","name":"HMCTS Marston"}]')::json)
ON CONFLICT (configuration_item_id) DO NOTHING;
COMMIT;
