package biz.netcentric.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Java Bean for a Book entity.
 *
 * @author Jhoan Muñoz
 */
public class Book
{
    private String name;
    private String author;
    private int year;
    private List<String> genres;

    public Book()
    {
        name = "";
        author = "";
        year = 0;
        genres = Collections.emptyList();
    }

    public Book(String name, String author, int year, String... genres)
    {
        this.name = name;
        this.author = author;
        this.year = year;
        this.genres = Arrays.asList(genres);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public int getYear()
    {
        return year;
    }

    public void setYear(int year)
    {
        this.year = year;
    }

    public List<String> getGenres()
    {
        return genres;
    }

    public void setGenres(List<String> genres)
    {
        this.genres = genres;
    }
}
