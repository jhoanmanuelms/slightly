package biz.netcentric.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HTLProcessor extends HttpServlet
{
    private static final String DEFAULT_PATH = "/";
    private static final String INDEX_PATH = "/index.html";

    private ServletConfig servletConfig;

    public void init(ServletConfig config)
    throws ServletException
    {
        super.init(config);
        servletConfig = config;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        StringBuilder responseBuilder = new StringBuilder();
        String filePath =
            request.getPathInfo().equals(DEFAULT_PATH) ? INDEX_PATH : request.getPathInfo();

        try
        {
            ServletContext servletContext = servletConfig.getServletContext();
            File htmlFile = new File(servletContext.getResource(filePath).toURI());
            Document htmlDoc = Jsoup.parse(htmlFile, "UTF-8");

            responseBuilder.append("<pre>" + htmlDoc.html() + "</pre>");
        }
        catch (FileNotFoundException | NullPointerException exception)
        {
            responseBuilder.delete(0, responseBuilder.length());
            responseBuilder.append("The requested HTML file doesn't exist");
            exception.printStackTrace();
        }
        catch (URISyntaxException use)
        {
            responseBuilder.delete(0, responseBuilder.length());
            responseBuilder.append("The provided URL is not correctly formed");
            use.printStackTrace();
        }
        finally
        {
            printResponse(response, responseBuilder.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        processRequest(request, response);
    }

    private void printResponse(HttpServletResponse response, String text)
    throws IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter())
        {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>HTL Processor</title>");
            out.println("</head>");
            out.println("<body>");
            out.println(text);
            out.println("</body>");
            out.println("</html>");
        }
    }
}
