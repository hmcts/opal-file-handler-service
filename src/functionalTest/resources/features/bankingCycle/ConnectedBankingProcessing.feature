@Opal @ConnectedBanking @JIRA-STORY:PO-7244 @JIRA-EPIC:PO-3497 @JIRA-LABEL:file-handler-service
Feature: DWP and Bailiffs reject invalid files before downstream processing
  # Requires the existing local stack and isolated reference-data access.

  @ConnectedInvalid
  Scenario Outline: Invalid <supplier> content cannot reach Fines ANT
    Given a connected "<supplier>" banking file for the "invalid" case
    When the connected banking ingestion job runs
    Then the connected banking file is rejected with "<error>"

    Examples:
      | supplier | error                |
      | DWP      | was not valid XML    |
      | Jacobs   | was not valid XML    |
      | Cder     | was not valid XML    |
      | Marston  | was not valid XML    |
