@Opal @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-6436 @DwpFileFixture
Feature: DWP file ingestion

  # The fixture maps DWP1234567 to business unit DW01 and its MAINTENANCE bank account.
  Scenario: A valid DWP file is ingested
    Given the configured DWP report is available on bais
    When the DWP report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful DWP interface file is stored
    And the stored DWP report content matches the bais file
    And the DWP JSON file contains the extracted payments and bank details
    And the configured DWP report no longer exists on bais
