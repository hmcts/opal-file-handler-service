@Ignore @Opal @NatWest @FPCAS @PO-6398 @JIRA-STORY:PO-7230
@JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497
Feature: NatWest FPCAS ingestion and Fines preprocessing

  @NatWestFpcasMixedCodes
  Scenario: 01 - An FPCAS file extracts cash payments and translates code 93
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest FPCAS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 00   | 1200        | 123456     | 99887766      | Ms Priya Shah  | FP-MIXED-01      | 09/01/2026       |
      | 93   | 500         | 654321     | 55443322      | Mr Noah Evans  | FP-MIXED-02      | 17/06/2026       |
      | 99   | 300         | 123456     | 99887766      | Ms Priya Shah  | FP-MIXED-03      | 28/11/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 00              | 1200          | FP-MIXED-01           | FP-MIXED-01              | 09/01/2026       | false  | true             |
      | 99              | 500           | FP-MIXED-02           | FP-MIXED-02              | 17/06/2026       | false  | true             |
      | 99              | 300           | FP-MIXED-03           | FP-MIXED-03              | 28/11/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 3 and total amount 2000

  @NatWestFpcasApr
  Scenario: 02 - An FPCAS payment resolves an APR in its Fines business unit
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | FP-APR-02    | 80             | 3028061            | 80000931A              |
    And the following cash payments are written to a unique NatWest FPCAS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 750         | 123456     | 99887766      | Mrs Leah Mills | FP-APR-02        | 04/03/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 750           | FP-APR-02             | 3028061                  | 04/03/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 750

  @NatWestFpcasEmpty
  Scenario: 03 - An FPCAS file with no transactions records an empty-file result
    Given a unique NatWest FPCAS file contains no transactions
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus            | sourceDomain | errorContains | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | SUCCESS_NO_TRANSACTIONS | null         |               | true                | false             | false                  |

  @NatWestFpcasUnmappedBank
  Scenario: 04 - An FPCAS file with an unmapped destination bank account is rejected
    Given the following destination bank account is unmapped:
      | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 111112              | 22222223                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest FPCAS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 900         | 123456     | 99887766      | Mr Isaac Jones | FP-BANK-04       | 15/08/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | sourceDomain | errorContains                                                                                         | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | null         | Business unit bank account with sort code '111112' and account number '22222223' could not be located | true                | false             | true                   |

  @NatWestFpcasDuplicate
  Scenario: 05 - Duplicate FPCAS ingestion reuses the original Fines preprocessing result
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest FPCAS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 00   | 600         | 123456     | 99887766      | Ms Hana Cole   | FP-DUP-05        | 12/09/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    When the same NatWest FPCAS file is placed on bais again
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the duplicate NatWest ingestion result matches:
      | sourceStatus | sourceBlobReused | sourceJsonCount | finesPreprocessingCount | inputFileRemainsOnBais |
      | DUPLICATE    | true             | 1               | 1                       | false                  |

  @NatWestFpcasUnsupportedName
  Scenario: 06 - NatWest ignores an FPCAS file with an unsupported filename
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest FPCAS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 450         | 123456     | 99887766      | Mr Oliver Reed | FP-NAME-06       | 05/10/2026       |
    And the generated NatWest FPCAS filename has the unsupported suffix ".txt"
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the ignored NatWest file result matches:
      | interfaceFileCount | inputFileRemainsOnBais |
      | 0                  | true                   |

  @NatWestFpcasInvalidNumericCode
  Scenario: 07 - FPCAS keeps code 33 in SOURCE_JSON but excludes it from Fines payments
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest FPCAS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied | expectedInTransformedJson |
      | 00   | 1200        | 123456     | 99887766      | Ms Priya Shah  | FP-CODE-07A      | 06/02/2026       | true                      |
      | 33   | 800         | 654321     | 55443322      | Mr Noah Evans  | FP-CODE-07B      | 15/07/2026       | false                     |
      | 99   | 300         | 123456     | 99887766      | Ms Priya Shah  | FP-CODE-07C      | 11/12/2026       | true                      |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 00              | 1200          | FP-CODE-07A           | FP-CODE-07A              | 06/02/2026       | false  | true             |
      | 99              | 300           | FP-CODE-07C           | FP-CODE-07C              | 11/12/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 2 and total amount 1500
