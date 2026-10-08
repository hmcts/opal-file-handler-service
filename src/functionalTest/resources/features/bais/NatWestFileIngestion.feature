@Opal @PO-6398 @BaisReportFixture
Feature: NatWest file ingestion

  Scenario: A valid NatWest file is ingested
    Given the configured "NatWest" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "NATWEST" interface file is stored
    And the configured "NatWest" report no longer exists on bais

  Scenario: A duplicate NatWest file is detected
    Given the configured "NatWest" report has already been processed
    When the configured "NatWest" report is placed on bais
    And the "NatWest" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "NATWEST" interface file is recorded
    And the duplicate "NATWEST" interface file reuses the original blob
    And the configured "NatWest" report no longer exists on bais
