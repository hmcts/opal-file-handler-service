@Opal @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-6436 @BaisFileFixture
Feature: DWP file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid DWP file with unmatched account references is ingested
    Given the configured "DWP" file "0000015232_dat_0000000612_08011008_111355.txt" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "DWP" file ingestion job is requested through testing support
    Then the "DWP" file is processed successfully
    And the extracted "DWP" payments are ready for the external interface
    And Fines preprocessing completes for the stored "DWP" file
    And the transformed "DWP" file has payment type "CASH" and transaction codes in order
      | 99 |
      | 99 |
      | 99 |
      | 99 |
      | 99 |
    And the transformed "DWP" file contains transaction amounts in pence in order
      | 2125 |
      | 2125 |
      | 2125 |
      | 2125 |
      | 2001 |

  Scenario: A DWP payment with a valid APR resolves to its Fines account
    Given the configured "DWP" file "0000015232_dat_0000000612_08011008_111356.txt" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "DWP" file ingestion job is requested through testing support
    Then the "DWP" file is processed successfully
    And the stored "DWP" JSON file contains the account references in order
      | DWP-APR-01 |
    And Fines preprocessing completes for the stored "DWP" file
    And the transformed "DWP" file has payment type "CASH" and transaction codes in order
      | 99 |
    And the transformed "DWP" file contains the account references in order
      | 88008040000001 |
    And the transformed "DWP" file contains transaction amounts in pence in order
      | 2125 |

  Scenario: A duplicate DWP file is detected
    Given the configured "DWP" file has already been processed
    When the configured "DWP" file is placed on bais
    And the "DWP" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "DWP" interface file is recorded
    And the duplicate "DWP" interface file reuses the original blob
    And the configured "DWP" file no longer exists on bais
    And Fines preprocessing reuses the original result for the stored "DWP" file

  Scenario: An unsupported DWP file is ignored
    Given the configured "DWP" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "DWP" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "DWP" interface file is stored for the unsupported file
    And the unsupported "DWP" file remains on bais

  Scenario: Invalid DWP content is recorded as failed with its source blob
    Given the configured "DWP" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "DWP" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "DWP" interface file is recorded with its source blob
    And the invalid "DWP" file remains on bais
