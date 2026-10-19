@Opal @PO-6398 @BaisReportFixture
Feature: NatWest file ingestion

  Scenario: A valid NatWest file is ingested
    Given the configured "NatWest" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "NATWEST" interface file is stored
    And the stored "NatWest" file can be retrieved by interface file ID
    And the stored "NatWest" file content matches the bais "file"
    And the configured "NatWest" file no longer exists on bais

  Scenario: A duplicate NatWest file is detected
    Given the configured "NatWest" file has already been processed
    When the configured "NatWest" file is placed on bais
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "NATWEST" interface file is recorded
    And the duplicate "NATWEST" interface file reuses the original blob
    And the configured "NatWest" file no longer exists on bais

  Scenario: An unsupported NatWest file is ignored
    Given the configured "NatWest" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "NATWEST" interface file is stored for the unsupported file
    And the unsupported "NatWest" file remains on bais

  Scenario: Invalid NatWest content is recorded as failed with its source blob
    Given the configured "NatWest" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "NatWest" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "NATWEST" interface file is recorded with its source blob
    And the invalid "NatWest" file remains on bais
