@Opal @PO-7228 @PO-6428 @BaisReportFixture
Feature: BTEckoh file ingestion

  Scenario: A valid BTEckoh transfer file is ingested
    Given the configured "BTEckoh transfer" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "BTEckoh transfer" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "BTECKOH" interface file is stored
    And the configured "BTEckoh transfer" report no longer exists on bais

  Scenario: A duplicate BTEckoh transfer file is detected
    Given the configured "BTEckoh transfer" report has already been processed
    When the configured "BTEckoh transfer" report is placed on bais
    And the "BTEckoh transfer" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "BTECKOH" interface file is recorded
    And the duplicate "BTECKOH" interface file reuses the original blob
    And the configured "BTEckoh transfer" report no longer exists on bais
