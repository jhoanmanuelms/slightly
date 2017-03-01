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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;

public class HTLProcessor extends HttpServlet
{
    private static final String CHARSET_NAME = "UTF-8";
    private static final String DEFAULT_PATH = "/";
    private static final String INDEX_PATH = "/index.html";
    private static final String JS_ATTR_VAL = "server/javascript";
    private static final String JS_SOURCE_NAME = "<code>";
    private static final String REQUEST_OBJ_KEY = "request";
    private static final String TYPE_ATTR_KEY = "type";

    private ServletConfig servletConfig;

    @Override
    public void init(ServletConfig config)
    throws ServletException
    {
        super.init(config);
        servletConfig = config;
    }

    void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        StringBuilder responseBuilder = new StringBuilder();
        String filePath =
            request.getPathInfo().equals(DEFAULT_PATH) ? INDEX_PATH : request.getPathInfo();

        try
        {
            ServletContext servletContext = servletConfig.getServletContext();
            File htmlFile = new File(servletContext.getResource(filePath).toURI());
            Document htmlDoc = Jsoup.parse(htmlFile, CHARSET_NAME);

            String jsCode = htmlDoc.getElementsByAttributeValue(TYPE_ATTR_KEY, JS_ATTR_VAL).html();
            Context context = Context.enter();
            ScriptableObject scope = new ImporterTopLevel(context);
            scope.put(REQUEST_OBJ_KEY, scope, request);
            context.evaluateString(scope, jsCode, JS_SOURCE_NAME, 1, null);

            // TODO create a helper methods to find the special HTML tags and process them using the
            //      JS objects
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
        catch (RhinoException rhe)
        {
            responseBuilder.delete(0, responseBuilder.length());
            responseBuilder.append("There's an error in the javascript code in the requested page.");
            responseBuilder.append(rhe.getMessage());
            rhe.printStackTrace();
        }
        finally
        {
            printResponse(response, responseBuilder.toString());
            Context.exit();
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
