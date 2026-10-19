@Opal @PO-6438 @BaisReportFixture
Feature: CDER file ingestion

  Scenario: A valid CDER file is ingested
    Given the configured "Cder" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Cder" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "CDER" interface file is stored
    And the stored "Cder" file can be retrieved by interface file ID
    And the stored "Cder" file content matches the bais "file"
    And the configured "Cder" file no longer exists on bais

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
