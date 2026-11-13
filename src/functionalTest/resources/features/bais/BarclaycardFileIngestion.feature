@Opal @PO-6427 @BaisFileFixture
Feature: Barclaycard file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid Barclaycard file with mixed transaction codes is ingested
    Given the configured "Barclaycard" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Barclaycard" file ingestion job is requested through testing support
    Then the "Barclaycard" file is processed successfully
    And the extracted "Barclaycard" payments are ready for the external interface
    And the stored "Barclaycard" JSON file contains the account references in order
      | 77100001A |
      | BC-APR-01 |
    And Fines preprocessing completes for the stored "Barclaycard" file
    And the transformed "Barclaycard" file has payment type "CASH" and transaction codes in order
      | 99 |
      | 99 |
    And the transformed "Barclaycard" file contains the account references in order
      | 77100001A |
      | 88007710000002 |
    And the transformed "Barclaycard" file contains transaction amounts in pence in order
      | 1500 |
      | 500 |

  Scenario: A duplicate Barclaycard file is detected
    Given the configured "Barclaycard" file has already been processed
    And Fines preprocessing completes for the stored "Barclaycard" file
    When the configured "Barclaycard" file is placed on bais
    And the "Barclaycard" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "BARCLAYCARD" interface file is recorded
    And the duplicate "BARCLAYCARD" interface file reuses the original blob
    And the configured "Barclaycard" file no longer exists on bais
    And Fines preprocessing reuses the original result for the stored "Barclaycard" file

  Scenario: An unsupported Barclaycard file is ignored
    Given the configured "Barclaycard" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Barclaycard" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "BARCLAYCARD" interface file is stored for the unsupported file
    And the unsupported "Barclaycard" file remains on bais

  Scenario: Malformed Barclaycard content is recorded as failed with its source blob
    Given the configured "Barclaycard" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Barclaycard" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "BARCLAYCARD" interface file is recorded with its source blob
    And the invalid "Barclaycard" file remains on bais
