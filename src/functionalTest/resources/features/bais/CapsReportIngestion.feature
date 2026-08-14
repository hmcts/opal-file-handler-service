@Opal @JIRA-LABEL:file-handler-service @JIRA-STORY:PO-5608 @CapsReportFixture
Feature: CAPS report ingestion

  Scenario: A valid CAPS report is ingested
    Given the configured CAPS report is available on bais
    When the CAPS report ingestion task is triggered
    Then a successful CAPS_REPORT interface file is stored
    And the stored CAPS report content matches the bais file
    And the configured CAPS report no longer exists on bais

  Scenario: An unsupported CAPS filename is ignored
    Given a CAPS report with an unsupported filename is available on bais
    When the CAPS report ingestion task is triggered
    Then no interface file is created for the unsupported CAPS filename
    And the unsupported CAPS file remains on bais

  Scenario: A previously successful CAPS report is recorded as a duplicate
    Given the configured CAPS report is available on bais
    And the configured CAPS report has already been ingested successfully
    And the same CAPS report is uploaded again
    When the CAPS report ingestion task is triggered
    Then one successful and one duplicate CAPS interface file are stored
    And the configured CAPS report no longer exists on bais
