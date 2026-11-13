-- Run against opal-fines-db after any dependent test jobs have been cleaned up.
-- Restores the base seed account numbers and removes its consolidation; keeps the original ten accounts.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(770077, 10);

-- Only remove the exact relationship installed by the supplemental setup.
-- Refuse partial/changed ownership so cleanup cannot reset unrelated account state.
DO $consolidation$
DECLARE
    owned_count integer;
BEGIN
    SELECT count(*) INTO owned_count FROM public.defendant_transactions
    WHERE posted_by='ALLPAYSEED' AND associated_record_type::text='defendant_accounts'
      AND ((defendant_transaction_id=88007700000901 AND defendant_account_id=88007700000003
            AND transaction_type::text='WRTOFF' AND text='CONSOLIDATED ALLPAY BU77 CHILD'
            AND associated_record_id='88007700000002')
        OR (defendant_transaction_id=88007700000902 AND defendant_account_id=88007700000002
            AND transaction_type::text='CONSOL' AND text='ALLPAY BU77 MASTER CONSOLIDATION'
            AND associated_record_id='88007700000003'));
    IF owned_count=0 AND NOT EXISTS (SELECT 1 FROM public.defendant_transactions
        WHERE defendant_transaction_id IN (88007700000901,88007700000902)) THEN
        RETURN;
    END IF;
    IF owned_count<>2 OR (SELECT count(*) FROM public.defendant_accounts
        WHERE business_unit_id=77 AND
          ((defendant_account_id=88007700000002 AND account_comments='CODEX_BU77_10_V1:0002'
            AND account_status::text='L' AND consolidated_account_type::text='M')
           OR (defendant_account_id=88007700000003 AND account_comments='CODEX_BU77_10_V1:0003'
               AND account_status::text='CS' AND consolidated_account_type::text='C')))<>2 THEN
        RAISE EXCEPTION 'Allpay consolidation ownership/state changed; preserving it.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_transactions t
        WHERE (t.defendant_account_id IN (88007700000002,88007700000003)
               OR (t.associated_record_type::text='defendant_accounts'
                   AND t.associated_record_id IN ('88007700000002','88007700000003')))
          AND (t.transaction_type::text='CONSOL'
               OR (t.transaction_type::text='WRTOFF' AND t.text LIKE 'CONSOLIDATED%'))
          AND t.defendant_transaction_id NOT IN (88007700000901,88007700000902)) THEN
        RAISE EXCEPTION 'Additional consolidation links found; preserving Allpay account state.';
    END IF;
    DELETE FROM public.defendant_transactions
    WHERE defendant_transaction_id IN (88007700000901,88007700000902);
    UPDATE public.defendant_accounts SET account_status='L', consolidated_account_type=NULL
    WHERE defendant_account_id IN (88007700000002,88007700000003);
END
$consolidation$;

-- Restore only the known, unpaid large-account fixture to its original £400 debt.
DO $large_account$
BEGIN
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
        RAISE EXCEPTION 'Allpay large-account amounts or ownership changed; preserving them.';
    END IF;
    UPDATE public.defendant_accounts SET amount_imposed=-400.00, account_balance=-400.00
    WHERE defendant_account_id=88007700000004;
    UPDATE public.impositions SET imposed_amount=-400.00
    WHERE imposition_id=88007700000604 AND defendant_account_id=88007700000004;
    UPDATE public.payment_terms SET account_balance=-400.00
    WHERE payment_terms_id=88007700000504 AND defendant_account_id=88007700000004;
END
$large_account$;

WITH fixtures AS (
    SELECT 88007700000800::bigint + n AS apr_id, 88007700000000::bigint + n AS defendant_account_id,
           'ZB77C' || lpad(n::text,4,'0') AS apr_text FROM generate_series(1,10) AS g(n)
    UNION ALL SELECT 88007700000811, 88007700000001, 'TEST-APR-01'
    UNION ALL SELECT 88007700000812, 88007700000001, 'ABCXYZ'
)
DELETE FROM public.alternate_payment_references a USING fixtures f
WHERE a.alternate_payment_reference_id=f.apr_id AND a.defendant_account_id=f.defendant_account_id
  AND a.apr_text=f.apr_text AND a.business_unit_id=77
  AND a.category::text='APR' AND a.relationship::text='APR';

DELETE FROM public.configuration_items
WHERE configuration_item_id=88007700000700 AND business_unit_id=77
  AND item_name='BANK_ACCOUNTS' AND item_value='ALLPAY_BU77_PREPROCESSING_V1';

WITH fixtures AS (
    SELECT n, 88007700000000::bigint+n AS defendant_account_id,
           (77000000+n)::text || 'A' AS account_number,
           'ZB77D' || lpad(n::text,4,'0') AS original_number,
           'CODEX_BU77_10_V1:' || lpad(n::text,4,'0') AS marker FROM generate_series(1,10) AS g(n)
)
UPDATE public.defendant_accounts a SET account_number=f.original_number
FROM fixtures f
WHERE a.defendant_account_id=f.defendant_account_id AND a.business_unit_id=77
  AND a.account_comments=f.marker AND a.account_number=f.account_number;
-- Dedicated BU78 preprocessing-only account and duplicate-name APR.
DELETE FROM public.alternate_payment_references
WHERE alternate_payment_reference_id=88007800000801 AND business_unit_id=78
  AND defendant_account_id=88007800000001 AND apr_text='TEST-APR-01'
  AND category::text='APR' AND relationship::text='APR';
DELETE FROM public.configuration_items
WHERE configuration_item_id=88007800000700 AND business_unit_id=78
  AND item_name='BANK_ACCOUNTS' AND item_value='ALLPAY_BU78_APR_V1';
DELETE FROM public.defendant_accounts
WHERE defendant_account_id=88007800000001 AND business_unit_id=78
  AND account_number='78000001A' AND account_comments='ALLPAY_BU78_APR_V1'
  AND amount_imposed=-100.00 AND amount_paid=0 AND account_balance=-100.00
  AND account_status::text='L' AND account_type::text='Fine';
COMMIT;
