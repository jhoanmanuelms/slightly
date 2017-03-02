package biz.netcentric.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test cases for the {@link Person} class.
 *
 * @author Jhoan Muñoz
 */
public class PersonTest
{
    private static final String TEST_PERSON_ID = "2";
    private static final String TEST_NOT_EXISTING_PERSON_ID = "0";

    @Test
    public void testCreatePerson()
    {
        String name = "Carlos Martinez";
        boolean married = true;
        String spouse = "Sara Fernandez";
        int numberOfChildren = 2;
        Person person = new Person(name, spouse, married, numberOfChildren);

        assertPersonInfo(person, name, spouse, married, numberOfChildren);
    }

    @Test
    public void testCreateEmptyPerson()
    {
        Person person = new Person();
        assertPersonInfo(person, "", "", false, 0);
    }

    @Test
    public void testPersonLookup()
    {
        Person person = Person.lookup(TEST_PERSON_ID);
        assertPersonInfo(person, "Erik", "Dora", true, 3);
    }

    @Test
    public void testPersonLookup_NotExistent()
    {
        Person person = Person.lookup(TEST_NOT_EXISTING_PERSON_ID);
        assertPersonInfo(person, "Empty Name", "Empty spouse", false, 0);
    }

    private void assertPersonInfo(
        Person person, String name, String spouse, boolean married, int numberOfChildren)
    {
        assertEquals("Not expected name", name, person.getName());
        assertEquals("Not expected spouse", spouse, person.getSpouse());
        assertEquals("Not expected status", married, person.isMarried());
        assertEquals(
            "Not expected number of children", numberOfChildren, person.getChildren().size());
    }
}
