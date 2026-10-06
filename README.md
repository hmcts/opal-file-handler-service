# opal-filehandler

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Note: Docker Compose V2 is highly recommended for building and running the application.
In the Compose V2 old `docker-compose` command is replaced with `docker compose`.

Create docker image:

```bash
  docker compose build
```

Run the distribution (created in `build/install/opal-filehandler` directory)
by executing the following command:

```bash
  docker compose up
```

This will start the API container exposing the application's port
(set to `4075` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4075/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

### Functional test tasks

Use the standard functional suite for normal functional coverage:

```bash / zsh
  ./gradlew clean functional
```

This runs the default functional suite and publishes the Serenity
functional report under `/functional-test-report/`.

## Nightly Jenkins pipeline

`Jenkinsfile_nightly` runs on weekdays using `H 07 * * 1-5`. The shared HMCTS nightly
pipeline performs checkout, build and dependency-check stages before running the
file-handler integration, staging functional and staging smoke suites.

Nightly parameters:

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `Integration` | `true` | Runs the Testcontainers-backed integration suite once. |
| `Functional` | `true` | Runs the functional suite against the staging deployment. |
| `Smoke` | `true` | Runs the smoke suite against the staging deployment. |
| `ZephyrExecution` | `false` | Creates Zephyr executions. Zephyr execution is also enabled automatically on Fridays. |

The staging functional suite loads database and blob-storage credentials and endpoints
from the Opal Key Vault, and reads the functional-test blob container from the staging
chart values. Functional fixture hooks manage their own setup and cleanup because the
nightly pipeline sets `FUNCTIONAL_TEST_DB_MANAGED_BY_PIPELINE=false`.

Nightly reports and artifacts:

- Integration publishes `Integration Tests Report` and archives `integration-output/`,
  including the JUnit 5 Zephyr report under `integration-output/zephyr/`.
- Functional publishes `Serenity Functional Test Report` and archives
  `functional-output/`, including its Cucumber Zephyr report.
- Smoke publishes `Serenity Smoke Test Report` and archives `smoke-output/`, including
  its Cucumber Zephyr report.
- A failed Gradle stage, missing report, missing Zephyr input or failed Zephyr execution
  marks the nightly build as failed after all selected suites have published their output.
- Master failures and subsequent fixes are reported to `#opal-nightly-builds`.

The HMCTS nightly organisation suppresses automatic SCM-triggered builds. After the
nightly job is first discovered, run the `master` job manually once with Zephyr disabled
to apply the cron trigger declared in `Jenkinsfile_nightly`. Subsequent weekday builds
will then be timer-triggered.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

### PO-6453 upload regression tests

Run the upload scenarios through the existing functional runner:

```bash
./gradlew functional '-Dcucumber.filter.tags=@JIRA-STORY:PO-6453'
./gradlew integration --tests '*AddInterfaceFileTest*'
```

`TEST_URL` must point to the file-handler application with the banking-interface feature enabled.
The existing `OPAL_USER_SERVICE_API_URL` token service must issue tokens trusted by that application.
The scenarios reuse `opal-test@dev.platform.hmcts.net` and the existing no-permission user
`opal-test-2@dev.platform.hmcts.net`. They require direct access to the application's database through
`FUNCTIONAL_TEST_DB_URL`, `FUNCTIONAL_TEST_DB_USERNAME` and `FUNCTIONAL_TEST_DB_PASSWORD` (or the existing
application database fallbacks), plus the existing `FUNCTIONAL_TEST_BLOB_*` settings targeting its
BTEckoh container. The container must already exist. Local defaults use PostgreSQL and Azurite.
A pipeline that only seeds data inside a database pod must also provide this direct database access;
these tests do not bypass persistence assertions when `FUNCTIONAL_TEST_DB_MANAGED_BY_PIPELINE` is set.

Each scenario generates a unique `po-6453-<UUID>.xlsx` filename and cleans up only its own database
records and blobs. The duplicate scenario explicitly changes its first upload to `SUCCESS` as a
fixture precondition; it does not claim to test downstream processing to that state.

The assertions use `201 Created`, matching the OpenAPI contract and supplied developer clarification.
The ticket's quoted E2E.01/E2E.03 text says `200` and should be reconciled. Only a prior `SUCCESS`
record qualifies as a duplicate; an `INGESTED` repeat creates another file. Invalid BTEckoh JSON is
expected to produce `201 FAILED`, not a successful JSON ingestion.

AC1 (disabled feature) and AC3 (blob upload failure/503 with database rollback) remain covered by the
existing isolated integration fixtures. Deployed E2E.02 needs a dedicated failure environment or an
approved fault-injection mechanism; the functional suite does not delete a shared blob container or
stop shared services. No deployed E2E.02 scenario is registered until that prerequisite is available.
The Create Interface File permission in the API description also differs from the implementation's
View Interface Files permission. The tests do not change application authorisation or claim to resolve
that mismatch. The linked TDIA Test and QA section was unavailable when this coverage was added.

Required-part integration regressions currently expose a product defect: omitting either `file` or
`metadata` raises `MissingServletRequestPartException`, which the shared servlet-exception handler
maps to 500 instead of the expected 400. Tracked in [PO-10908](https://hmcts.atlassian.net/browse/PO-10908). Only these two tests are temporarily
disabled with defect-linked reasons; their 400 assertions are preserved. Remove their `@Disabled`
annotations and rerun them when PO-10908 is fixed. Production code is unchanged.

PR functional tests run on a Jenkins VM outside Kubernetes. Before the dev functional stage,
Jenkins opens a loopback-only `kubectl port-forward` to that PR's PostgreSQL pod and sets
`FUNCTIONAL_TEST_DB_URL` to its dynamically allocated local port. The tunnel is stopped after
functional tests (including failures) and has a 45-minute maximum lifetime for aborted builds.
Database persistence assertions and scenario cleanup remain enabled; pipeline-managed shared
fixtures continue to use in-pod SQL. Staging and local connections are unchanged.
