@Opal @PO-6426 @BaisReportFixture
Feature: Allpay file ingestion

  Scenario: A valid Allpay file is ingested
    Given the configured "Allpay" file is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais

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
    And the unsupported "Allpay" report remains on bais

  Scenario: Invalid Allpay content is recorded as failed with its source blob
    Given the configured "Allpay" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "ALLPAY" interface file is recorded with its source blob
    And the invalid "Allpay" file remains on bais
