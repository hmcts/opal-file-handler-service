-- Fines resolves bailiff SOURCE_JSON destinations using BANK_ACCOUNTS, separately from
-- File Handler's business_unit_bank_account lookup. Keep this seed between scenarios.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(770077, 7244);

DO $check$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.business_units WHERE business_unit_id = 77) THEN
        RAISE EXCEPTION 'Bailiff preprocessing requires migrated Fines BU77.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.configuration_items
               WHERE configuration_item_id = 88007730000701
                 AND (business_unit_id IS DISTINCT FROM 77 OR item_name <> 'BANK_ACCOUNTS'
                      OR item_value IS DISTINCT FROM 'BAILIFF_BU77_PREPROCESSING_V1'
                      OR item_values::jsonb IS DISTINCT FROM
                         ('[{"sort_code":"560036","account_number":"27048530","name":"HMCTS CDER"},'
                         || '{"sort_code":"560037","account_number":"27048531","name":"HMCTS Jacobs"},'
                         || '{"sort_code":"560038","account_number":"27048532","name":"HMCTS Marston"}]')::jsonb)) THEN
        RAISE EXCEPTION 'Reserved bailiff bank configuration is occupied or changed.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.configuration_items c,
             LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(c.item_values::jsonb) = 'array'
                 THEN c.item_values::jsonb ELSE '[]'::jsonb END) b
        WHERE c.item_name = 'BANK_ACCOUNTS' AND c.configuration_item_id <> 88007730000701
          AND (b->>'sort_code', b->>'account_number') IN
              (('560036', '27048530'), ('560037', '27048531'), ('560038', '27048532'))
    ) THEN
        RAISE EXCEPTION 'A bailiff destination bank already has another Fines configuration.';
    END IF;
END
$check$;

INSERT INTO public.configuration_items
    (configuration_item_id, business_unit_id, item_name, item_value, item_values)
VALUES (88007730000701, 77, 'BANK_ACCOUNTS', 'BAILIFF_BU77_PREPROCESSING_V1',
    ('[{"sort_code":"560036","account_number":"27048530","name":"HMCTS CDER"},'
    || '{"sort_code":"560037","account_number":"27048531","name":"HMCTS Jacobs"},'
    || '{"sort_code":"560038","account_number":"27048532","name":"HMCTS Marston"}]')::json)
ON CONFLICT (configuration_item_id) DO NOTHING;
COMMIT;
