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

## EI1 local end-to-end tests (PO-6382)

The BAIS features use the existing Serenity/Cucumber `functional` task and run the
real Java 21 batch application against disposable PostgreSQL, SFTP and Azurite
containers. No deployed services, test users or LaunchDarkly connection are needed.
Docker Compose V2 and `JAVA_HOME` pointing to JDK 21 are required.

```bash
bin/test-ei1.sh
```

The script builds the application, allocates random loopback ports, migrates a fresh
database, runs both report features sequentially and removes its containers/volumes
on exit. It does not use the existing local Opal stack. Fixtures are reset before and
after every scenario. Blob names and ETags detect new uploads and overwrites; happy
paths also verify downloaded bytes, metadata and removal from SFTP.

The complete acceptance run currently exposes PO-6382 gaps: duplicates are uploaded
as new blobs, and supported filenames containing malformed data are uploaded without
content validation. These four scenarios have `@EI1AcceptanceGap`; they are real,
failing assertions, not ignored tests. To run only the currently supported behaviour:

```bash
bin/test-ei1.sh '@EI1 and not @EI1AcceptanceGap'
```

The ordinary deployed `functional` suite excludes `@EI1`. The script explicitly
selects it with `-Dcucumber.filter.tags` and supplies isolated local infrastructure.
Do not set `FUNCTIONAL_TEST_EI1_ISOLATED` against a shared environment: fixture setup
replaces the named BAIS files and removes their database/blob records.

Reports: `build/test-results/functional`, `functional-test-report/index.html`.
Bootstrap diagnostics: `build/ei1-bootstrap.log`.

PO-6454 is not implemented in this checkout: there is no job-trigger controller,
the OpenAPI path is `/test-support/automated-jobs/{name}`, and
`TESTING_SUPPORT_ENDPOINTS_ENABLED` currently defaults to `true`. Consequently this
suite uses the existing batch JVM entry point; it does not claim to verify the
`/testing-support/automated-jobs/{name}` HTTP 202 contract or deployed dev/master jobs.
