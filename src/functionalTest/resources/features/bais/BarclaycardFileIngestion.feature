@Opal @PO-6427 @BaisReportFixture
Feature: Barclaycard file ingestion

  Scenario: A valid Barclaycard file is ingested
    Given the configured "Barclaycard" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Barclaycard" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "BARCLAYCARD" interface file is stored
    And the stored "Barclaycard" file can be retrieved by interface file ID
    And the stored "Barclaycard" file content matches the bais "file"
    And the configured "Barclaycard" file no longer exists on bais

  Scenario: A duplicate Barclaycard file is detected
    Given the configured "Barclaycard" file has already been processed
    When the configured "Barclaycard" file is placed on bais
    And the "Barclaycard" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "BARCLAYCARD" interface file is recorded
    And the duplicate "BARCLAYCARD" interface file reuses the original blob
    And the configured "Barclaycard" file no longer exists on bais

  Scenario: An unsupported Barclaycard file is ignored
    Given the configured "Barclaycard" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Barclaycard" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "BARCLAYCARD" interface file is stored for the unsupported file
    And the unsupported "Barclaycard" file remains on bais

  Scenario: Invalid Barclaycard content is recorded as failed with its source blob
    Given the configured "Barclaycard" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Barclaycard" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "BARCLAYCARD" interface file is recorded with its source blob
    And the invalid "Barclaycard" file remains on bais
