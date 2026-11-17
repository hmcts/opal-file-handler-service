@Opal @PO-6438 @BaisFileFixture
Feature: CDER file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid CDER file is ingested
    Given the configured "Cder" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Cder" file ingestion job is requested through testing support
    Then the "Cder" file is processed successfully
    And the extracted "Cder" payments are ready for the external interface

  Scenario: A duplicate CDER file is detected
    Given the configured "Cder" file has already been processed
    When the configured "Cder" file is placed on bais
    And the "Cder" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "CDER" interface file is recorded
    And the duplicate "CDER" interface file reuses the original blob
    And the configured "Cder" file no longer exists on bais

  Scenario: An unsupported CDER file is ignored
    Given the configured "Cder" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Cder" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "CDER" interface file is stored for the unsupported file
    And the unsupported "Cder" file remains on bais

  Scenario: Invalid CDER content is recorded as failed with its source blob
    Given the configured "Cder" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Cder" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "CDER" interface file is recorded with its source blob
    And the invalid "Cder" file remains on bais
