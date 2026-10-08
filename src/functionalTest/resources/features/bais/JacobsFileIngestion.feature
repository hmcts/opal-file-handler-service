@Ignore @Opal @PO-6437 @BaisReportFixture
Feature: Jacobs file ingestion

  Scenario: A valid Jacobs file is ingested
    Given the configured "Jacobs" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Jacobs" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "JACOBS" interface file is stored
    And the configured "Jacobs" report no longer exists on bais

  Scenario: A duplicate Jacobs file is detected
    Given the configured "Jacobs" report has already been processed
    When the configured "Jacobs" report is placed on bais
    And the "Jacobs" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "JACOBS" interface file is recorded
    And the duplicate "JACOBS" interface file reuses the original blob
    And the configured "Jacobs" report no longer exists on bais
