package biz.netcentric.bdd;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.After;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Step definitions for Slightly automated testing.
 *
 * @author Jhoan Muñoz
 */
public class SlightlyStepDef
{
    /** Default URL where the application is loaded. */
    private static final String HOST_URL = "http://localhost:8080";

    /** Web Driver instance use to interact with the browser. */
    protected WebDriver webDriver;

    /**
     * Finishes the Web Driver process.
     */
    @After
    public void tearDown()
    {
        getWebDriver().quit();
        webDriver = null;
    }

    /**
     * Step definition to load the HTML template located in the given path.
     *
     * @param templatePath Path where HTML template is located.
     */
    @When("^I load the template \"([^\"]*)\"$")
    public void givenIOpenTemplate(String templatePath)
    {
        getWebDriver().get(HOST_URL + templatePath);
    }

    /**
     * Step definition to verify that an element is rendered by using the given XPath. If the
     * element is not found, the step will fail.
     *
     * @param xPath XPath used to try to locate the element.
     */
    @Then("I expect to find the element with XPath \"([^\"]*)\"$")
    public void thenIExpectToFind(String xPath)
    {
        assertNotNull(getWebDriver().findElement(By.xpath(xPath)));
    }

    /**
     * Step definition to verify that an element is NOT rendered by using the given XPath. If the
     * element is found the step will fail.
     *
     * @param xPath XPath used to try to locate the element.
     */
    @Then("I don't expect to find the element with XPath \"([^\"]*)\"$")
    public void thenIDontExpectToFind(String xPath)
    {
        try
        {
            getWebDriver().findElement(By.xpath(xPath));
            fail();
        }
        catch(NoSuchElementException nsee)
        {
            // Do nothing since this is what is required to check
        }
    }

    /**
     * Step definition to verify that the element with the given XPath contains the given text. If
     * the element can't be found or it doesn't contain the given text, the step will fail.
     *
     * @param xPath  XPath used to try to locate the element.
     * @param text Text expected to be found in the element.
     */
    @Then("I expect the element with XPath \"([^\"]*)\" to contain the text \"([^\"]*)\"$")
    public void thenIExpectElementToContainText(String xPath, String text)
    {
        String elementText = getWebDriver().findElement(By.xpath(xPath)).getText();
        assertTrue(elementText.contains(text));
    }

    /**
     * Helper method to initialized the WebDriver.
     *
     * @return An instance of {@link FirefoxDriver}.
     */
    private WebDriver getWebDriver()
    {
        if (webDriver == null)
        {
            webDriver = new FirefoxDriver();
        }

        return  webDriver;
    }
}
