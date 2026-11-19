# Fines data for Allpay preprocessing

The first Allpay scenario automatically prepares these fixtures against the
separately configured **Fines database**. Setup runs once per test JVM, before
file-handler/SFTP setup. Other BAIS features do not trigger this seed.

- If none of the ten reserved defendant accounts exists, setup executes the
  BU80 base seed `01_seed_bu80_10_accounts.sql`.
- If all ten exist, setup skips the base script and validates their ownership
  through `02_setup_preprocessing.sql`, accepting original or canonical fixture
  account numbers.
- A partial base seed, occupied fixture IDs, conflicting APRs or bank mappings
  fails setup without replacing existing data. The base script also checks its
  associated reserved IDs before inserting anything.
- Setup always applies the repeatable supplemental script to ensure the three
  bank mappings, canonical references, APRs (including the BU78 duplicate-name fixture),
  the former-child CONSOLIDATED APR and
  the large-account balance exist. A database advisory lock
  serializes preparation across test JVMs and the manual setup/cleanup scripts.

The normal Fines migrations must already provide BU80, BU78, the BU80 court, result FCOMP,
and the schema. The tests do not provision a database or run application migrations.
No SQL from Downloads needs to be run manually. Scenario cleanup retains the
shared seed so later scenarios and subsequent test runs can reuse it.

For deliberate manual removal, `03_cleanup_preprocessing.sql` removes only the
supplemental bank/APR fixtures (and any owned consolidation links from the old fixture),
restores account 4 to its original £400 debt,
and restores original defendant account numbers;
it keeps the ten-account base seed. The next Allpay run reapplies the supplement.
Run that cleanup only after dependent tests/jobs have finished. The scripts are
also usable directly against Fines for investigation.

The supplemental setup gives the ten synthetic defendant accounts references
`80000001A` through `80000010A`, except account 3 retains `ZB80D0003` so
`80000003A` exists only as an APR. The canonical numbers match the translation
service's eight-digits plus letter pattern. It does not change shared numbering
sequences or populate `account_number_index`.

Synthetic `ZB80C0001` through `ZB80C0010` values are explicitly configured as APR
aliases to the corresponding seeded defendant accounts. They are creditor
numbers in the original seed; the aliases let existing Allpay fixture references
resolve deliberately rather than assuming creditor-account lookup occurs.
`TEST-APR-01` and the originator-name APR `ABCXYZ` both resolve to defendant
account `88008000000001`.

`TEST-APR-01` is deliberately present in both BU80 and BU78, which is permitted
by the `(business_unit_id, apr_text)` unique constraint. The existing BU80 APR
scenario (`000004.dat`) resolves to `88008000000001`. The duplicate-name scenario
(`000016.dat`) uses destination bank `678901/67890123` to resolve BU78 and must
produce account ID `88007800000001`, not the BU80 account.

The supplement creates one dedicated live Fine account `78000001A` in migrated
BU78, with £100 outstanding (`-100.00` in the database), £0 paid, and marker
`ALLPAY_BU78_APR_V1`. It is an account lookup target for preprocessing only;
no parties, impositions or payment allocation setup are created for this account.
The owned BU78 bank configuration is `88007800000700` and APR is `88007800000801`.
The setup validates ownership and both same-text APRs. Explicit cleanup removes
these BU78 supplemental rows and account while retaining the ten BU80 base accounts.

The fixture with no matching account (`80345678A`) and the unmapped destination
bank (`111111/22222222`) are intentionally absent from this setup. Missing
references and invalid codes remain negative cases. The mixed-code cheque
fixture retains supported codes `11` and `99`; the `11`/`44` totals-row fixture
verifies that file-handler removes `44` before Fines preprocessing.

The originator-name APR fixture has `ABCXYZ` in the 18-character name field and
`UNMATCHED-REF` in its reference field. The prefix/suffix fixture uses
`80000001A`, so its cases exercise pattern extraction and direct defendant
lookup instead of requiring separate APRs for every decorated string.

The former-child APR fixture (`a121_00000001_000006.dat`) contains a direct
payment reference for master account `80000002A` (ID `88008000000002`) and the
former child reference `80000003A`. No defendant account or account-number-index
entry may match `80000003A`. Seed account 3 keeps its original `ZB80D0003` number.
The APR (`88008000000813`, relationship `CONSOLIDATED`, category `ACC`) maps
`80000003A` to the master. Preprocessing retains the directly matched master
number and translates the former child reference to master ID `88008000000002`.

This fixture proves APR translation only; it does not execute consolidation,
create consolidation transactions, change consolidation statuses, or allocate payments.
When upgrading an existing fixture, run `03_cleanup_preprocessing.sql` first,
after dependent test jobs finish. Its guarded compatibility cleanup removes only
the old fixture's owned consolidation links before the new setup is applied.

The large-payment fixtures both contain `99,999,999,999` pence (£999,999,999.99):

| File | Reference / resolved account | Outstanding debt |
| --- | --- | --- |
| `a121_00000001_000008.dat` | `ZB80C0004` / `80000004A` (`88008000000004`) | £1,000,000,000.00 (`100,000,000,000` pence) |
| `a121_00000001_000009.dat` | `ZB80C0001` / `80000001A` (`88008000000001`) | £100.00 (`10,000` pence) |

Fines stores these debit balances in pounds as `-1000000000.00` and `-100.00`.
Supplemental setup enlarges account 4's amount imposed, account balance, original
imposition and payment-terms balance together, leaving account 1 unchanged.
It accepts only the original or already-enlarged unpaid fixture amounts, so it
cannot silently reset payment activity. Explicit cleanup restores account 4's
original `-400.00` amounts.

Account 4's debt exceeds the payment by one penny; account 1 represents a future
overpayment case. Current ingestion/preprocessing does not validate payment
amounts against debt, so both examples expect preprocessing success. The small
account is not an asserted failure within this suite's scope.

Before asserting downstream output, wait for the Fines job and its
`TRANSFORMED_JSON`; ordinary ingestion hooks remove the source rows and blobs
when their scenario ends. Full payment allocation is a separate operation via
`POST /interface-jobs/process` and is not performed by these scripts.

## Permanent functional checks

Successful Allpay scenarios now finish with
`Fines preprocessing completes for the stored "Allpay" file`. The step follows
that scenario's exact SOURCE -> SOURCE_JSON -> Fines result -> TRANSFORMED_JSON
IDs. It verifies the CREATED job, BU, related IDs, stored JSON checksum, payment
type, transaction count and amount total. Reference-focused scenarios also assert
transformed references. The duplicate scenario waits for the first result before
re-uploading and checks that its source JSON, Fines job and transformed ID remain
unchanged during the duplicate observation period.

Configure the Fines connection independently of the file-handler database:

| Environment variable | Local default |
| --- | --- |
| `FUNCTIONAL_TEST_FINES_DB_URL` | `jdbc:postgresql://localhost:5432/opal-fines-db` |
| `FUNCTIONAL_TEST_FINES_DB_USERNAME` | File-handler test database username |
| `FUNCTIONAL_TEST_FINES_DB_PASSWORD` | File-handler test database password |
| `FUNCTIONAL_TEST_FINES_BUSINESS_UNIT_ID` | `80` |
| `FUNCTIONAL_TEST_FINES_PREPROCESSING_TIMEOUT_SECONDS` | `60` |

The checks require the Fines seed above, running Fines and file-handler services,
and a working Service Bus consumer. They do not invoke payment allocation.
The Fines service must run with `INTERFACE_FILE_CONSUMER_ENABLED=true` and
`REPORT_CONSUMER_ENABLED=true`: the preprocessing listener uses the shared report
JMS connection factory. Both now default to false in Fines. Set them in the
service's Compose environment and recreate that service after changing them;
setting them only on the functional-test JVM does not enable the service consumer.
The transformed-upload API currently stores files as INGESTED; the check accepts
INGESTED or SUCCESS and reads the stored blob directly, while requiring the
source JSON to be SUCCESS. It never accepts FAILED transformed files.

Allpay cleanup now waits for queued source JSON to have a committed Fines result,
then removes only the single-file CREATED job linked to that source JSON, in a
Fines database transaction. It then removes that fixture's file-handler records
and source/transformed blobs. Jobs in other states or jobs with additional files
are preserved. Pending preprocessing timeouts preserve source data and fixture bank mappings
for diagnosis, and leave the remaining SFTP state untouched.
The Fines base seed, APRs and BANK_ACCOUNTS configuration remain installed across
scenarios; the explicit cleanup script removes those supplemental fixtures.
