@Opal @JIRA-LABEL:file-handler-service @JIRA-STORY:PO-6382 @PO-5609 @BaisReportFixture
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

  Scenario: A duplicate BTEckoh report is detected
    Given the configured "BTEckoh" report has already been processed
    When the configured "BTEckoh" report is placed on bais
    And the "BTEckoh" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "BTECKOH_REPORT" interface file is recorded
    And the duplicate "BTECKOH_REPORT" interface file reuses the original blob
    And the configured "BTEckoh" report no longer exists on bais

  Scenario: An unsupported BTEckoh report is ignored
    Given the configured "BTEckoh" report has an unsupported file on bais
    When the "BTEckoh" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "BTECKOH_REPORT" interface file is stored for the unsupported report
    And the unsupported "BTEckoh" report remains on bais

  Scenario: Invalid BTEckoh report content is recorded as failed without a source blob
    Given the configured "BTEckoh" report has invalid content on bais
    When the "BTEckoh" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "BTECKOH_REPORT" interface file is recorded without a source blob
    And the invalid "BTEckoh" report remains on bais
