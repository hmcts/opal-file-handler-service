# Automated Test Framework

## Summary

This repository now has a minimum viable functional/smoke test framework aligned to the existing
`opal-fines-service` approach, but scoped down to reusable infrastructure. The implementation lifts
the core execution model and helper patterns without carrying over fines-specific workflows.

The supplied file-handler design material and the current codebase point to a small initial surface:

- REST endpoints exposed by the service.
- authentication via the shared user-service test support flow.
- database and blob-storage connectivity for controlled functional-test fixtures and diagnostics.
- future SFTP checks and file-ingestion smoke coverage.
- future `/testing-support/**` endpoints for scheduler and ingestion control.

## What Was Copied Directly

- Gradle `functionalTest` and `smokeTest` source-set pattern.
- Cucumber-on-JUnit Platform runner model.
- Serenity reporting configuration.
- lightweight raw HTTP client for test-support and auth calls.
- environment-variable driven URL resolution.
- bearer-token acquisition through `OPAL_USER_SERVICE_API_URL` test-support endpoints.
- generic response assertion pattern.

## What Was Adapted

- package names changed from `uk.gov.hmcts.opal` to `uk.gov.hmcts.opal.filehandler`.
- scenario context reduced to generic request/response state instead of fines account-specific data.
- generic REST steps support `GET`, `POST`, and `PATCH` only.
- smoke coverage is limited to the current stable endpoints: `/` and `/health`.
- reusable DB, blob-storage, and SFTP utilities are available; interface-file content scenarios use tagged
  setup and cleanup hooks.
- test-support access is exposed through a simple client rather than concrete endpoint steps.

## What Was Not Carried Over

- fines domain workflows, request factories, and account-specific assertions.
- legacy/opal mode branching.
- feature-toggle-specific steps.
- fines data cleanup hooks and draft-account state handling.
- Zephyr/Cucumber post-processing tasks beyond the existing shared Gradle wiring.

## Recommended Structure

- `src/functionalTest/java/.../config`: environment and runtime settings.
- `src/functionalTest/java/.../auth`: bearer-token support.
- `src/functionalTest/java/.../steps`: reusable Cucumber glue.
- `src/functionalTest/java/.../support`: raw HTTP helpers.
- `src/functionalTest/java/.../db`: DB fixture and diagnostic utilities.
- `src/functionalTest/java/.../blob`: blob fixture utilities.
- `src/functionalTest/java/.../sftp`: reusable SFTP utilities.
- `src/functionalTest/java/.../testsupport`: future `/testing-support/**` client wrappers.
- `src/functionalTest/resources/features`: service-level functional and smoke scenarios.
- `src/smokeTest/java/...`: smoke runner only, reusing functional support classes.

## Gaps Still Outside The Framework

- SFTP smoke features once connection details and target paths are agreed.
- file-upload and scheduler-trigger test-support endpoints in the application.
- higher-level fixtures for Common Platform stubbing when those flows are added to tests.

## Improvements Over A Straight Copy

- the framework is intentionally generic and service-shaped instead of inheriting fines entities.
- execution is kept small enough to be extended incrementally.
- DB access remains environment-gated and is limited to tagged scenarios that own dedicated fixture IDs.
- blob access is limited to tagged scenarios and dedicated test-owned blob UUIDs.
- test-support endpoints are treated as the preferred future control plane instead of direct DB
  assertions.

## Serenity Reports

`./gradlew functional` aggregates the Serenity results and copies the HTML report to
`functional-test-report/index.html`.

`./gradlew smoke` aggregates the Serenity results and copies the HTML report to
`smoke-test-report/index.html`.

Both tasks also retain their JUnit XML and standard Gradle HTML reports under `build/` for CI
publication and diagnostics.

## Database-backed fixtures

The interface-file content feature creates and removes its own `interface_files` rows around every
scenario. Local runs default to `FUNCTIONAL_TEST_DB_URL=http://localhost:5432`,
`FUNCTIONAL_TEST_DB_USERNAME=opal-db-user`, and `FUNCTIONAL_TEST_DB_PASSWORD=opal-db-password`.
The HTTP-style local URL is converted to `jdbc:postgresql://localhost:5432/opal-file-handler-db`
before connecting. Explicit `FUNCTIONAL_TEST_DB_*` values still take precedence over all other
settings. For pull-request deployments, Jenkins reads the database settings from
`charts/opal-file-handler-service/values.dev.template.yaml`, resolves the Helm release database
host in the `opal` namespace, and exports the corresponding `OPAL_FILE_HANDLER_DB_*` application
variables. Cleanup targets only the dedicated IDs reserved in the fixture SQL.

## Blob-backed fixtures

The interface-file content feature uploads its BTECKOH workbook before every scenario and removes
it afterward. Local runs default to the `bteckoh-report` container in Azurite account
`devstoreaccount1` at `http://127.0.0.1:10000/devstoreaccount1`. The standard Azurite development
account key is used locally. `FUNCTIONAL_TEST_BLOB_CONTAINER_NAME`,
`FUNCTIONAL_TEST_BLOB_ACCOUNT_NAME`, `FUNCTIONAL_TEST_BLOB_ACCOUNT_KEY`, and
`FUNCTIONAL_TEST_BLOB_ENDPOINT` override these values for other environments.

For pull-request deployments, Jenkins reads the account name, resource group, and container from
`charts/opal-file-handler-service/values.dev.template.yaml`, resolves the provisioned account's
endpoint and key through Azure, and exports them as `FILE_STORE_STORAGE_ACCOUNT_NAME`,
`FILE_STORE_STORAGE_URL`, and `FILE_STORE_STORAGE_KEY`. The functional tests use those application
storage variables when their functional-test-specific overrides are not set.

## Phased Plan

### Phase 1: Done in this change

- establish Cucumber/Serenity runners.
- add reusable auth, HTTP, config, DB, SFTP, and test-support utilities.
- replace placeholder functional/smoke tests with smoke features.

### Phase 2: Next

- add authenticated `GET /interface-files` scenarios.
- add negative-auth scenarios using the token helpers.
- add first SFTP connectivity smoke test behind environment flags.

### Phase 3: When service support exists

- add `/testing-support` wrappers for scheduler triggering and ingestion orchestration.
- add end-to-end file-ingestion smoke tests.
- add controlled data-reset helpers where needed.

### Phase 4: Later hardening

- richer JSON/body assertion helpers.
- contract-style fixtures for Common Platform interactions.
- environment profiles for CI, staging, and local developer execution.
