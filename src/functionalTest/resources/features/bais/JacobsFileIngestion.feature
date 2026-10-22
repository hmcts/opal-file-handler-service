@Opal @PO-6437 @BaisFileFixture
Feature: Jacobs file ingestion

  Scenario: A valid Jacobs file is ingested
    Given the configured "Jacobs" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Jacobs" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "JACOBS" interface file is stored
    And the stored "Jacobs" file can be retrieved by interface file ID
    And the stored "Jacobs" file content matches the bais "file"
    And the configured "Jacobs" file no longer exists on bais

  Scenario: A duplicate Jacobs file is detected
    Given the configured "Jacobs" file has already been processed
    When the configured "Jacobs" file is placed on bais
    And the "Jacobs" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "JACOBS" interface file is recorded
    And the duplicate "JACOBS" interface file reuses the original blob
    And the configured "Jacobs" file no longer exists on bais

  Scenario: An unsupported Jacobs file is ignored
    Given the configured "Jacobs" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Jacobs" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "JACOBS" interface file is stored for the unsupported file
    And the unsupported "Jacobs" file remains on bais

  Scenario: Invalid Jacobs content is recorded as failed with its source blob
    Given the configured "Jacobs" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Jacobs" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "JACOBS" interface file is recorded with its source blob
    And the invalid "Jacobs" file remains on bais
