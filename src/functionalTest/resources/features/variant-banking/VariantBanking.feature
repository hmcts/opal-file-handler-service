@Opal @VariantBanking @PO-8744 @JIRA-STORY:PO-8744 @JIRA-EPIC:PO-3952 @JIRA-LABEL:file-handler-service
Feature: Variant Banking file upload, extraction and Fines preprocessing

  @VariantBankingMixed
  Scenario: Mixed cash payments for business unit 065 retain their order and translate code 93
    Given the following cash payments are written to a unique Variant Banking file for business unit "065":
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 00   | 1200        | 123456     | 99887766      | Ms Priya Shah  | VB-MIXED-01      | 09/01/2026       |
      | 93   | 500         | 654321     | 55443322      | Mr Noah Evans  | VB-MIXED-02      | 17/06/2026       |
      | 99   | 300         | 123456     | 99887766      | Ms Priya Shah  | VB-MIXED-03      | 28/11/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the Variant Banking file is uploaded through the Add Interface Files API
    Then the stored Variant Banking SOURCE and SOURCE_JSON match the payments and filename business unit
    And Fines preprocessing creates a TRANSFORMED_JSON for the Variant Banking file
    And the expected transformed Variant Banking payments are:
      | transactionCode | amountInPence | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 00              | 1200          | VB-MIXED-01              | 09/01/2026       | false  | true             |
      | 99              | 500           | VB-MIXED-02              | 17/06/2026       | false  | true             |
      | 99              | 300           | VB-MIXED-03              | 28/11/2026       | false  | true             |
    And the complete transformed Variant Banking JSON has payment type "CASH", record count 3 and total amount 2000

  Scenario: The filename selects London business unit 065 with the configured Fines bank mapping
    Given the following cash payments are written to a unique Variant Banking file for business unit "065":
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 750         | 123456     | 99887766      | Mrs Leah Mills | VB-LONDON-01     | 04/03/2026       |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the Variant Banking file is uploaded through the Add Interface Files API
    Then the stored Variant Banking SOURCE and SOURCE_JSON match the payments and filename business unit
    And Fines preprocessing creates a TRANSFORMED_JSON for the Variant Banking file
    And the expected transformed Variant Banking payments are:
      | transactionCode | amountInPence | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 99              | 750           | VB-LONDON-01             | 04/03/2026       | false  | true             |
    And the complete transformed Variant Banking JSON has payment type "CASH", record count 1 and total amount 750

  Scenario: An unsupported numeric transaction code remains extracted but is excluded from Fines payments
    Given the following cash payments are written to a unique Variant Banking file for business unit "065":
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied | expectedInTransformedJson |
      | 00   | 1200        | 123456     | 99887766      | Ms Priya Shah  | VB-CODE-01       | 06/02/2026       | true                      |
      | 33   | 800         | 654321     | 55443322      | Mr Noah Evans  | VB-CODE-02       | 15/07/2026       | false                     |
      | 99   | 300         | 123456     | 99887766      | Ms Priya Shah  | VB-CODE-03       | 11/12/2026       | true                      |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the Variant Banking file is uploaded through the Add Interface Files API
    Then the stored Variant Banking SOURCE and SOURCE_JSON match the payments and filename business unit
    And Fines preprocessing creates a TRANSFORMED_JSON for the Variant Banking file
    And the expected transformed Variant Banking payments are:
      | transactionCode | amountInPence | expectedAccountReference | dateEntryApplied | cheque | validTransaction |
      | 00              | 1200          | VB-CODE-01               | 06/02/2026       | false  | true             |
      | 99              | 300           | VB-CODE-03               | 11/12/2026       | false  | true             |
    And the complete transformed Variant Banking JSON has payment type "CASH", record count 2 and total amount 1500

  @VariantBankingRejection
  Scenario Outline: Preprocessing rejects <condition> during upload and rolls back the file records
    Given the following cash payments are written to a unique Variant Banking file for business unit "<businessUnitId>":
      | code | amountPence | originSort | originAccount | originatorName | accountReference | dateEntryApplied |
      | 99   | 900         | 123456     | 99887766      | Mr Isaac Jones | VB-REJECT-01     | 15/08/2026       |
    And the Variant Banking input has "<condition>"
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the Variant Banking file is uploaded through the Add Interface Files API
    Then the Variant Banking upload is rejected without persisted interface files

    Examples:
      | businessUnitId | condition                 |
      | 065            | an invalid filename       |
      | 999            | an unmapped business unit |
      | 065            | missing headers           |
      | 065            | a blank payment reference |
