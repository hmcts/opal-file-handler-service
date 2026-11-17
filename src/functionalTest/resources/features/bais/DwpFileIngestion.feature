@Opal @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-6436 @BaisFileFixture
Feature: DWP file ingestion

  # The fixture maps DWP1234567 to business unit DW01 and its MAINTENANCE bank account.
  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid DWP file is ingested
    Given the configured "DWP" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "DWP" file ingestion job is requested through testing support
    Then the "DWP" file is processed successfully
    And the extracted "DWP" payments are ready for the external interface

  Scenario: A duplicate DWP file is detected
    Given the configured "DWP" file has already been processed
    When the configured "DWP" file is placed on bais
    And the "DWP" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "DWP" interface file is recorded
    And the duplicate "DWP" interface file reuses the original blob
    And the configured "DWP" file no longer exists on bais

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
