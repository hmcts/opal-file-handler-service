@Smoke
Feature: Health API

  Scenario: The health endpoint reports that the service is up
    When I request the file handler api health status
    Then the file handler service reports as up
