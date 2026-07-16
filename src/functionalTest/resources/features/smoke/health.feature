@Smoke
Feature: Service health

  Scenario: Health endpoint responds successfully
    When I call the health endpoint
    Then the response status code is 200
