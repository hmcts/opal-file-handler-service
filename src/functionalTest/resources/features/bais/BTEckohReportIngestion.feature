@Opal @JIRA-LABEL:file-handler-service @JIRA-STORY:PO-5609 @BteckohReportFixture
Feature: BTEckoh report ingestion

  Scenario: A valid BTEckoh report is ingested
    Given the configured BTEckoh report is available on bais
    When the BTEckoh report ingestion task is triggered
    Then a successful BTECKOH_REPORT interface file is stored
    And the stored BTEckoh report content matches the bais workbook
    And the configured BTEckoh report no longer exists on bais

  Scenario: An unsupported BTEckoh filename is ignored
    Given a BTEckoh report with an unsupported filename is available on bais
    When the BTEckoh report ingestion task is triggered
    Then no interface file is created for the unsupported BTEckoh filename
    And the unsupported BTEckoh file remains on bais

  Scenario: A previously successful BTEckoh report is recorded as a duplicate
    Given the configured BTEckoh report is available on bais
    And the configured BTEckoh report has already been ingested successfully
    And the same BTEckoh report is uploaded again
    When the BTEckoh report ingestion task is triggered
    Then one successful and one duplicate BTEckoh interface file are stored
    And the configured BTEckoh report no longer exists on bais
