# Bruno API Collection

This directory contains the Bruno collections and local environment used to manually test the
`opal-file-handler-service` REST APIs. The collection provides requests for the interface-file
APIs and manual processing requests for each supported local BAIS file type.

Bruno is a fast, Git-friendly API client designed for teams that prefer version-controlled,
text-based API collections.

```text
bruno/
├── collections/
│   ├── Get interface file/
│   │   └── Get interface file.bru
│   ├── Get interface files/
│   │   ├── Get interface files.bru
│   │   ├── Get interface files - business unit focused.bru
│   │   ├── Get interface files - multiple types.bru
│   │   ├── Get interface files - multiple excluded statuses.bru
│   │   └── Get interface files - excluded target.bru
│   └── Process files/
│       ├── Process CAPS report.bru
│       ├── Process BTEckoh report.bru
│       ├── Process ALLPAY file.bru
│       ├── Process DWP file.bru
│       ├── Process NatWest file.bru
│       ├── Process Barclaycard file.bru
│       ├── Process BTEckoh file.bru
│       ├── Process Jacobs file.bru
│       └── Process CDER file.bru
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
6. Run either collection request. Change the IDs or filter variables in `environments/local.bru` when needed.

To process a fixture manually:

1. Copy the fixture into the matching local SFTP directory.
2. Ensure the corresponding transfer feature flag is enabled and its blob container exists.
3. Run the matching `Process ...` request.
4. Verify the resulting records using `Get interface files - filters`.

For the first PO-8669 happy-path filter check, set `businessUnitCode` to `BC12` in the local
environment and run `Get interface files - business unit focused`. This request deliberately uses
only the `business_unit_code` query parameter and verifies that matching results are returned.

The `Get interface files` folder contains focused requests for the PO-8669 filters:
business-unit code, multiple type values, multiple excluded statuses, and excluded target.

The available processing requests and their job names are:

| Request | Job name | SFTP directory |
| --- | --- | --- |
| Process CAPS report | `CAPSReport` | `CAPS-report` |
| Process BTEckoh report | `BTEckohReport` | `BTEckoh-report` |
| Process ALLPAY file | `AllpayFileTransferJob` | `AllPay` |
| Process DWP file | `DWPFileTransferJob` | `DWP` |
| Process NatWest file | `NatWestFileTransferJob` | `NATWEST` |
| Process Barclaycard file | `BarclaycardFileTransferJob` | `BARCLAYCARD` |
| Process BTEckoh file | `BTEckohFileTransferJob` | `BTEckoh` |
| Process Jacobs file | `JacobsFileTransferJob` | `Jacobs` |
| Process CDER file | `CderFileTransferJob` | `CDER` |

Each processing request obtains a local bearer token for `userEmail` automatically and includes
the required SHA-512 digest for the empty POST body. A `202` response means that the job was
accepted; check the service logs and database for the eventual processing status.

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
scenarios, as well as the list endpoint filters. The filtered list request asserts the response
status, result count, supported types, excluded statuses and targets, and business-unit code.

## Git and security guidelines

Commit the collection files and the local environment with an empty `BEARER_TOKEN`.

Do not commit populated bearer tokens or other sensitive values.

## Updating the collection

Keep the requests aligned with the functional tests and update the expected responses when the
API contract changes. The test IDs and user can be changed in `environments/local.bru` if the
local fixture data changes.
