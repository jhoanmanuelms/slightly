package biz.netcentric.entity;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Test cases for the {@link Book} class.
 *
 * @author Jhoan Muñoz
 */
public class BookTest
{
    @Test
    public void testCreateBook()
    {
        String name = "Game Of Thrones";
        String author = "George R.R. Martin";
        int year = 1996;
        String[] genres = new String[]{"Heroic Fantasy", "Political Strategy"};
        Book book = new Book(name, author, year, genres);

        assertBookInfo(book, name, author, year, Arrays.asList(genres));
    }

    @Test
    public void testCreateEmptyBook()
    {
        Book book = new Book();
        assertBookInfo(book, "", "", 0, Collections.emptyList());
    }

    private void assertBookInfo(Book book, String name, String author, int year, List<String> genres)
    {
        assertEquals("Not expected name", name, book.getName());
        assertEquals("Not expected author", author, book.getAuthor());
        assertEquals("Not expected year", year, book.getYear());
        assertEquals("Not expected genres", genres, book.getGenres());
    }
}
