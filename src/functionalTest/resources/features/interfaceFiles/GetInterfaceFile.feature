@Opal @JIRA-LABEL:file-handler-service @InterfaceFileDbFixture
Feature: Get Interface File

  Background:
    Given I am testing as the "opal-test@dev.platform.hmcts.net" user

  @JIRA-STORY:PO-7205 @JIRA-EPIC:PO-3495
  Scenario: Returns the requested interface file details
    When I call GET "/interface-files/9000000000000001"
    Then the response status code is 200
    And the response is as expected:
      | interface_file_id | 9000000000000001                     |
      | checksum          | d553f8f289bd08e5c513de5c000c0374     |
      | domain            | FILE_HANDLER                         |
      | errors            | null                                 |
      | file_name         | bteckoh-test-file.xlsx               |
      | filestore_uuid    | f0000000-0000-0000-0000-000000000001 |
      | source            | BTECKOH_REPORT                       |
      | status            | SUCCESS                              |
      | target            | BTECKOH_REPORT                       |
      | type              | SOURCE                               |

  @JIRA-STORY:PO-7205 @JIRA-EPIC:PO-3495
  Scenario: Returns not found for an unknown interface file ID
    When I call GET "/interface-files/9000000000000512"
    Then the response status code is 404
    And the response is as expected:
      | detail    | Interface file with id 9000000000000512 could not be located. |
      | status    | 404                                                           |
      | title     | Not Found                                                     |
      | type      | https://hmcts.gov.uk/problems/response-status                 |
      | retriable | false                                                         |

  @JIRA-STORY:PO-7205 @JIRA-EPIC:PO-3495
  Scenario: Rejects a request with an invalid token
    When I call GET "/interface-files/9000000000000001" with an invalid token
    Then the response status code is 401
    And the response is as expected:
      | type      | https://hmcts.gov.uk/problems/unauthorized |
      | title     | Unauthorized                               |
      | status    | 401                                        |
      | retriable | false                                      |

  @JIRA-STORY:PO-7205 @JIRA-EPIC:PO-3495
  Scenario: Rejects a request when the user has no view interface files permission
    Given I am testing as the "opal-test-2@dev.platform.hmcts.net" user
    When I call GET "/interface-files/9000000000000001"
    Then the response status code is 403

  @JIRA-STORY:PO-7205 @JIRA-EPIC:PO-3495
  Scenario: Rejects an interface file request with an invalid ID format
    When I call GET "/interface-files/110000000X"
    Then the response status code is 406
    And the response is as expected:
      | detail    | Invalid parameter value format                                                                                                              |
      | status    | 406                                                                                                                                         |
      | title     | Not Acceptable                                                                                                                              |
      | type      | https://hmcts.gov.uk/problems/type-mismatch                                                                                                 |
      | retriable | false                                                                                                                                       |
      | reason    | Method parameter 'id': Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: "110000000X" |

  @Ignore @JIRA-STORY:PO-7205 @JIRA-EPIC:PO-3495
  Scenario: Returns feature disabled when the banking interface feature is disabled
    When I call GET "/interface-files/9000000000000001"
    Then the response status code is 404
    And the response body contains "The requested feature is not currently available"
