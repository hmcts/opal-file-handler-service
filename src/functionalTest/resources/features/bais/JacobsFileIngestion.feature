@Opal @PO-6437 @BaisFileFixture
Feature: Jacobs file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid Jacobs file is ingested
    Given the configured "Jacobs" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Jacobs" file ingestion job is requested through testing support
    Then the "Jacobs" file is processed successfully
    And the extracted "Jacobs" payments are ready for the external interface
    And Fines preprocessing completes for the stored "Jacobs" file
    And the transformed "Jacobs" file has payment type "CASH" and transaction codes in order
      | 99 |
      | 99 |
    And the transformed "Jacobs" file contains the account references in order
      | 23000106E |
      | 23000113P |
    And the transformed "Jacobs" file contains transaction amounts in pence in order
      | 1000 |
      | 5000 |

  Scenario: A duplicate Jacobs file is detected
    Given the configured "Jacobs" file has already been processed
    When the configured "Jacobs" file is placed on bais
    And the "Jacobs" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "JACOBS" interface file is recorded
    And the duplicate "JACOBS" interface file reuses the original blob
    And the configured "Jacobs" file no longer exists on bais
    And Fines preprocessing reuses the original result for the stored "Jacobs" file

  Scenario: An unsupported Jacobs file is ignored
    Given the configured "Jacobs" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Jacobs" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "JACOBS" interface file is stored for the unsupported file
    And the unsupported "Jacobs" file remains on bais

  Scenario: Invalid Jacobs content is recorded as failed with its source blob
    Given the configured "Jacobs" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Jacobs" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "JACOBS" interface file is recorded with its source blob
    And the invalid "Jacobs" file remains on bais
