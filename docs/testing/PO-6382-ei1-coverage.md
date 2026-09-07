# PO-6382 EI1 coverage and environment evidence

## Requirements reviewed

This review uses two text extracts supplied on 4 September 2026:

- External banking interfaces functional overview, created by Ben Owens and updated 13 May 2026: retain original reports for reconciliation and download; CAPS and BTEckoh reports are global rather than Business Unit-specific; failed files can be resubmitted, while successful files must not be processed again.
- External integration testing guidance, created by Calvin Chu and updated by Kamala Chadalawada on 3 September 2026: use representative interface test files and verify delivery to the correct Azure Blob storage location; perform environment integration testing with the appropriate accounts and reference data where the interface processes payments.

These are extracts, not the complete TDIA, data-store specification or LLD. Neither extract defines custom Azure metadata tags, exact deployed container names, or the expected domain value for each report. The existing MAINTENANCE domain assertion reflects the implementation and needs confirmation against the report-specific LLD.

The functional overview labels the CAPS format as `.xlsx` in its table but calls it an XML document in the description. Tests use XML, matching the description and existing processor. Confirm this documentation inconsistency; do not substitute an XLSX fixture without a contract change.

## Automated coverage and pipeline placement

The additional cases extend the existing CAPS and BTEckoh Spring integration tests. They use the
repository's shared Testcontainers configuration (SFTP, PostgreSQL and Azurite), real processor services,
repositories and blob clients. External services are simulated locally; no deployed services are contacted.

| Requirement | Integration evidence |
| --- | --- |
| Valid report ingestion | SUCCESS metadata, original blob checksum and size, source removal, listing and original-byte download through InterfaceFilesService. |
| Global report metadata | No Business Unit or payment type on the persisted report; source, target, type, domain, filename and UUID checked. |
| Duplicate report | Ingest an original before resubmission; the DUPLICATE row reuses its UUID. Blob names and ETags prove the duplicate creates or overwrites nothing while a different valid report still succeeds. |
| No new files | Existing no-file test also asserts unchanged blob storage. |
| Disabled feature flags | Existing service integration cases cover disabled flags; no duplicate functional cases are added. |
| Unsupported filename | No database record or blob change; source file retained. |
| Malformed report and repeated retry | No blob upload; errors recorded; source retained. The first FAILED becomes FAILED_SUPERSEDED and a new FAILED remains traceable. |
| Corrected report | Replacing malformed bytes with a valid report succeeds and preserves the earlier failed attempt; original bytes can be downloaded. |

`./gradlew integration` runs these detailed cases without a custom script or tag. The existing `check` task
depends on `integration`, and `Jenkinsfile_CNP` publishes the integration results after the test stage.

Two focused Serenity/Cucumber scenarios provide deployed journey coverage: one for CAPS and one for
BTEckoh. Each uploads an approved fixture to the pipeline-provisioned BAIS SFTP account, calls
`POST /testing-support/automated-jobs/{name}`, asserts `202`, and verifies the SUCCESS metadata, original
blob bytes and removal of the BAIS source. The PR pipeline supplies the generated Azure SFTP host/users;
the master pipeline uses the staging BAIS secrets. Both use the existing functional-test task and reports.
No separate Compose infrastructure, local batch JVM, `-Pei1` switch or functional-test exclusion is used.

The earlier 18-scenario screenshots describe a superseded local suite and are not evidence for the revised
functional or integration suites.

Business metadata is stored in `interface_files`; blob Content-MD5, size and ETag are storage properties.
No custom Azure metadata tags are asserted because no such contract was supplied.

## Outstanding deployed verification

The focused functional scenarios exercise the deployed BAIS route, Azure storage and PO-6454 trigger in
the PR/master functional pipelines. Scheduled execution is not covered. Viewer Business Unit filtering
and authenticated UI downloads remain separate verification. Payment transformation and posting for
other interfaces are outside EI1.

## Staging/SIT handover — NOT RUN

Use an agreed test window and approved synthetic or sanitised samples. Record the environment, deployed commit, actual configured storage account/container, job name, feature flags and execution time. Use only environment-owner-approved fixture setup and cleanup.

| Test step | Test data | Expected result | Evidence to retain |
| --- | --- | --- | --- |
| Submit one valid report through the agreed BAIS route and trigger the deployed job | Approved CAPS XML and BTEckoh XLSX; record original filename, byte size and MD5 | Correct interface container receives one file; SUCCESS row links to the blob UUID; original bytes preserved | Sanitised job log, database row, Azure container/blob properties, downloaded-file checksum comparison |
| Inspect metadata and download the report in the viewer | The newly ingested reports | Displayed filename/source/status/time correspond to the stored row; downloaded file matches the original | Viewer metadata screenshot, row ID/UUID and checksum comparison |
| Change Business Unit filters | Same global reports; agreed authorised test user | CAPS and BTEckoh reports remain visible when Business Unit filters change | Screenshots identifying both filter values and the same report IDs |
| Resubmit the successful file | Identical filename and bytes | No new blob or processing; duplicate outcome recorded | Before/after blob names and ETags, SUCCESS/DUPLICATE rows |
| Run with no new files or an agreed disabled job flag | Controlled test window | No new blobs; source retained when disabled | Flag/job evidence and before/after records and blobs |
| Submit invalid content, then correct and resubmit | Accepted filename with malformed content, then valid content | Invalid file creates no blob; error is traceable; valid resubmission succeeds | Error metadata, retained source, successful retry and blob evidence |

Do not include storage keys, tokens, private keys or real financial/customer data in Jira screenshots or logs. Environment provisioning, representative test data and any shared-state cleanup must be coordinated with the environment owner.

## Local execution

```bash
JAVA_HOME=<JDK21> ./gradlew --no-daemon build checkstyleFunctionalTest
```

If the default WireMock port is occupied, set `INTEGRATION_WIREMOCK_PORT` to an available port.
JUnit results are in `build/test-results/integration`; the HTML report is in `build/reports/tests/integration`.
Staging/SIT and viewer filtering remain NOT RUN. See the PR validation section for the latest run totals.
