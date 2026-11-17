# DWP and Bailiff processing fixtures

These persistent PACS XML inputs are consumed by `PreparedBankingPackIntegrationTest` in the existing Gradle `integration` suite (also included by `check`). No external file path, manual upload or additional runner is required.

Each interface has six cases: valid payment, exact duplicate replay, absent court mapping, malformed XML, and two APR-reference extraction examples. Each well-formed file carries one type 02 payment and one positive type 03 adjustment, each for one penny. The summary total is two pence.

The duplicate fixture deliberately has identical bytes and filename to the valid fixture. Its test processes an initial upload and then re-uploads it, independently of the other cases. Fixed names and dates are intentional: each case removes its owned database records, blobs and SFTP input afterwards, allowing repeat runs without renaming.

The test seeds isolated local bank/court mappings for business-unit IDs 7240–7243 and restores the temporary SFTP configuration. Existing container infrastructure and queue publisher mocks are reused. Only test-owned data is cleaned up.

APR-labelled cases assert that reference text is preserved during extraction. Neither APR relationships nor debtor accounts are created here. Real APR resolution, queue consumption and final payment allocation require separate business E2E coverage. DWP uses the existing Maintenance domain scope; the three Bailiff suppliers use Fines.

Traceability: PO-7244 / PO-3497. Inputs were derived from the existing DWP, Jacobs, CDER and Marston functional fixtures, with synthetic routing data and consistent filenames/headers.
