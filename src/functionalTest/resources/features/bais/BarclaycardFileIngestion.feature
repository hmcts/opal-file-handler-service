@Opal @PO-7228 @PO-6427 @BaisReportFixture
Feature: Barclaycard file ingestion

  Scenario: A valid Barclaycard file is ingested
    Given the configured "Barclaycard" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Barclaycard" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "BARCLAYCARD" interface file is stored
    And the configured "Barclaycard" report no longer exists on bais

  Scenario: A duplicate Barclaycard file is detected
    Given the configured "Barclaycard" report has already been processed
    When the configured "Barclaycard" report is placed on bais
    And the "Barclaycard" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "BARCLAYCARD" interface file is recorded
    And the duplicate "BARCLAYCARD" interface file reuses the original blob
    And the configured "Barclaycard" report no longer exists on bais
