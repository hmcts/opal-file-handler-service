# Bruno API Collection

This directory contains the Bruno collections and local environment used to manually test the
`opal-file-handler-service` REST APIs. The collection currently provides a reusable request for
`GET /interface-files/{id}`.

Bruno is a fast, Git-friendly API client designed for teams that prefer version-controlled,
text-based API collections.

```text
bruno/
├── collections/
│   └── Get interface file.bru
├── environments/
│   └── local.bru
└── config.json
```

## Getting started

1. Install Bruno:

```bash
brew install --cask bruno
```

2. Open this `bruno` directory in Bruno.
3. Select the `local` environment.
4. Ensure the file-handler service is running on port `4075` and the user service is running on port `4555`.
5. Load the local fixture data if the request returns `404`.
6. Run `Get interface file.bru` and change the ID in `environments/local.bru` when needed.

The collection obtains a local token automatically for
`opal-test@dev.platform.hmcts.net` and stores it only as a Bruno runtime variable.

## Loading the local fixture data

The functional tests create and remove their database fixtures automatically. Bruno does not do
this, so load the same fixture manually before running the valid-ID request:

```bash
cd /Users/thathipammi/Opal/opal-file-handler-service

docker exec -i -e PGPASSWORD=opal-db-password opal-stack-opal-db-1 \
  psql -U opal-db-user -d opal-file-handler-db \
  < src/functionalTest/resources/db/interface-file-content/setup.sql
```

This creates interface file `9000000000000001`, which is used by the valid-ID request. The setup
script also creates the other interface-file records used by the functional tests.

To remove the fixture records afterwards, run:

```bash
docker exec -i -e PGPASSWORD=opal-db-password opal-stack-opal-db-1 \
  psql -U opal-db-user -d opal-file-handler-db \
  < src/functionalTest/resources/db/interface-file-content/cleanup.sql
```

The automated functional tests cover the valid ID, unknown ID, invalid token and feature-disabled
scenarios. This collection intentionally provides one authenticated request for hands-on API
exploration.

## Git and security guidelines

Commit the collection files and the local environment with an empty `BEARER_TOKEN`.

Do not commit populated bearer tokens or other sensitive values.

## Updating the collection

Keep the requests aligned with the functional tests and update the expected responses when the
API contract changes. The test IDs and user can be changed in `environments/local.bru` if the
local fixture data changes.
