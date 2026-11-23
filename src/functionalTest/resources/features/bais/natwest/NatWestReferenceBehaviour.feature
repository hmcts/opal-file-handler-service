@Ignore @Opal @NatWest @CARS @NatWestReferenceBehaviour @JIRA-STORY:PO-7230
@JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497
Feature: Observe NatWest account and APR reference behaviour before business-rule confirmation

  @NatWestMissingCheckDigit
  Scenario: 01 - A payment reference without a check digit remains unchanged during preprocessing
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 80             | 3028121            | 80000921A              |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference      |
      | 80             | APR        | 80000921       |
      | 80             | APR        | QA Check Digit |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1100        | 123456     | 99887766      | QA Check Digit | 80000921         | 03/01/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1100          | 80000921              | 80000921                 | 03/01/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1100

  @NatWestMalformedAccountReferences
  Scenario: 02 - Nonstandard account-reference formats remain in the transformed payments
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 80             | 3028122            | 80000922A              |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference    |
      | 80             | APR        | QA Format 22 |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 100         | 123456     | 99887766      | QA Format 22   | 80000922-A       | 04/01/2026       |
      | 99   | 200         | 123456     | 99887766      | QA Format 22   | 80000922/A       | 05/01/2026       |
      | 99   | 300         | 123456     | 99887766      | QA Format 22   | 80000922*A       | 06/01/2026       |
      | 99   | 400         | 123456     | 99887766      | QA Format 22   | 80000922&A       | 07/01/2026       |
      | 99   | 500         | 123456     | 99887766      | QA Format 22   | 80000922!A       | 08/01/2026       |
      | 99   | 600         | 123456     | 99887766      | QA Format 22   | 80000922 A       | 09/01/2026       |
      | 99   | 700         | 123456     | 99887766      | QA Format 22   | 7700092A         | 10/01/2026       |
      | 99   | 800         | 123456     | 99887766      | QA Format 22   | 770009222A       | 11/01/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 100           | 80000922-A            | 80000922-A               | 04/01/2026       | false  | true             |
      | 99              | 200           | 80000922/A            | 80000922/A               | 05/01/2026       | false  | true             |
      | 99              | 300           | 80000922*A            | 80000922*A               | 06/01/2026       | false  | true             |
      | 99              | 400           | 80000922&A            | 80000922&A               | 07/01/2026       | false  | true             |
      | 99              | 500           | 80000922!A            | 80000922!A               | 08/01/2026       | false  | true             |
      | 99              | 600           | 80000922 A            | 80000922 A               | 09/01/2026       | false  | true             |
      | 99              | 700           | 7700092A              | 7700092A                 | 10/01/2026       | false  | true             |
      | 99              | 800           | 770009222A            | 770009222A               | 11/01/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 8 and total amount 3600

  @NatWestUnmatchedAPR
  Scenario: 03 - An APR-like reference with no matching relationship remains unchanged
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference       |
      | 80             | APR        | APR-NO-MATCH-23 |
      | 80             | APR        | QA No APR       |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1200        | 123456     | 99887766      | QA No APR      | APR-NO-MATCH-23  | 12/01/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1200          | APR-NO-MATCH-23       | APR-NO-MATCH-23          | 12/01/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1200

  @NatWestAPRTextFormat
  Scenario: 04 - An APR containing punctuation resolves when an exact relationship exists
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | APR?QA-24    | 80             | 3028124            | 80000924A              |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1300        | 123456     | 99887766      | Ms Lena Scott  | APR?QA-24        | 13/01/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1300          | APR?QA-24             | 3028124                  | 13/01/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1300

  @NatWestStoredCaseReference
  Scenario: 05 - A stored case reference is not translated during Fines preprocessing
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 80             | 3028125            | 80000925A              |
    And the following prosecutor case reference is stored for a defendant account:
      | businessUnitId | defendantAccountId | caseReference |
      | 80             | 3028125            | LIBRA-QA-25   |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference     |
      | 80             | APR        | LIBRA-QA-25   |
      | 80             | APR        | Ms Tara Smith |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1400        | 123456     | 99887766      | Ms Tara Smith  | LIBRA-QA-25      | 14/01/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1400          | LIBRA-QA-25           | LIBRA-QA-25              | 14/01/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1400

  @NatWestCarsEmpty
  Scenario: 06 - A CARS file with no transactions records an empty-file result
    Given a unique NatWest CARS file contains no transactions
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus            | sourceDomain | errorContains | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | SUCCESS_NO_TRANSACTIONS | null         |               | true                | false             | false                  |

  @NatWestUnknownCaseReference
  Scenario: 07 - A case-like reference with no matching account remains unchanged during preprocessing
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference         |
      | 80             | APR        | LIBRA-NO-MATCH-27 |
      | 80             | APR        | Ms Nina Rao       |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference  | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Ms Nina Rao    | LIBRA-NO-MATCH-27 | 15/01/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | LIBRA-NO-MATCH-27     | LIBRA-NO-MATCH-27        | 15/01/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500
