# Slightly
Slightly is a Java Servlet that process request for HTML templates which are expected to contain
expressions that can be later evaluated and rendered as valid HTML.
<br />
This project is developed as a coding exercise to apply for the role of Software Engineer at
NetCentric.

###IMPLEMENTATION
In order to solve the problem proposed in the coding exercise, I implemented a Java Servlet that's
capable of process the requested HTML template and evaluate the expressions expected to be found
inside it. On a high level, these are the steps followed to process the template: <br />
1. Load the requested HTML document based on the request path.<br />
2. Evaluate Javascript code in the specified HTML document. The servlet will only evaluate code
contained inside a "script" tag with the "type" attribute set to "server/javascript".<br />
3. Evaluate data-if expressions using the Javascript engine<br />
4. Evaluate data-for-x expressions using the Javascript engine<br />
5. Evaluate $-expressions<br />
6. Print out the response

If there's any error in between any of these steps, the servlet will handle it and print out the
problem as response. For more details about the implementation, please take a look to the
documentation of the [HTLProcessor](src/main/java/biz/netcentric/servlet/HTLProcessor.java) class.

###USING THE APPLICATION
1. Clone this repository (or download the zip artifact)
2. Open a command line and move to the Slightly folder
3. Execute the command _mvn jetty:run_
4. The application should be available at [http://localhost:8080](http://localhost:8080) <br />
**Note**: Before start using the application, you'll need to have
[Apache Maven](https://maven.apache.org/) properly installed and configured in your environment.

### TESTING
Quality Assurance is an important part on any software development process, this is why I decided to
make it part of this project. The QA in this project is verified with three mechanisms:
#### Manual Testing
In order to test the application behavior, I designed two additional templates besides the base case
provided with the problem description.
* [Base Case](src/main/webapp/index.html)
<br/>
The base case template is located at _src/main/webapp/index.html_. This template loads the Person
object corresponding to the given id. It displays the name of the person, the name of the person's
spouse (only if the person is married) and a list of the person children.
<br/>
**Expected Path:** http://localhost:8080/index.html?id=_personId_
<br />
Where: _personId_ is an integer identifying the requested person
 
* [Custom Person Case](src/main/webapp/tests/test1.html)
<br/>
The custom template to load a Person is located at _src/main/webapp/tests/test1.html_. This template
loads the Person object corresponding to the given id. It displays the name of the person, the name
of the person's spouse and the list of the person's children. The main difference between this
template and the previous one is that this one ONLY displays the spouse and list of children IF the
person is married. Otherwise, only the person's name will be displayed (data-for-x inside data-if).
<br />
**Expected Path:** http://localhost:8080/tests/test1.html?id=_personId_
<br />
Where: _personId_ is an integer identifying the requested person

* [Books Template](src/main/webapp/tests/test2.html)
<br />
The template to load Books is located at _src/main/webapp/tests/test2.html_. This template creates
a Book object using the data provided by the user. It displays the book's title, author and
associated genres. The book's publication year is ONLY displayed IF the book was published after 1990.
<br />
**Expected Path:** http://localhost:8080/tests/test2.html?name=_name_&&author=_author_&&year=_year_
<br />
Where: _name_ is the book's name, _author_ is the book's author and _year_ is the book's publication
year. The genres _Genre0, Genre1, Genre2_ will be associated by default. 

#### Unit Tests
The unit test are a very good (and broadly used) tool to ensure the code is working as expected and
to make sure that further changes won't break its expected functionality.

* In this project, the test cases are located at [src/test/java](src/test/java) directory.
* In order to execute them you can use the command: _mvn clean test_

<br />
The unit tests were written using JUnit, Mockito and PowerMock.

#### Automated Tests
Finally, automated tests are very useful to ensure the proper behavior of the application. For this
project I wrote several test scenarios using the templates described in the **Manual Testing**
section.
* Test Scenarios: [src/test/resources/biz/netcentric/bdd/TestTemplates.feature](src/test/resources/biz/netcentric/bdd/TestTemplates.feature)
* Step Definitions: [src/test/java/biz/netcentric/bdd/SlightlyStepDef.java](src/test/java/biz/netcentric/bdd/SlightlyStepDef.java)


##### Automated Tests Execution
In order to execute the automated tests cases you need to follow these steps:<br />
1. Execute the command _mvn jetty:run_ in order to start the application<br />
2. In a separate console, execute the command: _mvn test -Dtest=biz.netcentric.bdd.BDDRunner_

The automated test cases were created using Cucumber and Selenium. 

### DEVELOPMENT TOOLS
For the development of this project, I used the following tools and libraries:

|TOOL|VERSION|USAGE|
|----|-------:|-----|
|Maven|3.3.9|Dependency Management System|
|Jetty|9.4.0|Servlet Engine|
|JSoup|1.10.2|DOM Management Library|
|Rhino|1.7.7.1|JavaScript Engine|
|Apache Common-lang|3.5|Utilities Library|
|JUnit|4.12|Java Unit Tests|
|Mockito|1.10.19|Mock objects for testing|
|PowerMock|1.6.6|Mocks for static methods|
|Selenium|2.53.1|Execution of automated tests|
|Cucumber|1.2.5|Step definitions for the automated tests|
|Firefox|30.0|Execution of automated tests|
|IntelliJ IDEA|2016.3.4|IDE|
|Git|2.9.0|Version Control System|
|Source Tree|1.9.10.0|UI for Git|
|GitHub| |Source Repository|
