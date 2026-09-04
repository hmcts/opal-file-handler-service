# PO-6382 EI1 coverage and environment evidence

## Requirements reviewed

This review uses two text extracts supplied on 4 September 2026:

- External banking interfaces functional overview, created by Ben Owens and updated 13 May 2026: retain original reports for reconciliation and download; CAPS and BTEckoh reports are global rather than Business Unit-specific; failed files can be resubmitted, while successful files must not be processed again.
- External integration testing guidance, created by Calvin Chu and updated by Kamala Chadalawada on 3 September 2026: use representative interface test files and verify delivery to the correct Azure Blob storage location; perform environment integration testing with the appropriate accounts and reference data where the interface processes payments.

These are extracts, not the complete TDIA, data-store specification or LLD. Neither extract defines custom Azure metadata tags, exact deployed container names, or the expected domain value for each report. The existing MAINTENANCE domain assertion reflects the implementation and needs confirmation against the report-specific LLD.

The functional overview labels the CAPS format as `.xlsx` in its table but calls it an XML document in the description. Tests use XML, matching the description and existing processor. Confirm this documentation inconsistency; do not substitute an XLSX fixture without a contract change.

## What the automation proves

The suite uses the production batch application and real SFTP, PostgreSQL and Azure SDK operations against disposable local services. Azurite emulates Azure Blob Storage and the SFTP container simulates BAIS. Assertions read independently persisted records and downloaded bytes; outcomes are not mocked as successful.

| Requirement | Automated evidence |
| --- | --- |
| Correct report destination | Exactly one new blob in the configured `caps-report` or `bteckoh-report` container; every other container and existing blob remains unchanged. |
| Original file retained | Bytes downloaded from blob storage equal the source fixture. Azure Content-MD5 equals the database checksum and the fixture checksum; blob size equals the source size. |
| Metadata links to the physical file | Filename, source, target, type, status, domain, timestamp and filestore UUID are checked. The UUID identifies the actual blob, rather than assuming the original filename is its storage name. |
| Global reports | No Business Unit assignment and no payment type in the persisted record. This is not proof of UI filter behaviour. |
| Duplicate handling | SUCCESS and DUPLICATE records share the original UUID and checksum; no blob is added or overwritten; source duplicate is removed. |
| Invalid and disabled flows | Unsupported filenames and disabled jobs leave storage unchanged. Invalid contents produce an explained FAILED record with a timestamp/checksum, no blob reference, and a retained source file. |
| Retry after correcting a file | Replacing invalid content with a valid report permits successful ingestion. The previous failed attempt remains identifiable and the original valid bytes are stored. |
| Repeated invalid submission | The latest attempt remains FAILED, the earlier matching failure is FAILED_SUPERSEDED, and neither attempt creates a blob or deletes the source. |
| Listing and download | Related Spring integration tests list a newly ingested report by source/status, compare the returned metadata to its record, and download the original bytes through InterfaceFilesService. These are service integration checks, not browser or authenticated HTTP E2E tests. |

The BDD features are `src/functionalTest/resources/features/bais/CapsReportIngestion.feature` and `BTEckohReportIngestion.feature`: nine scenarios each, 18 total. Existing Given/When/Then step definitions and Serenity reporting are reused.

Business metadata is held in `interface_files`. Azure blob properties such as Content-MD5, size and ETag are separate. No custom `x-ms-meta-*` tags are asserted because no such contract was supplied. Serenity attaches actual database metadata and blob-property values to the relevant steps.

## What remains outside this local evidence

- Delivery through the deployed BAIS route, Azure identity/RBAC, firewall/network paths, deployed configuration, and the scheduled job.
- PO-6454's HTTP trigger and testing-support flag contract; the checkout used by this PR has no trigger controller. Local automation invokes the existing batch entry point.
- Viewer visibility when a Business Unit filter changes. The current listing API has no Business Unit filter, and the supplied extracts do not provide the frontend implementation or acceptance fixtures.
- Complete authentication/permission and viewer upload/download behaviour.
- Payment posting, transformations, account consolidation, APR lookups and reference-data reconciliation for other banking interfaces. BTEckoh REPORT is distinct from the BTEckoh payment interface. These flows must not be claimed from EI1 report-transfer results.
- Staging or dev/master SIT. Local passes are not evidence of deployed connectivity.
- A dedicated CI invocation of `bin/test-ei1.sh`; ordinary deployed functional runs intentionally exclude `@EI1`.

## Staging/SIT handover — NOT RUN

Use an agreed test window and approved synthetic or sanitised samples. Record the environment, deployed commit, actual configured storage account/container, job name, feature flags and execution time. Do not point the destructive local fixture/reset script at a shared environment.

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
JAVA_HOME=<JDK21> bin/test-ei1.sh
INTEGRATION_WIREMOCK_PORT=<available-port> JAVA_HOME=<JDK21> ./gradlew --no-daemon build checkstyleFunctionalTest
```

Serenity output: `functional-test-report/index.html`. JUnit output: `build/test-results/functional`. Report attachments show the metadata and blob properties actually observed in the run.

## Verified on 4 September 2026

- `bin/test-ei1.sh`: 18 EI1 scenarios passed, zero failures. The tag filter excluded 14 unrelated functional scenarios.
- `./gradlew --no-daemon build checkstyleFunctionalTest`: BUILD SUCCESSFUL; 226 unit tests and 47 integration tests passed, including the new listing/download assertions. All build style checks passed.
- An available `INTEGRATION_WIREMOCK_PORT` was supplied to avoid the local stack's port 4553.
- Staging/SIT and viewer Business Unit filtering remain NOT RUN.
