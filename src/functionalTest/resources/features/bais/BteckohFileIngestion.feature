@Opal @PO-6428 @BaisReportFixture
Feature: BTEckoh file ingestion

  Scenario: A valid BTEckoh transfer file is ingested
    Given the configured "BTEckoh transfer" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "BTEckoh transfer" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "BTECKOH" interface file is stored
    And the stored "BTEckoh transfer" file can be retrieved by interface file ID
    And the stored "BTEckoh transfer" file content matches the bais "file"
    And the configured "BTEckoh transfer" file no longer exists on bais

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
