@Opal @PO-6426 @BaisFileFixture
Feature: Allpay file ingestion

  @JIRA-LABEL:file-handler-service @JIRA-EPIC:PO-3497 @JIRA-STORY:PO-7244
  Scenario: A valid Allpay file is ingested
    Given the configured "Allpay" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the "Allpay" file is processed successfully
    And the extracted "Allpay" payments are ready for the external interface

  Scenario: A duplicate Allpay file is detected
    Given the configured "Allpay" file has already been processed
    When the configured "Allpay" file is placed on bais
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "ALLPAY" interface file is recorded
    And the duplicate "ALLPAY" interface file reuses the original blob
    And the configured "Allpay" file no longer exists on bais

  Scenario: An unsupported Allpay file is ignored
    Given the configured "Allpay" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "ALLPAY" interface file is stored for the unsupported file
    And the unsupported "Allpay" file remains on bais

  Scenario: Invalid Allpay content is recorded as failed with its source blob
    Given the configured "Allpay" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "ALLPAY" interface file is recorded with its source blob
    And the invalid "Allpay" file remains on bais
