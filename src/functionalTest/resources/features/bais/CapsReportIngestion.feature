@Opal @JIRA-LABEL:file-handler-service @JIRA-STORY:PO-6382 @PO-5608 @BaisFileFixture
Feature: CAPS report ingestion

  @JIRA-STORY:PO-7205
  Scenario: A valid CAPS report is ingested
    Given the configured "CAPS" report is available on bais
    When the "CAPS" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "CAPS_REPORT" interface file is stored
    And the stored CAPS report can be retrieved by interface file ID
    And the stored "CAPS" report content matches the bais "file"
    And the configured "CAPS" report no longer exists on bais

  Scenario: A duplicate CAPS report is detected
    Given the configured "CAPS" report has already been processed
    When the configured "CAPS" report is placed on bais
    And the "CAPS" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "CAPS_REPORT" interface file is recorded
    And the duplicate "CAPS_REPORT" interface file reuses the original blob
    And the configured "CAPS" report no longer exists on bais

  Scenario: An unsupported CAPS report is ignored
    Given the configured "CAPS" report has an unsupported file on bais
    When the "CAPS" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "CAPS_REPORT" interface file is stored for the unsupported report
    And the unsupported "CAPS" report remains on bais

  Scenario: Invalid CAPS report content is recorded as failed without a source blob
    Given the configured "CAPS" report has invalid content on bais
    When the "CAPS" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "CAPS_REPORT" interface file is recorded without a source blob
    And the invalid "CAPS" report remains on bais
