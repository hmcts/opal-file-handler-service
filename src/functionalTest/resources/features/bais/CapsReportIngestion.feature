@Opal @JIRA-LABEL:file-handler-service @JIRA-STORY:PO-6382 @CapsReportFixture
Feature: CAPS report ingestion

  Scenario: A valid CAPS report is ingested
    Given the configured CAPS report is available on bais
    When the CAPS report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful CAPS_REPORT interface file is stored
    And the stored CAPS report content matches the bais file
    And the configured CAPS report no longer exists on bais
