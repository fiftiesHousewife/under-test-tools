Feature: Refunds

  Scenario: A full refund returns the whole amount
    Given an order of 1000 paid in full
    When the customer asks for a full refund
    Then 1000 is refunded

  @ignore
  Scenario: A refund after 30 days is refused
    Given an order paid 31 days ago
    When the customer asks for a full refund
    Then the refund is refused

  Scenario Outline: A part refund returns the lines asked for
    Given an order with lines of <first> and <second>
    When the customer asks for a refund of the first line
    Then <refunded> is refunded

    Examples:
      | first | second | refunded |
      | 500   | 500    | 500      |
      | 250   | 750    | 250      |
      | 999   | 1      | 999      |
