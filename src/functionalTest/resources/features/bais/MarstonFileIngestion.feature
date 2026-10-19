@Opal @PO-6439 @BaisReportFixture
Feature: Marston file ingestion

  Scenario: A valid Marston file is ingested
    Given the configured "Marston" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "MARSTON" interface file is stored
    And the stored "Marston" file can be retrieved by interface file ID
    And the stored "Marston" file content matches the bais "file"
    And the configured "Marston" file no longer exists on bais

  Scenario: A duplicate Marston file is detected
    Given the configured "Marston" file has already been processed
    When the configured "Marston" file is placed on bais
    And the "Marston" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "MARSTON" interface file is recorded
    And the duplicate "MARSTON" interface file reuses the original blob
    And the configured "Marston" file no longer exists on bais

  Scenario: An unsupported Marston file is ignored
    Given the configured "Marston" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "MARSTON" interface file is stored for the unsupported file
    And the unsupported "Marston" file remains on bais

  Scenario: Invalid Marston content is recorded as failed with its source blob
    Given the configured "Marston" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "MARSTON" interface file is recorded with its source blob
    And the invalid "Marston" file remains on bais
