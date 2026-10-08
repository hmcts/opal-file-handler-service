@Ignore @Opal @PO-6426 @BaisReportFixture
Feature: Allpay file ingestion

  Scenario: A valid Allpay file is ingested
    Given the configured "Allpay" report is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the configured "Allpay" report no longer exists on bais

  Scenario: A duplicate Allpay file is detected
    Given the configured "Allpay" report has already been processed
    When the configured "Allpay" report is placed on bais
    And the "Allpay" report ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "ALLPAY" interface file is recorded
    And the duplicate "ALLPAY" interface file reuses the original blob
    And the configured "Allpay" report no longer exists on bais
