-- Run against opal-fines-db after 01_seed_bu80_10_accounts.sql.
-- Owns two BANK_ACCOUNTS configuration rows, fourteen APRs, and the account-number
-- changes on the ten synthetic CODEX_BU80_10_V1 defendant accounts only.
-- Maps the former child reference to account 2 through a CONSOLIDATED APR.
-- Also owns account 4 outstanding debt of 100,000,000,000 pence (stored in pounds).
-- Adds one account in BU78 to prove identical APR text resolves within each BU.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(800080, 10);

CREATE TEMP TABLE _allpay_accounts ON COMMIT DROP AS
SELECT n, 58008000000000::bigint + n AS defendant_account_id,
       'ZB80D' || lpad(n::text, 4, '0') AS original_number,
       CASE WHEN n=3 THEN 'ZB80D0003' ELSE (80000000 + n)::text || 'A' END AS account_number,
       'CODEX_BU80_10_V1:' || lpad(n::text, 4, '0') AS marker
FROM generate_series(1, 10) AS g(n);

CREATE TEMP TABLE _allpay_aprs ON COMMIT DROP AS
SELECT 58008000000800::bigint + n AS apr_id, defendant_account_id,
       'ZB80C' || lpad(n::text, 4, '0') AS apr_text, 80::smallint AS business_unit_id,
       'APR'::text AS relationship, 'APR'::text AS category
FROM _allpay_accounts
UNION ALL SELECT 58008000000811, 58008000000001, 'TEST-APR-01', 80, 'APR', 'APR'
UNION ALL SELECT 58008000000812, 58008000000001, 'ABCXYZ', 80, 'APR', 'APR'
UNION ALL SELECT 88007800000801, 88007800000001, 'TEST-APR-01', 78, 'APR', 'APR'
UNION ALL SELECT 58008000000813, 58008000000002, '80000003A', 80, 'CONSOLIDATED', 'ACC';

DO $check$
BEGIN
    IF (SELECT count(*) FROM public.defendant_accounts a JOIN _allpay_accounts f
        ON a.defendant_account_id=f.defendant_account_id
        WHERE a.business_unit_id=80 AND a.account_comments=f.marker
          AND a.account_number IN (f.original_number, f.account_number)) <> 10 THEN
        RAISE EXCEPTION 'Expected all ten unchanged BU80 seed accounts; run the base seed first.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_accounts a JOIN _allpay_accounts f
               ON a.account_number=f.account_number AND a.business_unit_id=80
               WHERE a.defendant_account_id<>f.defendant_account_id)
       OR EXISTS (SELECT 1 FROM public.account_number_index i JOIN _allpay_accounts f
                  ON i.account_number=f.account_number WHERE i.business_unit_id=80) THEN
        RAISE EXCEPTION 'An Allpay canonical account reference is already occupied.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.configuration_items
               WHERE configuration_item_id=58008000000700
                 AND (business_unit_id IS DISTINCT FROM 80 OR item_name<>'BANK_ACCOUNTS'
                      OR item_value IS DISTINCT FROM 'ALLPAY_BU80_PREPROCESSING_V1')) THEN
        RAISE EXCEPTION 'Reserved Allpay configuration ID is occupied.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.configuration_items c,
             LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(c.item_values::jsonb)='array'
                 THEN c.item_values::jsonb ELSE '[]'::jsonb END) b
        WHERE c.item_name='BANK_ACCOUNTS' AND c.configuration_item_id NOT IN (58008000000700,88007800000700)
          AND (b->>'sort_code', b->>'account_number') IN
              (('803456','80345678'),('804567','80456789'),('806789','80678901'),
               ('678901','67890123'))
    ) THEN
        RAISE EXCEPTION 'A fixture destination bank already has another configuration; refusing ambiguous resolution.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.alternate_payment_references a JOIN _allpay_aprs f
               ON a.alternate_payment_reference_id=f.apr_id OR (a.business_unit_id=f.business_unit_id AND a.apr_text=f.apr_text)
               WHERE a.alternate_payment_reference_id<>f.apr_id OR a.business_unit_id<>f.business_unit_id
                  OR a.defendant_account_id<>f.defendant_account_id OR a.apr_text<>f.apr_text
                  OR a.category::text<>f.category OR a.relationship::text<>f.relationship) THEN
        RAISE EXCEPTION 'An Allpay APR fixture ID or value is already occupied.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_accounts
               WHERE business_unit_id=80 AND account_number='80000003A')
       OR EXISTS (SELECT 1 FROM public.account_number_index
                  WHERE business_unit_id=80 AND account_number='80000003A') THEN
        RAISE EXCEPTION 'Former Allpay child reference must exist only as an APR; run old fixture cleanup first.';
    END IF;
    -- Only accept the original or already-enlarged, unpaid account-4 fixture.
    IF (SELECT count(*) FROM public.defendant_accounts
        WHERE defendant_account_id=58008000000004 AND business_unit_id=80
          AND account_comments='CODEX_BU80_10_V1:0004' AND amount_paid=0
          AND amount_imposed=account_balance AND account_balance IN (-400.00,-1000000000.00))<>1
       OR (SELECT count(*) FROM public.impositions
           WHERE imposition_id=58008000000604 AND defendant_account_id=58008000000004
             AND posted_by='ZB80SEED' AND paid_amount=0
             AND imposed_amount IN (-400.00,-1000000000.00))<>1
       OR (SELECT count(*) FROM public.payment_terms
           WHERE payment_terms_id=58008000000504 AND defendant_account_id=58008000000004
             AND posted_by='ZB80SEED' AND account_balance IN (-400.00,-1000000000.00))<>1 THEN
        RAISE EXCEPTION 'Unexpected Allpay large-account amounts or ownership; refusing to overwrite them.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.business_units WHERE business_unit_id=78) THEN
        RAISE EXCEPTION 'Allpay duplicate-APR fixture requires migrated Fines BU78.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_accounts WHERE defendant_account_id=88007800000001
               AND (business_unit_id<>78 OR account_number<>'78000001A'
                    OR account_comments IS DISTINCT FROM 'ALLPAY_BU78_APR_V1'
                    OR amount_imposed<>-100.00 OR amount_paid<>0 OR account_balance<>-100.00
                    OR account_status::text<>'L' OR account_type::text<>'Fine'))
       OR EXISTS (SELECT 1 FROM public.defendant_accounts WHERE business_unit_id=78
                  AND account_number='78000001A' AND defendant_account_id<>88007800000001)
       OR EXISTS (SELECT 1 FROM public.account_number_index
                  WHERE business_unit_id=78 AND account_number='78000001A') THEN
        RAISE EXCEPTION 'Allpay BU78 account ID, reference or amounts are occupied/changed.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.configuration_items WHERE configuration_item_id=88007800000700
               AND (business_unit_id IS DISTINCT FROM 78 OR item_name<>'BANK_ACCOUNTS'
                    OR item_value IS DISTINCT FROM 'ALLPAY_BU78_APR_V1')) THEN
        RAISE EXCEPTION 'Reserved Allpay BU78 configuration ID is occupied.';
    END IF;
END
$check$;

UPDATE public.defendant_accounts a SET account_number=f.account_number
FROM _allpay_accounts f
WHERE a.defendant_account_id=f.defendant_account_id AND a.business_unit_id=80
  AND a.account_comments=f.marker AND a.account_number=f.original_number;

INSERT INTO public.configuration_items
    (configuration_item_id, business_unit_id, item_name, item_value, item_values)
VALUES (58008000000700, 80, 'BANK_ACCOUNTS', 'ALLPAY_BU80_PREPROCESSING_V1',
    '[{"sort_code":"803456","account_number":"80345678","name":"Allpay BU80 test bank 1"},
      {"sort_code":"804567","account_number":"80456789","name":"Allpay BU80 test bank 2"},
      {"sort_code":"806789","account_number":"80678901","name":"Allpay BU80 test bank 3"}]')
ON CONFLICT (configuration_item_id) DO NOTHING;

-- Preprocessing needs an account lookup target; no payment allocation is seeded for BU78.
INSERT INTO public.defendant_accounts
    (defendant_account_id,business_unit_id,account_number,amount_imposed,amount_paid,
     account_balance,account_status,account_type,account_comments,version_number)
VALUES (88007800000001,78,'78000001A',-100.00,0,-100.00,'L','Fine','ALLPAY_BU78_APR_V1',0)
ON CONFLICT (defendant_account_id) DO NOTHING;

INSERT INTO public.configuration_items
    (configuration_item_id,business_unit_id,item_name,item_value,item_values)
VALUES (88007800000700,78,'BANK_ACCOUNTS','ALLPAY_BU78_APR_V1',
    '[{"sort_code":"678901","account_number":"67890123","name":"Allpay BU78 APR test bank"}]')
ON CONFLICT (configuration_item_id) DO NOTHING;

INSERT INTO public.alternate_payment_references
    (alternate_payment_reference_id, defendant_account_id, relationship, category,
     business_unit_id, apr_text, created_datetime, updated_datetime, version_number)
SELECT apr_id, defendant_account_id, relationship::public.t_apr_relationship_enum,
       category::public.t_apr_category_enum, business_unit_id, apr_text, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM _allpay_aprs
ON CONFLICT (alternate_payment_reference_id) DO NOTHING;

-- 000008 pays 99,999,999,999 pence; give account 4 one penny more debt.
-- Keep the account summary, original imposition and payment-terms balance aligned.
UPDATE public.defendant_accounts
SET amount_imposed=-1000000000.00, account_balance=-1000000000.00
WHERE defendant_account_id=58008000000004;
UPDATE public.impositions SET imposed_amount=-1000000000.00
WHERE imposition_id=58008000000604 AND defendant_account_id=58008000000004;
UPDATE public.payment_terms SET account_balance=-1000000000.00
WHERE payment_terms_id=58008000000504 AND defendant_account_id=58008000000004;

-- Prove the former child reference can resolve only through its APR to the master.
DO $verify$
BEGIN
    IF EXISTS (SELECT 1 FROM public.defendant_accounts
               WHERE business_unit_id=80 AND account_number='80000003A')
       OR NOT EXISTS (SELECT 1 FROM public.alternate_payment_references
                      WHERE alternate_payment_reference_id=58008000000813
                        AND business_unit_id=80 AND apr_text='80000003A'
                        AND defendant_account_id=58008000000002
                        AND relationship::text='CONSOLIDATED' AND category::text='ACC') THEN
        RAISE EXCEPTION 'Allpay former-child APR fixture was not established.';
    END IF;
    IF (SELECT count(*) FROM public.alternate_payment_references
        WHERE apr_text='TEST-APR-01' AND
          ((business_unit_id=80 AND defendant_account_id=58008000000001)
           OR (business_unit_id=78 AND defendant_account_id=88007800000001)))<>2 THEN
        RAISE EXCEPTION 'Allpay same-text APR fixtures were not established in both BUs.';
    END IF;
END
$verify$;
COMMIT;
