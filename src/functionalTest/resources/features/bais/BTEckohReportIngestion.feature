@Opal @JIRA-LABEL:file-handler-service @JIRA-STORY:PO-6382 @BteckohReportFixture
Feature: BTEckoh report ingestion

  @JIRA-STORY:PO-7205
  Scenario: A valid BTEckoh report is ingested
    Given the configured "BTEckoh" report is available on bais
    When the "BTEckoh" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "BTECKOH_REPORT" interface file is stored
    And the stored BTEckoh report can be retrieved by interface file ID
    And the stored "BTEckoh" report content matches the bais "workbook"
    And the configured "BTEckoh" report no longer exists on bais
