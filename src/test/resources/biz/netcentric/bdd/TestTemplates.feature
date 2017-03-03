# Automated test cases for Slightly
# @author Jhoan Muñoz

Feature: Slightly HTML templates processing
  Scenario: Base case for Person 1 "Kerstin". Load the provided index template using the id 1.
    When I load the template "/index.html?id=1"
    Then I expect to find the element with XPath "//h1[@title='Kerstin' and text()='Kerstin']"
      And I don't expect to find the element with XPath "//h2"
      And I expect the element with XPath "//div" to contain the text "Child: Child 0"

  Scenario: Base case for Person 2 "Erik". Load the provided index template using the id 2.
    When I load the template "/index.html?id=2"
    Then I expect to find the element with XPath "//h1[@title='Erik' and text()='Erik']"
      And I expect to find the element with XPath "//h2[@title='Dora' and text()='Spouse: Dora']"
      And I expect the element with XPath "//div[1]" to contain the text "Child: Child 0"
      And I expect the element with XPath "//div[2]" to contain the text "Child: Child 1"
      And I expect the element with XPath "//div[3]" to contain the text "Child: Child 2"

  Scenario: Base case for Person 3 "Svajune". Load the provided index template using the id 3.
    When I load the template "/index.html?id=3"
    Then I expect to find the element with XPath "//h1[@title='Svajune' and text()='Svajune']"
      And I expect to find the element with XPath "//h2[@title='Thomas' and text()='Spouse: Thomas']"
      And I don't expect to find the element with XPath "//div"

  Scenario: Custom form of base case for Person 1 "Kerstin". Load custom template using the id 1.
    When I load the template "/tests/test1.html?id=1"
    Then I expect to find the element with XPath "//h1[@title='Kerstin' and text()='Kerstin']"
      And I don't expect to find the element with XPath "//h2"
      And I don't expect to find the element with XPath "//div"

  Scenario: Custom form of base for Person 2 "Erik". Load custom template using the id 2.
    When I load the template "/tests/test1.html?id=2"
    Then I expect to find the element with XPath "//h1[@title='Erik' and text()='Erik']"
      And I expect to find the element with XPath "//div[@title='Dora']/h2[text()='Spouse: Dora']"
      And I expect the element with XPath "//div[@title='Dora']/div[1]" to contain the text "Child: Child 0"
      And I expect the element with XPath "//div[@title='Dora']/div[2]" to contain the text "Child: Child 1"
      And I expect the element with XPath "//div[@title='Dora']/div[3]" to contain the text "Child: Child 2"

  Scenario: Custom form of base for Person 3 "Svajune". Load custom template using the id 3.
    When I load the template "/tests/test1.html?id=3"
    Then I expect to find the element with XPath "//h1[@title='Svajune' and text()='Svajune']"
      And I expect to find the element with XPath "//div[@title='Thomas']/h2[text()='Spouse: Thomas']"
      And I don't expect to find the element with XPath "//div[@title='Thomas']/div"

  Scenario: Custom template to load books. Load a book from before 1990
    When I load the template "/tests/test2.html?name=Book1&author=JMS&year=1986"
    Then I expect to find the element with XPath "//h1[@title='Book1' and text()='Book: Book1']"
      And I expect to find the element with XPath "//h2[@title='JMS' and text()='Author: JMS']"
      And I don't expect to find the element with XPath "//div[@title]/h2"
      And I expect the element with XPath "//div[1]/h2" to contain the text "Genre: Genre0"
      And I expect the element with XPath "//div[2]/h2" to contain the text "Genre: Genre1"
      And I expect the element with XPath "//div[3]/h2" to contain the text "Genre: Genre2"

  Scenario: Custom template to load books. Load a book from after 1990
    When I load the template "/tests/test2.html?name=Book2&author=JMS&year=2002"
    Then I expect to find the element with XPath "//h1[@title='Book2' and text()='Book: Book2']"
      And I expect to find the element with XPath "//h2[@title='JMS' and text()='Author: JMS']"
      And I expect the element with XPath "//div[@title='2002']/h2" to contain the text "Year: 2002"
      And I expect the element with XPath "//div[2]/h2" to contain the text "Genre: Genre0"
      And I expect the element with XPath "//div[3]/h2" to contain the text "Genre: Genre1"
      And I expect the element with XPath "//div[4]/h2" to contain the text "Genre: Genre2"
