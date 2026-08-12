@Opal @JIRA-LABEL:file-handler-service @InterfaceFileContentDbFixture @InterfaceFileContentBlobFixture
Feature: Get Interface File Content

  # Tagged fixture hooks manage local and staging data; Jenkins prepares PR database data around the test stage.
  Background:
    Given I am testing as the "opal-test@dev.platform.hmcts.net" user

  @JIRA-STORY:PO-3948 @JIRA-EPIC:PO-3495
  Scenario: Get interface file content
    When I request the content for interface file 9000000000000001
    Then the response status code is 200
    And the interface file content type is application octet stream
    And the interface file content matches the classpath resource "test-data/bteckoh-report/bteckoh-test-file.xlsx"

  @JIRA-STORY:PO-3948 @JIRA-EPIC:PO-3495
  Scenario: Non-existent file returns not found
    When I request the content for interface file 9000000000000004
    Then the response status code is 404
    And the interface file response detail contains "could not be located"

  @JIRA-STORY:PO-3948 @JIRA-EPIC:PO-3495
  Scenario: File with failed status returns as unprocessable
    When I request the content for interface file 9000000000000002
    Then the response status code is 422
    And the interface file response detail contains "could not be retrieved"

  @JIRA-STORY:PO-3948 @JIRA-EPIC:PO-3495
  Scenario: File does not exist on blobstore
    When I request the content for interface file 9000000000000003
    Then the response status code is 500
    And the interface file response detail contains "to exist in blobstore container"
