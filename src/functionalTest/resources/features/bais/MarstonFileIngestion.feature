@Opal @PO-6439 @BaisReportFixture
Feature: Marston file ingestion

  Scenario: A valid Marston file is ingested
    Given the configured "Marston" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "MARSTON" interface file is stored
    And the stored "Marston" report can be retrieved by interface file ID
    And the stored "Marston" report content matches the bais "file"
    And the configured "Marston" report no longer exists on bais

  Scenario: A duplicate Marston file is detected
    Given the configured "Marston" report has already been processed
    When the configured "Marston" report is placed on bais
    And the "Marston" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "MARSTON" interface file is recorded
    And the duplicate "MARSTON" interface file reuses the original blob
    And the configured "Marston" report no longer exists on bais

  Scenario: An unsupported Marston file is ignored
    Given the configured "Marston" report has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "MARSTON" interface file is stored for the unsupported report
    And the unsupported "Marston" report remains on bais

  Scenario: Invalid Marston content is recorded as failed with its source blob
    Given the configured "Marston" report has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "MARSTON" interface file is recorded with its source blob
    And the invalid "Marston" report remains on bais
