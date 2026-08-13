@Opal @JIRA-LABEL:file-handler-service @InterfaceFileDbFixture
Feature: Get Interface Files

  Background:
    Given I am testing as the "opal-test@dev.platform.hmcts.net" user

  @JIRA-STORY:PO-3947 @JIRA-EPIC:PO-3495
  Scenario: Returns interface files correctly
    When I request all interface files
    Then the response status code is 200
    And at least 4 interface files are returned
    And the interface file with filestore UUID "f0000000-0000-0000-0000-000000000003" has details:
      | source           | BTECKOH_REPORT                        |
      | target           | OPAL                                  |
      | type             | SOURCE                                |
      | domain           | FILE_HANDLER                          |
      | status           | FAILED                                |
      | file_name        | 2500-Payments-Report-Daily.xlsx       |
      | errors           | {"error":"malformed xlsx"}            |
      | created_datetime | 2026-01-04T12:30:00                   |
      | checksum         | null                                  |

  @JIRA-STORY:PO-3947 @JIRA-EPIC:PO-3495
  Scenario: Filters interface files correctly by status and source
    When I request interface files with source "CAPS_REPORT" and status "SUCCESS"
    Then the response status code is 200
    And at least 1 interface files are returned
    And every returned interface file has details:
      | source | CAPS_REPORT |
      | status | SUCCESS     |

  @JIRA-STORY:PO-3947 @JIRA-EPIC:PO-3495
  Scenario: Filters interface files correctly by target and type
    When I request interface files with target "OPAL" and type "SOURCE"
    Then the response status code is 200
    And at least 1 interface files are returned
    And every returned interface file has details:
      | target | OPAL   |
      | type   | SOURCE |

  @JIRA-STORY:PO-3947 @JIRA-EPIC:PO-3495
  Scenario: Filters interface files correctly by domain
    When I request interface files with domain "FINES"
    Then the response status code is 200
    And at least 1 interface files are returned
    And every returned interface file has details:
      | domain | FINES |

  @JIRA-STORY:PO-3947 @JIRA-EPIC:PO-3495
  Scenario: Filters interface files correctly by to and from dates
    When I request interface files from date "2025-12-30T00:00" to date "2026-01-04T12:30"
    Then the response status code is 200
    And at least 1 interface files are returned
    And every returned interface file was created from date "2025-12-30T00:00" to date "2026-01-04T12:30"

  @JIRA-STORY:PO-3947 @JIRA-EPIC:PO-3495
  Scenario: Returns not acceptable for a non-existent status
    When I request interface files with source "CAPS_REPORT" and status "INVALID_STATUS"
    Then the response status code is 406
