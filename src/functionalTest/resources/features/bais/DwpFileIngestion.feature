@Opal @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-6436 @BaisReportFixture
Feature: DWP file ingestion

  # The fixture maps DWP1234567 to business unit DW01 and its MAINTENANCE bank account.
  Scenario: A valid DWP file is ingested
    Given the configured "DWP" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "DWP" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "DWP" interface file is stored
    And the stored "DWP" report content matches the bais "file"
    And the DWP JSON file contains the extracted payments and bank details
    And the configured "DWP" report no longer exists on bais

  Scenario: A duplicate DWP file is detected
    Given the configured "DWP" report has already been processed
    When the configured "DWP" report is placed on bais
    And the "DWP" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "DWP" interface file is recorded
    And the duplicate "DWP" interface file reuses the original blob
    And the configured "DWP" report no longer exists on bais
