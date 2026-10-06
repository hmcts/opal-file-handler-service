@Opal @PO-6439 @BaisReportFixture
Feature: Marston file ingestion

  Scenario: A valid Marston file is ingested
    Given the configured "Marston" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "MARSTON" interface file is stored
    And the configured "Marston" report no longer exists on bais

  Scenario: A duplicate Marston file is detected
    Given the configured "Marston" report has already been processed
    When the configured "Marston" report is placed on bais
    And the "Marston" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "MARSTON" interface file is recorded
    And the duplicate "MARSTON" interface file reuses the original blob
    And the configured "Marston" report no longer exists on bais
