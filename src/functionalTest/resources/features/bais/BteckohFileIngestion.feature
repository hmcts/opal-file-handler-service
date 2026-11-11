@Opal @PO-6428 @BaisFileFixture
Feature: BTEckoh file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid BTEckoh transfer file is ingested
    Given the configured "BTEckoh transfer" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "BTEckoh transfer" file ingestion job is requested through testing support
    Then the "BTEckoh transfer" file is processed successfully
    And the extracted "BTEckoh transfer" payments are ready for the external interface

  Scenario: A duplicate BTEckoh transfer file is detected
    Given the configured "BTEckoh transfer" file has already been processed
    When the configured "BTEckoh transfer" file is placed on bais
    And the "BTEckoh transfer" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "BTECKOH" interface file is recorded
    And the duplicate "BTECKOH" interface file reuses the original blob
    And the configured "BTEckoh transfer" file no longer exists on bais

  Scenario: An unsupported BTEckoh transfer file is ignored
    Given the configured "BTEckoh transfer" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "BTEckoh transfer" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "BTECKOH" interface file is stored for the unsupported file
    And the unsupported "BTEckoh transfer" file remains on bais

  Scenario: Invalid BTEckoh transfer content is recorded as failed with its source blob
    Given the configured "BTEckoh transfer" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "BTEckoh transfer" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "BTECKOH" interface file is recorded with its source blob
    And the invalid "BTEckoh transfer" file remains on bais
