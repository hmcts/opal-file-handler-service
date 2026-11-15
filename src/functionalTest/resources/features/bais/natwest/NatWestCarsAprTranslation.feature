@Opal @NatWest @CARS @APR @NatWestCARS @PO-6398 @JIRA-STORY:PO-7230
@JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497
Feature: NatWest CARS ingestion and account reference translation

  @JIRA-STORY:PO-6392 @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 01 - NatWest cash payments with mixed transaction codes resolve known APRs
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 99141114A    | 77             | 3027001            | 77000991A              |
      | 99141115B    | 77             | 3027002            | 77000992A              |
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
      | 99              | 1500          | 99141114A             | 3027001                  | 03/01/2026       | false  | true             |
      | 99              | 500           | 99141115B             | 3027002                  | 17/04/2026       | false  | true             |
      | 68              | 300           | 99141114A             | 3027001                  | 29/02/2024       | false  | true             |
      | 00              | 200           | 99141115B             | 3027002                  | 31/12/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 4 and total amount 2500

  @NatWestValidAccounts @JIRA-STORY:PO-6392 @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 02 - NatWest cash payments with mixed transaction codes retain valid account numbers
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 77             | 3027011            | 77000981A              |
      | 77             | 3027012            | 77000982A              |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference        |
      | 77             | APR        | 77000981A        |
      | 77             | APR        | 77000982A        |
      | 77             | APR        | Mrs D Richardson |
      | 77             | APR        | Mr A Waddams     |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName   | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mrs D Richardson | 77000981A        | 14/02/2026       |
      | 93   | 500         | 654321     | 55443322      | Mr A Waddams     | 77000982A        | 30/06/2026       |
      | 68   | 300         | 123456     | 99887766      | Mrs D Richardson | 77000981A        | 01/01/2027       |
      | 00   | 200         | 654321     | 55443322      | Mr A Waddams     | 77000982A        | 28/02/2025       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | 77000981A             | 77000981A                | 14/02/2026       | false  | true             |
      | 99              | 500           | 77000982A             | 77000982A                | 30/06/2026       | false  | true             |
      | 68              | 300           | 77000981A             | 77000981A                | 01/01/2027       | false  | true             |
      | 00              | 200           | 77000982A             | 77000982A                | 28/02/2025       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 4 and total amount 2500

  @NatWestNonexistentAccount @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 03 - NatWest cash payment for a nonexistent account retains its original reference
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType       | reference     |
      | 77             | defendantAccount | 98743162Q     |
      | 77             | APR              | 98743162Q     |
      | 77             | APR              | QA NW Missing |
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
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-APR-QA-03 | 77             | 3027003            | 77000993A              |
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
      | 99              | 1500          | NW-APR-QA-03          | 3027003                  | 21/09/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500

  @NatWestAPRBusinessUnit @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 05 - NatWest cash payment resolves a shared APR using its destination business unit
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName | destinationAccountType |
      | 78             | 0047             | 560078              | 27048578                 | HMCTS Fines     | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-APR-QA-03 | 77             | 3027003            | 77000993A              |
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
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType       | reference |
      | 77             | defendantAccount | 98743163R |
      | 77             | APR              | 98743163R |
    And the following APRs are linked to defendant accounts:
      | aprReference    | businessUnitId | defendantAccountId | defendantAccountNumber |
      | Mrs Kavita Shah | 77             | 3027006            | 77000996A              |
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
      | 99              | 1500          | 98743163R             | 3027006                  | 12/12/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 1500

  @NatWestConsolidatedAccounts @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 07 - NatWest payments to consolidated master and child accounts retain their references during preprocessing
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 77             | 3027021            | 77000971A              |
      | 77             | 3027022            | 77000972A              |
    And the following defendant account consolidations exist:
      | businessUnitId | masterAccountId | childAccountId |
      | 77             | 3027021         | 3027022        |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference      |
      | 77             | APR        | 77000971A      |
      | 77             | APR        | 77000972A      |
      | 77             | APR        | Mr Alan Clarke |
      | 77             | APR        | Ms Emma Clarke |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mr Alan Clarke | 77000971A        | 29/02/2024       |
      | 99   | 500         | 654321     | 55443322      | Ms Emma Clarke | 77000972A        | 31/12/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 1500          | 77000971A             | 77000971A                | 29/02/2024       | false  | true             |
      | 99              | 500           | 77000972A             | 77000972A                | 31/12/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 2 and total amount 2000

  @NatWestZeroPayment @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 08 - NatWest zero-value payment is retained and its APR resolves during preprocessing
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference  | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-ZERO-QA-08 | 77             | 3027008            | 77000998A              |
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
      | 99              | 0             | NW-ZERO-QA-08         | 3027008                  | 22/03/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 1 and total amount 0

  @NatWestLargePayment @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario Outline: 09 - NatWest maximum-width payment for an account with a <balanceSize> balance is preserved during preprocessing
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference   | businessUnitId | defendantAccountId   | defendantAccountNumber |
      | <aprReference> | 77             | <defendantAccountId> | <accountNumber>        |
    And the following defendant accounts have these outstanding balances:
      | businessUnitId | defendantAccountId   | outstandingBalancePence   |
      | 77             | <defendantAccountId> | <outstandingBalancePence> |
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
      | large       | 100000000000            | NW-LARGE-09A | 3027031            | 77000961A     | 14/05/2026  |
      | small       | 10000                   | NW-LARGE-09B | 3027032            | 77000962A     | 23/10/2026  |

  @NatWestReferenceFormatting @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 10 - NatWest extracts valid account numbers from prefixes and suffixes and trims surrounding spaces
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following defendant accounts exist:
      | businessUnitId | defendantAccountId | defendantAccountNumber |
      | 77             | 3027041            | 77000951A              |
    And the following Fines records do not exist, removing any existing matches:
      | businessUnitId | recordType | reference       |
      | 77             | APR        | ABC77000951A    |
      | 77             | APR        | 77000951AABC    |
      | 77             | APR        | ABC77000951AABC |
      | 77             | APR        | 77000951A       |
      | 77             | APR        | Mr Henry Evans  |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | referenceLeadingSpaces | referenceTrailingSpaces | dateEntryApplied |
      | 99   | 100         | 123456     | 99887766      | Mr Henry Evans | ABC77000951A     | 0                      | 0                       | 01/03/2026       |
      | 99   | 200         | 123456     | 99887766      | Mr Henry Evans | 77000951AABC     | 0                      | 0                       | 08/07/2026       |
      | 99   | 300         | 123456     | 99887766      | Mr Henry Evans | ABC77000951AABC  | 0                      | 0                       | 30/11/2026       |
      | 99   | 400         | 123456     | 99887766      | Mr Henry Evans | 77000951A        | 2                      | 2                       | 29/02/2024       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the stored NatWest SOURCE and SOURCE_JSON match the payments above
    And Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON
    And the expected transformed NatWest payments are:
      | transactionCode | amountInPence | inputAccountReference | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 100           | ABC77000951A          | 77000951A                | 01/03/2026       | false  | true             |
      | 99              | 200           | 77000951AABC          | 77000951A                | 08/07/2026       | false  | true             |
      | 99              | 300           | ABC77000951AABC       | 77000951A                | 30/11/2026       | false  | true             |
      | 99              | 400           | 77000951A             | 77000951A                | 29/02/2024       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CASH", record count 4 and total amount 1000

  @NatWestChequeOnly @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 11 - NatWest cheque-only payments retain code 11 and bypass APR translation
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-CHEQUE-11 | 77             | 3027051            | 77000941A              |
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
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following APRs are linked to defendant accounts:
      | aprReference | businessUnitId | defendantAccountId | defendantAccountNumber |
      | NW-MIXED-12  | 77             | 3027052            | 77000942A              |
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
      | 99              | 500           | NW-MIXED-12           | 3027052                  | 19/10/2026       | false  | true             |
    And the complete transformed NatWest JSON has payment type "CHEQUE", record count 2 and total amount 2000

  @NatWestChequeTotals @JIRA-STORY:PO-6460 @JIRA-STORY:PO-6461
  Scenario: 13 - NatWest cheque extraction excludes code 44 totals rows from JSON and payment totals
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
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
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | AB   | 1500        | 123456     | 99887766      | Mr David Price | BAD-CODE-14      | 25/09/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | errorContains                                                       | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | BACS Standard 18 transaction code at index 15 to 17 must be numeric | true                | false             | true                   |

  @NatWestMissingReference @JIRA-STORY:PO-7244
  Scenario: 15 - A NatWest CARS file with no originator account reference is rejected
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Ms Sarah Khan  |                  | 14/10/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | errorContains                                                               | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | BACS Standard 18 originator account reference is required at index 64 to 82 | true                | false             | true                   |

  @NatWestUnmappedBank @JIRA-STORY:PO-7244
  Scenario: 16 - A NatWest CARS file with an unmapped destination bank account is rejected
    Given the following destination bank account is unmapped:
      | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 111111              | 22222222                 | HMCTS Camberwell | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mr David Price | 77000991A        | 05/11/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | errorContains                                                                                         | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | Business unit bank account with sort code '111111' and account number '22222222' could not be located | true                | false             | true                   |

  @NatWestDuplicatePreprocessing @JIRA-STORY:PO-7244
  Scenario: 17 - Duplicate NatWest ingestion reuses the original Fines preprocessing result
    Given the following destination bank account is configured:
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName  | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mrs Aisha Patel | 77000991A        | 21/11/2026       |
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
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Mr Adam Clarke | 77000991A        | 02/12/2026       |
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
      | businessUnitId | businessUnitCode | destinationSortCode | destinationAccountNumber | destinationName  | destinationAccountType |
      | 77             | 0046             | 560077              | 27048577                 | HMCTS Camberwell | 0                      |
    And the following cash payments are written to a unique NatWest CARS file:
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 1500        | 123456     | 99887766      | Ms Helen Brown | 77000991A        | 17/12/2026       |
    And the following NatWest CARS header is replaced:
      | originalPrefix | replacementPrefix |
      | VOL1           | BAD1              |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And the NatWest ingestion result matches:
      | sourceStatus | errorContains                                              | sourceBlobPreserved | sourceJsonCreated | inputFileRemainsOnBais |
      | FAILED       | BACS Standard 18 file did not contain the expected headers | true                | false             | true                   |
