@Opal @NatWest @CARS @APR @NatWestCARS @PO-6398 @JIRA-STORY:PO-7230
@JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497
Feature: NatWest CARS ingestion and account reference translation

  @JIRA-STORY:PO-6392 @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 01 - NatWest cash payments with mixed transaction codes resolve known APRs
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 99141114A    | 80             | 3028001            | 80000991A              |
      | 99141115B    | 80             | 3028002            | 80000992A              |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName   | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mrs D Richardson | 99141114A        | 03/01/2026       |
      | 93   | 500         | 654321     | 55443322      | Mr A Waddams     | 99141115B        | 17/04/2026       |
      | 68   | 300         | 123456     | 99887766      | Mrs D Richardson | 99141114A        | 29/02/2024       |
      | 00   | 200         | 654321     | 55443322      | Mr A Waddams     | 99141115B        | 31/12/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | 99141114A             | 3028001                  | 03/01/2026       | false  | true             |
      | 99              | 500           | 99141115B             | 3028002                  | 17/04/2026       | false  | true             |
      | 68              | 300           | 99141114A             | 3028001                  | 29/02/2024       | false  | true             |
      | 00              | 200           | 99141115B             | 3028002                  | 31/12/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 4 and total amount 2500

  @NatWestValidAccounts @JIRA-STORY:PO-6392 @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 02 - NatWest cash payments with mixed transaction codes retain valid account numbers
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 80             | 3028011            | 80000981A              |
      | 80             | 3028012            | 80000982A              |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference        |
      | 80             | APR        | 80000981A        |
      | 80             | APR        | 80000982A        |
      | 80             | APR        | Mrs D Richardson |
      | 80             | APR        | Mr A Waddams     |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName   | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mrs D Richardson | 80000981A        | 14/02/2026       |
      | 93   | 500         | 654321     | 55443322      | Mr A Waddams     | 80000982A        | 30/06/2026       |
      | 68   | 300         | 123456     | 99887766      | Mrs D Richardson | 80000981A        | 01/01/2027       |
      | 00   | 200         | 654321     | 55443322      | Mr A Waddams     | 80000982A        | 28/02/2025       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | 80000981A             | 80000981A                | 14/02/2026       | false  | true             |
      | 99              | 500           | 80000982A             | 80000982A                | 30/06/2026       | false  | true             |
      | 68              | 300           | 80000981A             | 80000981A                | 01/01/2027       | false  | true             |
      | 00              | 200           | 80000982A             | 80000982A                | 28/02/2025       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 4 and total amount 2500

  @NatWestNonexistentAccount @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 03 - NatWest cash payment for a nonexistent account retains its original reference
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType       | reference     |
      | 80             | defendantAccount | 98743162Q     |
      | 80             | APR              | 98743162Q     |
      | 80             | APR              | QA NW Missing |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | QA NW Missing  | 98743162Q        | 09/08/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | 98743162Q             | 98743162Q                | 09/08/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500

  @NatWestValidAPR @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 04 - NatWest cash payment with a valid APR resolves to its linked defendant account
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-APR-QA-03 | 80             | 3028003            | 80000993A              |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mrs S Patel    | NW-APR-QA-03     | 21/09/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | NW-APR-QA-03          | 3028003                  | 21/09/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500

  @NatWestAPRBusinessUnit @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 05 - NatWest cash payment resolves a shared APR using its destination business unit
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName | destinationAccountType |
      | 78             | 0047             | 560078              | 27048578                 | HMCTS Fines     | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-APR-QA-03 | 80             | 3028003            | 80000993A              |
      | NW-APR-QA-03 | 78             | 3027803            | 78000993A              |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mrs S Patel    | NW-APR-QA-03     | 05/11/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | NW-APR-QA-03          | 3027803                  | 05/11/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500

  @NatWestOriginatorNameAPR @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 06 - NatWest cash payment resolves an APR from the originator name when its reference has no match
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType       | reference |
      | 80             | defendantAccount | 98743163R |
      | 80             | APR              | 98743163R |
    And the following APRs are linked to defendant accounts:
      | aprReference    | businessUnitId | defendantAccountId | defendantAccountNumber |
      | Mrs Kavita Shah | 80             | 3028006            | 80000996A              |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName  | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mrs Kavita Shah | 98743163R        | 12/12/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | 98743163R             | 3028006                  | 12/12/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500

  @NatWestConsolidatedAccounts @NatWestConsolidatedApr @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 07 - A former child account number resolves to the consolidated master
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 80             | 3028141            | 80001941A              |
    And the following consolidated payment references map to master accounts through APRs:
      | businessUnitId | paymentReference | masterAccountId | masterAccountNumber |
      | 80             | 80001942A        | 3028141         | 80001941A           |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Ms Aisha Patel | 80001942A        | 16/11/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | 80001942A             | 3028141                  | 16/11/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500

  @NatWestConsolidatedAprAlias @NatWestConsolidatedAccounts @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 07b - An alternate consolidated payment reference resolves to the master
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 80             | 3028141            | 80001941A              |
    And the following consolidated payment references map to master accounts through APRs:
      | businessUnitId | paymentReference | masterAccountId | masterAccountNumber |
      | 80             | NW-CHILD-41      | 3028141         | 80001941A           |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Ms Aisha Patel | NW-CHILD-41      | 17/11/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | NW-CHILD-41           | 3028141                  | 17/11/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500

  @NatWestZeroPayment @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 08 - NatWest zero-value payment is retained and its APR resolves during preprocessing
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference  | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-ZERO-QA-08 | 80             | 3028008            | 80000998A              |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName  | accountReference | dateEntryApplied |
      | 99   | 0           | 123456     | 99887766      | Mr James Wilson | NW-ZERO-QA-08    | 22/03/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 0             | NW-ZERO-QA-08         | 3028008                  | 22/03/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 0

  @NatWestLargePayment @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario Outline: 09 - NatWest maximum-width payment for an account with a <balanceSize> balance is preserved during preprocessing
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference   | businessUnitId | defendantAccountId   | defendantAccountNumber |
      | <aprReference> | 80             | <defendantAccountId> | <accountNumber>        |
    And the following defendant accounts have these outstanding balances:
      | businessUnitId | defendantAccountId   | outstandingBalancePence   |
      | 80             | <defendantAccountId> | <outstandingBalancePence> |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName  | accountReference | dateEntryApplied |
      | 99   | 99999999999 | 123456     | 99887766      | Mr David Morgan | <aprReference>   | <paymentDate>    |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 99999999999   | <aprReference>        | <defendantAccountId>     | <paymentDate>    | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 99999999999

    Examples:
      | balanceSize | outstandingBalancePence | aprReference | defendantAccountId | accountNumber | paymentDate |
      | large       | 100000000000            | NW-LARGE-09A | 3028031            | 80000961A     | 14/05/2026  |
      | small       | 10000                   | NW-LARGE-09B | 3028032            | 80000962A     | 23/10/2026  |

  @NatWestReferenceFormatting @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 10 - NatWest extracts valid account numbers from prefixes and suffixes and trims surrounding spaces
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 80             | 3028041            | 80000951A              |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference       |
      | 80             | APR        | ABC80000951A    |
      | 80             | APR        | 80000951AABC    |
      | 80             | APR        | ABC80000951AABC |
      | 80             | APR        | 80000951A       |
      | 80             | APR        | Mr Henry Evans  |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | referenceLeadingSpaces | referenceTrailingSpaces | dateEntryApplied |
      | 99   | 100         | 123456     | 99887766      | Mr Henry Evans | ABC80000951A     | 0                      | 0                       | 01/03/2026       |
      | 99   | 200         | 123456     | 99887766      | Mr Henry Evans | 80000951AABC     | 0                      | 0                       | 08/07/2026       |
      | 99   | 300         | 123456     | 99887766      | Mr Henry Evans | ABC80000951AABC  | 0                      | 0                       | 30/11/2026       |
      | 99   | 400         | 123456     | 99887766      | Mr Henry Evans | 80000951A        | 2                      | 2                       | 29/02/2024       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 100           | ABC80000951A          | 80000951A                | 01/03/2026       | false  | true             |
      | 99              | 200           | 80000951AABC          | 80000951A                | 08/07/2026       | false  | true             |
      | 99              | 300           | ABC80000951AABC       | 80000951A                | 30/11/2026       | false  | true             |
      | 99              | 400           | 80000951A             | 80000951A                | 29/02/2024       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 4 and total amount 1000

  @NatWestChequeOnly @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 11 - NatWest cheque-only payments retain code 11 and bypass APR translation
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-CHEQUE-11 | 80             | 3028051            | 80000941A              |
    And the following cheque payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName  | accountReference | dateEntryApplied |
      | 11   | 1500        | 123456     | 99887766      | Mrs Linda Brown | NW-CHEQUE-11     | 05/02/2026       |
      | 11   | 500         | 654321     | 55443322      | Mr Peter Brown  | NW-CHEQUE-11     | 17/09/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 11              | 1500          | NW-CHEQUE-11          | NW-CHEQUE-11             | 05/02/2026       | true   | false            |
      | 11              | 500           | NW-CHEQUE-11          | NW-CHEQUE-11             | 17/09/2026       | true   | false            |
    And the complete transformed NatWest JSON has payment type "CHEQUE", record count 2 and total amount 2000

  @NatWestMixedCheque @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 12 - NatWest file beginning with code 11 remains CHEQUE while its code 99 payment resolves an APR
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-MIXED-12  | 80             | 3028052            | 80000942A              |
    And the following cheque payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName  | accountReference | dateEntryApplied |
      | 11   | 1500        | 123456     | 99887766      | Mrs Linda Brown | NW-MIXED-12      | 11/04/2026       |
      | 99   | 500         | 654321     | 55443322      | Mr Peter Brown  | NW-MIXED-12      | 19/10/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 11              | 1500          | NW-MIXED-12           | NW-MIXED-12              | 11/04/2026       | true   | false            |
      | 99              | 500           | NW-MIXED-12           | 3028052                  | 19/10/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CHEQUE", record count 2 and total amount 2000

  @NatWestChequeTotals @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 13 - NatWest cheque extraction excludes code 44 totals rows from JSON and payment totals
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cheque payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName  | accountReference | dateEntryApplied | expectedInSourceJson |
      | 11   | 1500        | 123456     | 99887766      | Mr Arthur Hayes | CHQ-13-REF       | 18/08/2026       | true                 |
      | 44   | 1500        | 123456     | 99887766      | Bank Total      | TOTAL            | 19/08/2026       | false                |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 11              | 1500          | CHQ-13-REF            | CHQ-13-REF               | 18/08/2026       | true   | false            |
    And the complete transformed NatWest JSON has payment type "CHEQUE", record count 1 and total amount 1500

  @NatWestInvalidCode @JIRA-STORY:PO-7244
  Scenario: 14 - A NatWest CARS file with a nonnumeric transaction code is rejected
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | AB   | 1500        | 123456     | 99887766      | Mr David Price | BAD-CODE-14      | 25/09/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | sourceDomain | errorContains                                                       | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | MAINTENANCE  | BACS Standard 18 transaction code at index 15 to 17 must be numeric | true                | false             | true                   |

  @NatWestMissingReference @JIRA-STORY:PO-7244
  Scenario: 15 - A NatWest CARS file with no originator account reference is rejected
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Ms Sarah Khan  |                  | 14/10/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | sourceDomain | errorContains                                                               | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | MAINTENANCE  | BACS Standard 18 originator account reference is required at index 64 to 82 | true                | false             | true                   |

  @NatWestUnmappedBank @JIRA-STORY:PO-7244
  Scenario: 16 - A NatWest CARS file with an unmapped destination bank account is rejected
    Given the following destination bank account is unmapped:
      | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 111111              | 22222222                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mr David Price | 80000991A        | 05/11/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | sourceDomain | errorContains                                                                                         | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | MAINTENANCE  | Business unit bank account with sort code '111111' and account number '22222222' could not be located | true                | false             | true                   |

  @NatWestDuplicatePreprocessing @JIRA-STORY:PO-7244
  Scenario: 17 - Duplicate NatWest ingestion reuses the original Fines preprocessing result
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName  | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mrs Aisha Patel | 80000991A        | 21/11/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    When the same NatWest CARS file is placed on bais again
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the duplicate NatWest ingestion result matches:
      | sourceStatus | sourceBlobReused | sourceJsonCount | finesPreprocessingCount | inputFileRemainsOnBais |
      | DUPLICATE    | true             | 1               | 1                       | false                  |

  @NatWestUnsupportedName @JIRA-STORY:PO-7244
  Scenario: 18 - NatWest ignores a CARS file with an unsupported filename
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mr Adam Clarke | 80000991A        | 02/12/2026       |
    And the generated NatWest CARS filename has the unsupported suffix ".txt"
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the ignored NatWest file result matches:
      | interfaceFileCount | inputFileRemainsOnBais |
      | 0                  | true                   |

  @NatWestMalformedContent @JIRA-STORY:PO-7244
  Scenario: 19 - A NatWest CARS file missing a required BACS header is rejected
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Ms Helen Brown | 80000991A        | 17/12/2026       |
    And the following NatWest CARS header is replaced:
      | originalPrefix | replacementPrefix |
      | VOL1           | BAD1              |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | sourceDomain | errorContains                                              | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | MAINTENANCE  | BACS Standard 18 file did not contain the expected headers | true                | false             | true                   |

  @NatWestMixedValidInvalidCodes @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 20 - A NatWest CARS file with eight valid and two unsupported numeric codes retains eight payments
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName   | destinationAccountType |
      | 80             | 0049             | 560080              | 27048580                 | HMCTS Westminster | 0                      |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference      |
      | 80             | APR        | NW-CODE-MIX-20 |
      | 80             | APR        | Ms Jane Mercer |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied | expectedInTransformedJson |
      | 99   | 100         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 06/01/2026       | true                      |
      | 93   | 200         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 17/02/2026       | true                      |
      | 68   | 300         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 28/03/2026       | true                      |
      | 00   | 400         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 09/04/2026       | true                      |
      | 15   | 500         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 20/05/2026       | true                      |
      | 99   | 600         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 01/06/2026       | true                      |
      | 00   | 700         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 12/07/2026       | true                      |
      | 68   | 800         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 23/08/2026       | true                      |
      | 33   | 900         | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 04/09/2026       | false                     |
      | 55   | 1000        | 123456     | 99887766      | Ms Jane Mercer | NW-CODE-MIX-20   | 15/10/2026       | false                     |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 100           | NW-CODE-MIX-20        | NW-CODE-MIX-20           | 06/01/2026       | false  | true             |
      | 99              | 200           | NW-CODE-MIX-20        | NW-CODE-MIX-20           | 17/02/2026       | false  | true             |
      | 68              | 300           | NW-CODE-MIX-20        | NW-CODE-MIX-20           | 28/03/2026       | false  | true             |
      | 00              | 400           | NW-CODE-MIX-20        | NW-CODE-MIX-20           | 09/04/2026       | false  | true             |
      | 99              | 500           | NW-CODE-MIX-20        | NW-CODE-MIX-20           | 20/05/2026       | false  | true             |
      | 99              | 600           | NW-CODE-MIX-20        | NW-CODE-MIX-20           | 01/06/2026       | false  | true             |
      | 00              | 700           | NW-CODE-MIX-20        | NW-CODE-MIX-20           | 12/07/2026       | false  | true             |
      | 68              | 800           | NW-CODE-MIX-20        | NW-CODE-MIX-20           | 23/08/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 8 and total amount 3600
