@Opal @PO-6426 @BaisFileFixture
Feature: Allpay file ingestion

  Scenario: A valid Allpay file with mixed transaction codes is ingested
    Given the configured "Allpay" file "a121_00000001_000001.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the stored "Allpay" JSON file has payment type "CASH" and transaction codes in order
      | 99 |
      | 93 |
      | 68 |
      | 00 |
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file has payment type "CASH" and transaction codes in order
      | 99 |
      | 99 |
      | 68 |
      | 00 |
    And the transformed "Allpay" file contains the account references in order
      | 88007700000001 |
      | 88007700000001 |
      | 88007700000001 |
      | 88007700000001 |

  Scenario: An Allpay file for a nonexistent account is ingested
    Given the configured "Allpay" file "a121_00000001_000003.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file contains the account references in order
      | 12345678A |

  Scenario: An Allpay file with a valid APR is ingested
    Given the configured "Allpay" file "a121_00000001_000004.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file contains the account references in order
      | 88007700000001 |

  Scenario: An Allpay file with the same valid APR for a different business unit is ingested
    Given the configured "Allpay" file "a121_00000001_000016.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file in business unit 78
    And the transformed "Allpay" file contains the account references in order
      | 88007800000001 |

  Scenario: An Allpay file with an originator name set up as an APR is ingested
    Given the configured "Allpay" file "a121_00000001_000005.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file contains the account references in order
      | 88007700000001 |

  Scenario: An Allpay file with a master and child account is ingested
    Given the configured "Allpay" file "a121_00000001_000006.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file contains the account references in order
      | 77000002A |
      | 77000003A |

  Scenario: An Allpay file with a zero value payment is ingested
    Given the configured "Allpay" file "a121_00000001_000007.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file

  Scenario Outline: An Allpay file with a large payment for a <size> value account is ingested
    Given the configured "Allpay" file "<file>" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file

    Examples:
      | size            | file                      |
      | large           | a121_00000001_000008.dat  |
      | small           | a121_00000001_000009.dat  |

  Scenario: Allpay account reference prefixes and suffixes are preserved and surrounding spaces are trimmed
    Given the configured "Allpay" file "a121_00000001_000011.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the stored "Allpay" JSON file contains the account references in order
      | ABC77000001A    |
      | 77000001AABC    |
      | ABC77000001AABC |
      | 77000001A       |
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file contains the account references in order
      | 77000001A |
      | 77000001A |
      | 77000001A |
      | 77000001A |

  Scenario: An Allpay file with a cheque only payment is ingested
    Given the configured "Allpay" file "a121_00000001_000013.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file has payment type "CHEQUE" and transaction codes in order
      | 11 |
      | 11 |

  Scenario: An Allpay file with mixed transaction codes is ingested as a cheque file
    Given the configured "Allpay" file "a121_00000001_000014.dat" with transaction codes in order is available on bais
      | 11 |
      | 99 |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the stored "Allpay" JSON file has payment type "CHEQUE" and transaction codes in order
      | 11 |
      | 99 |
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file has payment type "CHEQUE" and transaction codes in order
      | 11 |
      | 99 |

  Scenario: An Allpay cheque file ignores transaction code 44 totals rows
    Given the configured "Allpay" file "a121_00000001_000015.dat" with transaction codes in order is available on bais
      | 11 |
      | 44 |
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a successful "ALLPAY" interface file is stored
    And the stored "Allpay" file can be retrieved by interface file ID
    And the stored "Allpay" file content matches the bais "file"
    And the stored "Allpay" JSON file has payment type "CHEQUE" and transaction codes in order
      | 11 |
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing completes for the stored "Allpay" file
    And the transformed "Allpay" file has payment type "CHEQUE" and transaction codes in order
      | 11 |

  Scenario: An Allpay file with invalid transaction code AB is recorded as failed
    Given the configured "Allpay" file "a121_00000001_000002.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "ALLPAY" interface file is recorded with its source blob
    And the "Allpay" file failure reports a nonnumeric transaction code
    And the invalid "Allpay" file remains on bais

  Scenario: An Allpay file with a missing originator account reference is recorded as failed
    Given the configured "Allpay" file "a121_00000001_000010.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "ALLPAY" interface file is recorded with its source blob
    And the "Allpay" file failure reports a missing originator account reference
    And the invalid "Allpay" file remains on bais

  Scenario: An Allpay file with an unmapped destination bank account is recorded as failed
    Given the configured "Allpay" file "a121_00000001_000012.dat" is available on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "ALLPAY" interface file is recorded with its source blob
    And the "Allpay" file failure reports no destination bank mapping for sort code "111111" and account number "22222222"
    And the invalid "Allpay" file remains on bais

  Scenario: A duplicate Allpay file is detected
    Given the configured "Allpay" file has already been processed
    And Fines preprocessing completes for the stored "Allpay" file
    When the configured "Allpay" file is placed on bais
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a duplicate "ALLPAY" interface file is recorded
    And the duplicate "ALLPAY" interface file reuses the original blob
    And the configured "Allpay" file no longer exists on bais
    And Fines preprocessing reuses the original result for the stored "Allpay" file

  Scenario: An unsupported Allpay file is ignored
    Given the configured "Allpay" file has an unsupported file on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And no "ALLPAY" interface file is stored for the unsupported file
    And the unsupported "Allpay" file remains on bais

  Scenario: Malformed Allpay content is recorded as failed with its source blob
    Given the configured "Allpay" file has invalid content on bais
    When I am testing as the "opal-test@dev.platform.hmcts.net" user
    And the "Allpay" file ingestion job is requested through testing support
    Then the testing-support request is accepted
    And a failed "ALLPAY" interface file is recorded with its source blob
    And the invalid "Allpay" file remains on bais
