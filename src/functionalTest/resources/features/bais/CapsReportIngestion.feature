@EI1 @Opal @JIRA-LABEL:file-handler-service @JIRA-STORY:PO-6382 @CapsReportFixture
Feature: CAPS report ingestion

  Scenario: A valid CAPS report is ingested
    Given the configured CAPS report is available on bais
    When the CAPS report ingestion task is triggered
    Then a successful CAPS_REPORT interface file is stored
    And the stored CAPS report content matches the bais file
    And the configured CAPS report no longer exists on bais

  Scenario: An unsupported CAPS filename is ignored
    Given a CAPS report with an unsupported filename is available on bais
    And the CAPS blobstore and interface records are recorded
    When the CAPS report ingestion task is triggered
    Then the CAPS blobstore is unchanged
    And no interface file is created for the unsupported CAPS filename
    And the unsupported CAPS file remains on bais

  Scenario: A previously successful CAPS report is recorded as a duplicate
    Given the configured CAPS report is available on bais
    And the configured CAPS report has already been ingested successfully
    And the same CAPS report is uploaded again
    When the CAPS report ingestion task is triggered
    Then one successful and one duplicate CAPS interface file are stored
    And the configured CAPS report no longer exists on bais

  Scenario: Running again with no new CAPS files preserves existing storage
    Given the configured CAPS report is available on bais
    And the configured CAPS report has already been ingested successfully
    And the configured CAPS report no longer exists on bais
    And the CAPS blobstore and interface records are recorded
    When the CAPS report ingestion task is triggered
    Then the CAPS blobstore is unchanged
    And the CAPS interface records are unchanged
    And the stored CAPS report content matches the bais file

  Scenario: A disabled CAPS job leaves the available report untouched
    Given the configured CAPS report is available on bais
    And the CAPS blobstore and interface records are recorded
    When the CAPS report ingestion task is triggered with its feature flag disabled
    Then the CAPS blobstore is unchanged
    And the CAPS interface records are unchanged
    And the configured CAPS report is available on bais

  Scenario: A duplicate CAPS report does not create another blob
    Given the configured CAPS report is available on bais
    And the configured CAPS report has already been ingested successfully
    And the same CAPS report is uploaded again
    And the CAPS blobstore and interface records are recorded
    When the CAPS report ingestion task is triggered
    Then the CAPS blobstore is unchanged
    And one successful and one duplicate CAPS interface file are stored
    And the configured CAPS report no longer exists on bais

  Scenario: A malformed CAPS report is not saved to blob storage
    Given a malformed CAPS report with a supported filename is available on bais
    And the CAPS blobstore and interface records are recorded
    When the CAPS report ingestion task is triggered
    Then the CAPS blobstore is unchanged
    And a failed CAPS report is recorded without a blob
    And the configured CAPS report is available on bais
