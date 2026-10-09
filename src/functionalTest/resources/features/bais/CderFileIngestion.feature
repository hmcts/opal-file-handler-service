@Opal @PO-6438 @BaisReportFixture
Feature: CDER file ingestion

  Scenario: A valid CDER file is ingested
    Given the configured "Cder" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Cder" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "CDER" interface file is stored
    And the configured "Cder" report no longer exists on bais

  Scenario: A duplicate CDER file is detected
    Given the configured "Cder" report has already been processed
    When the configured "Cder" report is placed on bais
    And the "Cder" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "CDER" interface file is recorded
    And the duplicate "CDER" interface file reuses the original blob
    And the configured "Cder" report no longer exists on bais
