@Smoke
Feature: Service root endpoint

  Scenario: Root endpoint returns the welcome message
    When I call GET "/"
    Then the response status code is 200
    And the response body contains "Welcome"
