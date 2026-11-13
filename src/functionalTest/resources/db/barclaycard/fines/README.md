# Independent Fines fixtures for Barclaycard preprocessing

The first Barclaycard scenario in each test JVM runs this directory's `setup.sql`
through `BarclaycardFinesSeedFixture`. It does not invoke Allpay setup or cleanup,
read Allpay fixture resources, or reuse Allpay accounts/APRs. The only Fines
reference-data prerequisite is migrated BU77 and the normal application schema.
No manual SQL execution or prior Allpay test run is required.

The `test-data/bacs/barclaycard-unique.dat` resource is uploaded under
Barclaycard's configured filename `a121_00010065_317608.dat`. It contains:

| Code | Payment | Source reference | Expected transformed reference | Seeded account / debt |
| --- | --- | --- | --- | --- |
| `99` | 1,500 pence (£15) | `77100001A` | `77100001A` | `88007710000001` / £500 |
| `93` | 500 pence (£5) | APR `BC-APR-01` | Account ID `88007710000002` | `77100002A` / £600 |

Both dedicated accounts belong to BU77. The second code normalizes to `99`;
the direct reference is retained while the APR resolves to an internal account ID.
Fines stores the debts as `-500.00` and `-600.00` pounds, with £0 paid. These are
preprocessing lookup fixtures: no parties, impositions or payment-allocation data
are created. Payment allocation remains outside this test suite's scope.

The APR ID is `88007710000801`. Account ownership markers are
`BARCLAYCARD_BU77_PREPROCESSING_V1:01` and `:02`. The dedicated Fines
`BANK_ACCOUNTS` mapping resolves destination `560034/27048528` to BU77, using ID
`88007700000701` and marker `BARCLAYCARD_BU77_PREPROCESSING_V1`.
Setup is transactional and repeatable, using its own advisory lock `(770077,6427)`.
It refuses occupied IDs, account references/APRs, changed amounts or ambiguous bank
mappings. It does not reset sequences or alter `account_number_index`.

The valid-file scenario checks source JSON, waits for its exact Fines CREATED job
and transformed JSON, then asserts BU77, account references, normalized codes,
individual amounts, record count 2 and total 2,000 pence. Generic assertion and
job-cleanup helpers are shared with Allpay; every cleanup targets only the exact
source JSON IDs produced by the current interface's scenario.

The duplicate scenario verifies that the original Fines result is reused.
Scenario cleanup waits for preprocessing, removes the scenario's single-file
CREATED job, and then removes its file-handler records/blobs. Pending preprocessing
preserves input and file-handler mappings for diagnosis. Negative scenarios
without successful source JSON do not wait for a Fines result.

Barclaycard's accounts, APR and Fines bank mapping are retained between scenarios
and runs. This directory's `cleanup.sql` optionally removes all four dedicated
fixture rows (two accounts, one APR and one bank mapping) after dependent jobs
finish. It preserves Allpay accounts, APRs and bank mappings. Changed ownership or
financial state causes cleanup to fail and roll back; other account dependencies
also prevent deletion through the database foreign keys. The next Barclaycard
run recreates its own fixtures.

Fines/file-handler services, Service Bus, PostgreSQL, SFTP and Azurite must be
running. Configure Fines separately using `FUNCTIONAL_TEST_FINES_DB_URL`,
`FUNCTIONAL_TEST_FINES_DB_USERNAME` and `FUNCTIONAL_TEST_FINES_DB_PASSWORD`.
The local URL default is `jdbc:postgresql://localhost:5432/opal-fines-db`.
Fines requires `INTERFACE_FILE_CONSUMER_ENABLED=true` and
`REPORT_CONSUMER_ENABLED=true` for its preprocessing listener/shared JMS factory.

Run this feature with:

```shell
./gradlew functional '-Dcucumber.filter.tags=@PO-6427'
```
