@Opal @PO-6437 @BaisFileFixture
Feature: Jacobs file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid Jacobs file is ingested
    Given the configured "Jacobs" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Jacobs" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "JACOBS" interface file is stored
    And the stored "Jacobs" file can be retrieved by interface file ID
    And the stored "Jacobs" file content matches the bais "file"
    And the "Jacobs" JSON file contains the following extracted data
      | field                                                | value      |
      | payment_type                                         | CASH       |
      | destination_details.bank_details.sort_code           | 560037     |
      | destination_details.bank_details.account_number      | 27048531   |
      | transactions.size()                                  | 2          |
      | transactions[0].amount                               | 1000       |
      | transactions[0].transaction_code                     | 99         |
      | dwp_court_code                                       | 0000031716 |
      | transactions[0].originator_details.account_reference | 23000106E  |
      | transactions[0].date_entry_applied                   | 03/09/2023 |
      | transactions[1].amount                               | 5000       |
      | transactions[1].transaction_code                     | 99         |
      | transactions[1].originator_details.account_reference | 23000113P  |
      | transactions[1].date_entry_applied                   | 01/09/2023 |
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
