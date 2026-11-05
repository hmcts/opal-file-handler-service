@Opal @PO-6439 @BaisFileFixture
Feature: Marston file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid Marston file is ingested
    Given the configured "Marston" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "MARSTON" interface file is stored
    And the stored "Marston" file can be retrieved by interface file ID
    And the stored "Marston" file content matches the bais "file"
    And the "Marston" JSON file contains the following extracted data
      | field                                                | value      |
      | payment_type                                         | CASH       |
      | destination_details.bank_details.sort_code           | 560038     |
      | destination_details.bank_details.account_number      | 27048532   |
      | transactions.size()                                  | 1          |
      | transactions[0].amount                               | 1000       |
      | transactions[0].transaction_code                     | 99         |
      | dwp_court_code                                       | DWP1234568 |
      | transactions[0].originator_details.account_reference | 23000106E  |
      | transactions[0].date_entry_applied                   | 03/09/2023 |
    And the configured "Marston" file no longer exists on bais

  Scenario: A duplicate Marston file is detected
    Given the configured "Marston" file has already been processed
    When the configured "Marston" file is placed on bais
    And the "Marston" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "MARSTON" interface file is recorded
    And the duplicate "MARSTON" interface file reuses the original blob
    And the configured "Marston" file no longer exists on bais

  Scenario: An unsupported Marston file is ignored
    Given the configured "Marston" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "MARSTON" interface file is stored for the unsupported file
    And the unsupported "Marston" file remains on bais

  Scenario: Invalid Marston content is recorded as failed with its source blob
    Given the configured "Marston" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Marston" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "MARSTON" interface file is recorded with its source blob
    And the invalid "Marston" file remains on bais
