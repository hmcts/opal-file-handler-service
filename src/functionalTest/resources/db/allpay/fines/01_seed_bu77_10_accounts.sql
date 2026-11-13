-- Standalone Codex fixture: CODEX_BU77_10_V1
-- Prepared 2026-11-04 from opal-fines-service master 0aa7f5f8e.
-- PostgreSQL 17; run the ENTIRE file as one script, outside an existing transaction.
-- No psql-only commands. On any error, issue ROLLBACK before retrying.
-- This is development/test data. No existing BU77 accounts or reference rows are updated.
-- Creates 10 live adult Fine accounts, each owing compensation to its own minor creditor.
-- Seed is deliberately insert-only. Rerunning it refuses collisions; clean up first.
BEGIN;
SET LOCAL search_path = pg_catalog, public, pg_temp;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';
-- Serializes this script pair only; does not lock the entire application.
SELECT pg_advisory_xact_lock(770077, 10);

-- Exact fixture ID sets; the same manifest is embedded in both files.
CREATE TEMP TABLE _bu77_fixture ON COMMIT DROP AS
SELECT n,
       88007700000000::bigint + n AS defendant_account_id,
       88007700000100::bigint + n AS defendant_party_id,
       88007700000200::bigint + n AS creditor_account_id,
       88007700000300::bigint + n AS creditor_party_id,
       88007700000400::bigint + n AS defendant_account_party_id,
       88007700000500::bigint + n AS payment_terms_id,
       88007700000600::bigint + n AS imposition_id,
       'ZB77D' || lpad(n::text, 4, '0') AS defendant_account_number,
       'ZB77C' || lpad(n::text, 4, '0') AS creditor_account_number,
       'ZZBU77SEED' || lpad(n::text, 4, '0') AS case_reference,
       'CODEX_BU77_10_V1:' || lpad(n::text, 4, '0') AS marker,
       -100.00::numeric * n AS amount_imposed
FROM generate_series(1, 10) AS g(n);

DO $preflight$
DECLARE r record; occupied boolean; sequence_value bigint;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.business_units WHERE business_unit_id = 77) THEN
        RAISE EXCEPTION 'BU77 is missing. Apply the normal reference migrations first.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.courts WHERE court_id = 770000000001 AND business_unit_id = 77) THEN
        RAISE EXCEPTION 'Expected BU77 court 770000000001 is missing; no data inserted.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.results WHERE result_id = 'FCOMP') THEN
        RAISE EXCEPTION 'Result FCOMP is missing; no data inserted.';
    END IF;
    -- The standard dev sequences are already above this fixture block. Refuse
    -- a differently provisioned database rather than changing any shared sequence.
    FOR r IN SELECT * FROM (VALUES
      ('party_id_seq',88007700000310::bigint),
      ('creditor_account_id_seq',88007700000210::bigint),
      ('defendant_account_id_seq',88007700000010::bigint),
      ('defendant_account_party_id_seq',88007700000410::bigint),
      ('payment_terms_id_seq',88007700000510::bigint),
      ('imposition_id_seq',88007700000610::bigint)
    ) AS x(sequence_name,reserved_max) LOOP
        EXECUTE format('SELECT last_value FROM public.%I',r.sequence_name) INTO sequence_value;
        IF sequence_value < r.reserved_max OR NOT EXISTS (
            SELECT 1 FROM pg_sequence WHERE seqrelid=to_regclass('public.'||r.sequence_name) AND seqincrement>0
        ) THEN
            RAISE EXCEPTION 'Sequence % is not above the reserved fixture block with positive increment. No changes made; this script never resets shared sequences.',r.sequence_name;
        END IF;
    END LOOP;
    -- Check every reserved primary key, not just account IDs.
    FOR r IN
        SELECT * FROM (VALUES
          ('defendant_accounts','defendant_account_id','defendant_account_id'),
          ('parties','party_id','defendant_party_id'),
          ('parties','party_id','creditor_party_id'),
          ('debtor_detail','party_id','defendant_party_id'),
          ('creditor_accounts','creditor_account_id','creditor_account_id'),
          ('defendant_account_parties','defendant_account_party_id','defendant_account_party_id'),
          ('payment_terms','payment_terms_id','payment_terms_id'),
          ('impositions','imposition_id','imposition_id')
        ) AS x(table_name, key_column, manifest_column)
    LOOP
        EXECUTE format('SELECT EXISTS (SELECT 1 FROM public.%I t JOIN pg_temp._bu77_fixture f ON t.%I=f.%I)',
                       r.table_name,r.key_column,r.manifest_column) INTO occupied;
        IF occupied THEN
            RAISE EXCEPTION 'Reserved ID already exists in %. No changes made. If already seeded, run the paired cleanup first.', r.table_name;
        END IF;
    END LOOP;
    IF EXISTS (
        SELECT 1 FROM public.defendant_accounts a JOIN _bu77_fixture f
          ON a.account_number IN (f.defendant_account_number,f.creditor_account_number) WHERE a.business_unit_id=77
        UNION ALL
        SELECT 1 FROM public.creditor_accounts a JOIN _bu77_fixture f
          ON a.account_number IN (f.defendant_account_number,f.creditor_account_number) WHERE a.business_unit_id=77
        UNION ALL
        SELECT 1 FROM public.account_number_index a JOIN _bu77_fixture f
          ON a.account_number IN (f.defendant_account_number,f.creditor_account_number) WHERE a.business_unit_id=77
    ) THEN
        RAISE EXCEPTION 'Reserved BU77 account number already exists. No changes made.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.defendant_accounts a JOIN _bu77_fixture f
               ON a.prosecutor_case_reference=f.case_reference WHERE a.business_unit_id=77) THEN
        RAISE EXCEPTION 'Reserved BU77 prosecutor case reference already exists. No changes made.';
    END IF;
END
$preflight$;

INSERT INTO public.parties
    (party_id, organisation, surname, forenames, title, address_line_1,
     address_line_2, postcode, account_type, birth_date, email_1, last_changed_date)
SELECT defendant_party_id, false, 'ZZBU77SeedDef' || lpad(n::text,4,'0'),
       'SeedPerson' || lpad(n::text,4,'0'), 'Mx', n || ' ZZBU77 Seed Lane',
       marker, 'ZZ9 9ZX', 'Defendant', DATE '1985-01-01' + (n-1),
       'zb77d' || lpad(n::text,4,'0') || '@example.invalid', CURRENT_TIMESTAMP
FROM _bu77_fixture;

INSERT INTO public.parties
    (party_id, organisation, surname, forenames, title, address_line_1,
     address_line_2, postcode, account_type, email_1, last_changed_date)
SELECT creditor_party_id, false, 'ZZBU77SeedCred' || lpad(n::text,4,'0'),
       'SeedCreditor' || lpad(n::text,4,'0'), 'Mx', n || ' ZZBU77 Creditor Lane',
       marker, 'ZZ9 9ZX', 'Creditor',
       'zb77c' || lpad(n::text,4,'0') || '@example.invalid', CURRENT_TIMESTAMP
FROM _bu77_fixture;

INSERT INTO public.debtor_detail
    (party_id, employee_reference, document_language, hearing_language)
SELECT defendant_party_id, marker, 'EN', 'EN' FROM _bu77_fixture;

-- Updated 2026-11-05: use the same ten bank-value pairs as the existing
-- BU77 development creditors (99000000000801 through 99000000000810).
-- Keep these as text so leading zeroes survive. No BU bank configuration is changed.
WITH fixture_banks(n, bank_sort_code, bank_account_number) AS (
    VALUES (1,  '123456', '12345678'),
           (2,  '234567', '23456789'),
           (3,  '345678', '34567890'),
           (4,  '456789', '45678901'),
           (5,  '567890', '56789012'),
           (6,  '678901', '67890123'),
           (7,  '789012', '78901234'),
           (8,  '890123', '89012345'),
           (9,  '901234', '90123456'),
           (10, '012345', '01234567')
)
INSERT INTO public.creditor_accounts
    (creditor_account_id, business_unit_id, account_number, creditor_account_type,
     prosecution_service, minor_creditor_party_id, repayment, hold_payout,
     pay_by_bacs, bank_sort_code, bank_account_number, bank_account_name,
     bank_account_reference, bank_account_type, version_number, last_changed_date)
SELECT creditor_account_id, 77, creditor_account_number, 'MN', false,
       creditor_party_id, false, false, true, b.bank_sort_code, b.bank_account_number,
       'ZB77 Creditor ' || lpad(f.n::text,4,'0'), creditor_account_number,
       '1', 0, CURRENT_TIMESTAMP
FROM _bu77_fixture f JOIN fixture_banks b USING (n);

INSERT INTO public.defendant_accounts
    (defendant_account_id, business_unit_id, account_number, imposed_hearing_date,
     imposing_court_id, amount_imposed, amount_paid, account_balance, account_status,
     enforcing_court_id, last_hearing_court_id, last_hearing_date, last_changed_date,
     originator_name, originator_type, allow_writeoffs, allow_cheques,
     cheque_clearance_period, credit_trans_clearance_period, collection_order,
     payment_card_requested, prosecutor_case_reference, account_type,
     account_comments, account_note_1, version_number, imposed_by_name)
SELECT defendant_account_id, 77, defendant_account_number, DATE '2026-11-01',
       770000000001, amount_imposed, 0.00, amount_imposed, 'L',
       770000000001, 770000000001, DATE '2026-11-01', CURRENT_TIMESTAMP,
       'Codex BU77 standalone seed', 'NEW', false, true, 14, 21, false,
       false, case_reference, 'Fine', marker,
       'Synthetic fixture; remove using the paired cleanup script.', 0, 'Codex BU77 seed'
FROM _bu77_fixture;

INSERT INTO public.defendant_account_parties
    (defendant_account_party_id, defendant_account_id, party_id, association_type, debtor)
SELECT defendant_account_party_id, defendant_account_id, defendant_party_id,
       'Defendant', true FROM _bu77_fixture;

INSERT INTO public.payment_terms
    (payment_terms_id, defendant_account_id, posted_date, posted_by, posted_by_name,
     terms_type_code, effective_date, instalment_period, instalment_amount,
     instalment_lump_sum, extension, account_balance, active)
SELECT payment_terms_id, defendant_account_id, DATE '2026-11-01', 'ZB77SEED',
       'Codex BU77 seed', 'I', DATE '2026-11-01', 'M', n * 10.00,
       0.00, false, amount_imposed, true FROM _bu77_fixture;

INSERT INTO public.impositions
    (imposition_id, defendant_account_id, posted_date, posted_by, posted_by_name,
     result_id, imposing_court_id, imposed_date, imposed_amount, paid_amount,
     offence_title, offence_code, creditor_account_id, unit_fine_adjusted, completed)
SELECT imposition_id, defendant_account_id, DATE '2026-11-01', 'ZB77SEED',
       'Codex BU77 seed', 'FCOMP', 770000000001, DATE '2026-11-01', amount_imposed, 0.00,
       'ZZBU77 standalone compensation fixture', 'ZB77SEED', creditor_account_id, false, false
FROM _bu77_fixture;

-- No sequence resets or account_number_index changes: synthetic numbers must not
-- influence f_get_account_number, and these IDs sit below the existing dev sequences.
SELECT a.defendant_account_id, a.account_number, p.surname, a.account_balance,
       c.creditor_account_id, c.account_number AS creditor_account_number,
       c.pay_by_bacs, c.bank_sort_code, c.bank_account_number
FROM _bu77_fixture f
JOIN public.defendant_accounts a USING (defendant_account_id)
JOIN public.parties p ON p.party_id=f.defendant_party_id
JOIN public.creditor_accounts c USING (creditor_account_id)
ORDER BY f.n;
COMMIT;
