@Opal @PO-6438 @BaisFileFixture
Feature: CDER file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid CDER file is ingested
    Given the configured "Cder" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Cder" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "CDER" interface file is stored
    And the stored "Cder" file can be retrieved by interface file ID
    And the stored "Cder" file content matches the bais "file"
    And the "Cder" JSON file contains the following extracted data
      | field                                                | value      |
      | payment_type                                         | CASH       |
      | destination_details.bank_details.sort_code           | 560036     |
      | destination_details.bank_details.account_number      | 27048530   |
      | transactions.size()                                  | 2          |
      | transactions[0].amount                               | 1000       |
      | transactions[0].transaction_code                     | 99         |
      | dwp_court_code                                       | 0000031715 |
      | transactions[0].originator_details.account_reference | 23000106E  |
      | transactions[0].date_entry_applied                   | 03/09/2023 |
      | transactions[1].amount                               | 5000       |
      | transactions[1].transaction_code                     | 99         |
      | transactions[1].originator_details.account_reference | 23000113P  |
      | transactions[1].date_entry_applied                   | 01/09/2023 |
    And the configured "Cder" file no longer exists on bais

  Scenario: A duplicate CDER file is detected
    Given the configured "Cder" file has already been processed
    When the configured "Cder" file is placed on bais
    And the "Cder" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "CDER" interface file is recorded
    And the duplicate "CDER" interface file reuses the original blob
    And the configured "Cder" file no longer exists on bais

  Scenario: An unsupported CDER file is ignored
    Given the configured "Cder" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Cder" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "CDER" interface file is stored for the unsupported file
    And the unsupported "Cder" file remains on bais

  Scenario: Invalid CDER content is recorded as failed with its source blob
    Given the configured "Cder" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Cder" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "CDER" interface file is recorded with its source blob
    And the invalid "Cder" file remains on bais
