# Automated Test Framework

## Summary

This repository now has a minimum viable functional/smoke test framework aligned to the existing
`opal-fines-service` approach, but scoped down to reusable infrastructure. The implementation lifts
the core execution model and helper patterns without carrying over fines-specific workflows.

The supplied file-handler design material and the current codebase point to a small initial surface:

- REST endpoints exposed by the service.
- authentication via the shared user-service test support flow.
- optional database connectivity for future diagnostics.
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
- reusable DB and SFTP utilities are present but intentionally not coupled into assertions yet.
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
- `src/functionalTest/java/.../db`: optional DB utilities.
- `src/functionalTest/java/.../sftp`: reusable SFTP utilities.
- `src/functionalTest/java/.../testsupport`: future `/testing-support/**` client wrappers.
- `src/functionalTest/resources/features`: service-level functional and smoke scenarios.
- `src/smokeTest/java/...`: smoke runner only, reusing functional support classes.

## Gaps Still Outside The Framework

- concrete authenticated API scenarios for `/interface-files` once controller/service behavior is implemented.
- SFTP smoke features once connection details and target paths are agreed.
- file-upload and scheduler-trigger test-support endpoints in the application.
- higher-level fixtures for Common Platform stubbing when those flows are added to tests.

## Improvements Over A Straight Copy

- the framework is intentionally generic and service-shaped instead of inheriting fines entities.
- execution is kept small enough to be extended incrementally.
- DB access remains opt-in and environment-gated, which matches the current delivery model.
- test-support endpoints are treated as the preferred future control plane instead of direct DB
  assertions.

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
