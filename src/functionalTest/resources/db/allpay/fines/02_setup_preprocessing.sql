-- Run against opal-fines-db after 01_seed_bu77_10_accounts.sql.
-- Owns two BANK_ACCOUNTS configuration rows, thirteen APRs, and the account-number
-- changes on the ten synthetic CODEX_BU77_10_V1 defendant accounts only.
-- Also owns the consolidation between accounts 2 (master) and 3 (child),
-- and account 4 outstanding debt of 100,000,000,000 pence (stored in pounds).
-- Adds one account in BU78 to prove identical APR text resolves within each BU.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(770077, 10);

CREATE TEMP TABLE _allpay_accounts ON COMMIT DROP AS
SELECT n, 88007700000000::bigint + n AS defendant_account_id,
       'ZB77D' || lpad(n::text, 4, '0') AS original_number,
       (77000000 + n)::text || 'A' AS account_number,
       'CODEX_BU77_10_V1:' || lpad(n::text, 4, '0') AS marker
FROM generate_series(1, 10) AS g(n);

CREATE TEMP TABLE _allpay_aprs ON COMMIT DROP AS
SELECT 88007700000800::bigint + n AS apr_id, defendant_account_id,
       'ZB77C' || lpad(n::text, 4, '0') AS apr_text, 77::smallint AS business_unit_id
FROM _allpay_accounts
UNION ALL SELECT 88007700000811, 88007700000001, 'TEST-APR-01', 77
UNION ALL SELECT 88007700000812, 88007700000001, 'ABCXYZ', 77
UNION ALL SELECT 88007800000801, 88007800000001, 'TEST-APR-01', 78;

-- Fixed IDs and posted_by identify only this test-owned consolidation.
CREATE TEMP TABLE _allpay_consolidation ON COMMIT DROP AS
SELECT 88007700000901::bigint AS transaction_id, 88007700000003::bigint AS account_id,
       'WRTOFF'::text AS transaction_type, 'CONSOLIDATED ALLPAY BU77 CHILD'::text AS text,
       '88007700000002'::text AS associated_id
UNION ALL SELECT 88007700000902, 88007700000002, 'CONSOL',
                 'ALLPAY BU77 MASTER CONSOLIDATION', '88007700000003';

DO $check$
BEGIN
    IF (SELECT count(*) FROM public.defendant_accounts a JOIN _allpay_accounts f
        ON a.defendant_account_id=f.defendant_account_id
        WHERE a.business_unit_id=77 AND a.account_comments=f.marker
          AND a.account_number IN (f.original_number, f.account_number)) <> 10 THEN
        RAISE EXCEPTION 'Expected all ten unchanged BU77 seed accounts; run the base seed first.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_accounts a JOIN _allpay_accounts f
               ON a.account_number=f.account_number AND a.business_unit_id=77
               WHERE a.defendant_account_id<>f.defendant_account_id)
       OR EXISTS (SELECT 1 FROM public.account_number_index i JOIN _allpay_accounts f
                  ON i.account_number=f.account_number WHERE i.business_unit_id=77) THEN
        RAISE EXCEPTION 'An Allpay canonical account reference is already occupied.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.configuration_items
               WHERE configuration_item_id=88007700000700
                 AND (business_unit_id IS DISTINCT FROM 77 OR item_name<>'BANK_ACCOUNTS'
                      OR item_value IS DISTINCT FROM 'ALLPAY_BU77_PREPROCESSING_V1')) THEN
        RAISE EXCEPTION 'Reserved Allpay configuration ID is occupied.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.configuration_items c,
             LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(c.item_values::jsonb)='array'
                 THEN c.item_values::jsonb ELSE '[]'::jsonb END) b
        WHERE c.item_name='BANK_ACCOUNTS' AND c.configuration_item_id NOT IN (88007700000700,88007800000700)
          AND (b->>'sort_code', b->>'account_number') IN
              (('123456','12345678'),('234567','23456789'),('456789','45678901'),
               ('678901','67890123'))
    ) THEN
        RAISE EXCEPTION 'A fixture destination bank already has another configuration; refusing ambiguous resolution.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.alternate_payment_references a JOIN _allpay_aprs f
               ON a.alternate_payment_reference_id=f.apr_id OR (a.business_unit_id=f.business_unit_id AND a.apr_text=f.apr_text)
               WHERE a.alternate_payment_reference_id<>f.apr_id OR a.business_unit_id<>f.business_unit_id
                  OR a.defendant_account_id<>f.defendant_account_id OR a.apr_text<>f.apr_text
                  OR a.category::text<>'APR' OR a.relationship::text<>'APR') THEN
        RAISE EXCEPTION 'An Allpay APR fixture ID or value is already occupied.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_accounts
               WHERE defendant_account_id=88007700000002
                 AND (account_status::text<>'L' OR
                      (consolidated_account_type IS NOT NULL AND consolidated_account_type::text<>'M')))
       OR EXISTS (SELECT 1 FROM public.defendant_accounts
                  WHERE defendant_account_id=88007700000003
                    AND (account_status::text NOT IN ('L','CS') OR
                         (consolidated_account_type IS NOT NULL AND consolidated_account_type::text<>'C'))) THEN
        RAISE EXCEPTION 'Unexpected Allpay master/child account state; refusing to overwrite it.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_transactions t JOIN _allpay_consolidation f
               ON t.defendant_transaction_id=f.transaction_id
               WHERE t.defendant_account_id<>f.account_id OR t.transaction_type::text IS DISTINCT FROM f.transaction_type
                  OR t.text IS DISTINCT FROM f.text OR t.posted_by IS DISTINCT FROM 'ALLPAYSEED'
                  OR t.associated_record_type::text IS DISTINCT FROM 'defendant_accounts'
                  OR t.associated_record_id IS DISTINCT FROM f.associated_id)
       OR EXISTS (SELECT 1 FROM public.defendant_transactions t
                  WHERE (t.defendant_account_id IN (88007700000002,88007700000003)
                         OR (t.associated_record_type::text='defendant_accounts'
                             AND t.associated_record_id IN ('88007700000002','88007700000003')))
                    AND (t.transaction_type::text='CONSOL'
                         OR (t.transaction_type::text='WRTOFF' AND t.text LIKE 'CONSOLIDATED%'))
                    AND t.defendant_transaction_id NOT IN (88007700000901,88007700000902)) THEN
        RAISE EXCEPTION 'An Allpay consolidation ID or account relationship is already occupied.';
    END IF;
    -- Only accept the original or already-enlarged, unpaid account-4 fixture.
    IF (SELECT count(*) FROM public.defendant_accounts
        WHERE defendant_account_id=88007700000004 AND business_unit_id=77
          AND account_comments='CODEX_BU77_10_V1:0004' AND amount_paid=0
          AND amount_imposed=account_balance AND account_balance IN (-400.00,-1000000000.00))<>1
       OR (SELECT count(*) FROM public.impositions
           WHERE imposition_id=88007700000604 AND defendant_account_id=88007700000004
             AND posted_by='ZB77SEED' AND paid_amount=0
             AND imposed_amount IN (-400.00,-1000000000.00))<>1
       OR (SELECT count(*) FROM public.payment_terms
           WHERE payment_terms_id=88007700000504 AND defendant_account_id=88007700000004
             AND posted_by='ZB77SEED' AND account_balance IN (-400.00,-1000000000.00))<>1 THEN
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
WHERE a.defendant_account_id=f.defendant_account_id AND a.business_unit_id=77
  AND a.account_comments=f.marker AND a.account_number=f.original_number;

INSERT INTO public.configuration_items
    (configuration_item_id, business_unit_id, item_name, item_value, item_values)
VALUES (88007700000700, 77, 'BANK_ACCOUNTS', 'ALLPAY_BU77_PREPROCESSING_V1',
    '[{"sort_code":"123456","account_number":"12345678","name":"Allpay BU77 test bank 1"},
      {"sort_code":"234567","account_number":"23456789","name":"Allpay BU77 test bank 2"},
      {"sort_code":"456789","account_number":"45678901","name":"Allpay BU77 test bank 3"}]')
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
SELECT apr_id, defendant_account_id, 'APR'::public.t_apr_relationship_enum,
       'APR'::public.t_apr_category_enum, business_unit_id, apr_text, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM _allpay_aprs
ON CONFLICT (alternate_payment_reference_id) DO NOTHING;
UPDATE public.defendant_accounts
SET consolidated_account_type='M'
WHERE defendant_account_id=88007700000002;
UPDATE public.defendant_accounts
SET account_status='CS', consolidated_account_type='C'
WHERE defendant_account_id=88007700000003;

INSERT INTO public.defendant_transactions
    (defendant_transaction_id, defendant_account_id, posted_date, posted_by,
     transaction_type, text, associated_record_type, associated_record_id)
SELECT transaction_id, account_id, CURRENT_TIMESTAMP, 'ALLPAYSEED',
       transaction_type::public.t_defendant_transaction_type_enum, text,
       'defendant_accounts'::public.t_associated_record_type_enum, associated_id
FROM _allpay_consolidation
ON CONFLICT (defendant_transaction_id) DO NOTHING;

-- 000008 pays 99,999,999,999 pence; give account 4 one penny more debt.
-- Keep the account summary, original imposition and payment-terms balance aligned.
UPDATE public.defendant_accounts
SET amount_imposed=-1000000000.00, account_balance=-1000000000.00
WHERE defendant_account_id=88007700000004;
UPDATE public.impositions SET imposed_amount=-1000000000.00
WHERE imposition_id=88007700000604 AND defendant_account_id=88007700000004;
UPDATE public.payment_terms SET account_balance=-1000000000.00
WHERE payment_terms_id=88007700000504 AND defendant_account_id=88007700000004;

-- Prove both the payment-processing lookup and account-details view see the link.
-- This does not invoke payment allocation.
DO $verify$
BEGIN
    IF public.f_get_master_account_id(88007700000003) IS DISTINCT FROM 88007700000002::bigint
       OR NOT EXISTS (SELECT 1 FROM public.v_consolidated_accounts
                      WHERE master_account_id=88007700000002
                        AND child_account_id::bigint=88007700000003) THEN
        RAISE EXCEPTION 'Allpay master/child relationship was not established.';
    END IF;
    IF (SELECT count(*) FROM public.alternate_payment_references
        WHERE apr_text='TEST-APR-01' AND
          ((business_unit_id=77 AND defendant_account_id=88007700000001)
           OR (business_unit_id=78 AND defendant_account_id=88007800000001)))<>2 THEN
        RAISE EXCEPTION 'Allpay same-text APR fixtures were not established in both BUs.';
    END IF;
END
$verify$;
COMMIT;
