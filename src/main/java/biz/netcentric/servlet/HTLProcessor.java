package biz.netcentric.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;

/**
 * Servlet that receives requests for HTML documents. These documents are expected to be templates
 * that may contain three types of expressions:
 * <ul>
 *     <li><b>data-if expressions:</b> Elements that are displayed based on a boolean value. These
 *     kind of elements are evaluated by the method
 *     {@link #evaluateIfExpressions(Document, Context, ScriptableObject)}.</li>
 *     <li><b>data-for-x expressions:</b> Elements that iterate over the specified list and render
 *     as many elements as the list has. These kind of elements are evaluated by the method
 *     {@link #evaluateForExpressions(Document, Context, ScriptableObject)}</li>
 *     <li><b>$-expressions:</b></li> Elements contained inside the ${ } characters which content is
 *     evaluated by the javascript engine. These kind of elements are evaluated by the method
 *     {@link #evaluateExpressions(Document, Context, ScriptableObject)}
 * </ul>
 *
 * The servlet is also capable of evaluate any Javascript code embedded inside a {@code <script>}
 * tag with the {@code type} attribute set to {@code "server/javascript"}.
 */
public class HTLProcessor extends HttpServlet
{
    /** Charset used to load the HTML document. */
    static final String CHARSET_NAME = "UTF-8";

    /** Default path received by the servlet when no file is specified. */
    static final String DEFAULT_PATH = "/";

    /** Prefix of the attribute used for data-for-x expressions. */
    static final String DATA_FOR_ATTR_PREFIX = "data-for";

    /** Index of the "x" variable in the data-for-x expressions. */
    static final int DATA_FOR_X_IDX = 2;

    /** Name of the attribute used for data-if expressions. */
    static final String DATA_IF_ATTR_NAME = "data-if";

    /** Prefix used for $-expressions. */
    static final String EXPR_PREFIX = "${";

    /** Suffix used for $-expressions. */
    static final String EXPR_SUFFIX = "}";

    /** Path for the index.html file. */
    static final String INDEX_PATH = "/index.html";

    /**
     * Name of the attribute expected to be used in the {@code <script>} tag containing the
     * Javascript code to be executed by the servlet
     */
    static final String JS_ATTR_NAME = "type";

    /**
     * Value for the attribute expected to be used in the {@code <script>} tag containing the
     * Javascript code to be executed by the servlet.
     */
    static final String JS_ATTR_VAL = "server/javascript";

    /** Reference name used to execute Javascript code in the engine. */
    static final String JS_SOURCE_NAME = "<code>";

    /** Key used to store the request object in the Javascript execution scope. */
    static final String REQUEST_OBJ_KEY = "request";

    /** Content type used for the Servlet response. */
    static final String RESPONSE_CONTENT_TYPE = "text/html;charset=UTF-8";

    /** {@link javax.servlet.ServletConfig} instance used to get the servlet context. */
    private ServletConfig servletConfig;

    /**
     * Stores the given configuration object in the class variable so it can be used later.
     *
     * @param config Servlet configuration object
     * @throws ServletException If there's any problem while initiating the servlet.
     */
    @Override
    public void init(ServletConfig config)
    throws ServletException
    {
        super.init(config);
        servletConfig = config;
    }

    /**
     * Processes the given request by performing the following steps:
     * <ol>
     *     <li>Load the requested HTML document based on the request path</li>
     *     <li>Evaluate Javascript code in the specified HTML document. The servlet will only
     *     evaluate code inside a {@code <script>} tag with the {@code type} attribute set to
     *     {@code "server/javascript"}.</li>
     *     <li>Evaluate data-if expressions</li>
     *     <li>Evaluate data-for-x expressions</li>
     *     <li>Evaluate $-expressions</li>
     *     <li>Print out the response</li>
     * </ol>
     *
     * If there's any problem during the execution of the steps above, the exception will be caught
     * and an error message will be returned as response.
     *
     * @param request Object containing the request information
     * @param response Object containing the response information
     * @throws ServletException If there's any exception not treated during the request processing.
     * @throws IOException If there's any problem trying to load the specified HTML document.
     */
    void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        StringBuilder responseBuilder = new StringBuilder();
        String filePath =
            request.getPathInfo().equals(DEFAULT_PATH) ? INDEX_PATH : request.getPathInfo();

        try
        {
            ServletContext servletContext = servletConfig.getServletContext();
            File htmlFile = loadHTMLFile(servletContext, filePath);
            Document htmlDoc = Jsoup.parse(htmlFile, CHARSET_NAME);

            // Evaluate Javascript code
            String jsCode = htmlDoc.getElementsByAttributeValue(JS_ATTR_NAME, JS_ATTR_VAL).html();
            Context context = Context.enter();
            ScriptableObject scope = createScope(context);
            scope.put(REQUEST_OBJ_KEY, scope, request);
            evaluateJS(context, scope, jsCode);

            evaluateIfExpressions(htmlDoc, context, scope);
            evaluateForExpressions(htmlDoc, context, scope);
            responseBuilder.append(evaluateExpressions(htmlDoc, context, scope));
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

    /**
     * Evaluates the data-if expressions. It looks for all the expressions of this kind in the HTML
     * document and evaluates its content using the Javascript engine. If the evaluation result is
     * true, the element is preserved; otherwise, it's removed. In any case, the data-if attribute
     * is removed from the element.
     *
     * @param htmlDoc HTML document requested by the user
     * @param context Javascript evaluation context
     * @param scope Javascript evaluation scope
     */
    void evaluateIfExpressions(Document htmlDoc, Context context, ScriptableObject scope)
    {
        Elements dataIfElements = htmlDoc.getElementsByAttributeStarting(DATA_IF_ATTR_NAME);
        dataIfElements.stream().forEach((element) -> {
            Object result = evaluateJS(context, scope, element.attr(DATA_IF_ATTR_NAME));
            if (!Boolean.valueOf(result.toString()))
            {
                element.remove();
            }

            element.removeAttr(DATA_IF_ATTR_NAME);
        });
    }

    /**
     * Evaluates the data-for-x expressions by following these steps:
     * <ol>
     *     <li>Look for all the elements containing attributes that start with the
     *     {@link #DATA_FOR_ATTR_PREFIX} prefix</li>
     *     <li>Identify the "x" value </li>
     *     <li>Evaluate the Javascript expression corresponding to the elements to display</li>
     *     <li>Generate as many elements as the list has and replace the "x" value with the actual
     *     values from the list</li>
     *     <li>Remove the data-for-x attribute so clean HTML is returned</li>
     * </ol>
     *
     * @param htmlDoc HTML document requested by the user
     * @param context Javascript evaluation context
     * @param scope Javascript evaluation scope
     */
    void evaluateForExpressions(Document htmlDoc, Context context, ScriptableObject scope)
    {
        Elements dataIfElements = htmlDoc.getElementsByAttributeStarting(DATA_FOR_ATTR_PREFIX);
        dataIfElements.stream().forEach((element) -> {
            Optional<Attribute> forAttr =
                element.attributes().asList().stream().filter(
                    attribute -> attribute.getKey().startsWith(DATA_FOR_ATTR_PREFIX)).findFirst();
            String fullForAttr = forAttr.get().getKey();
            String forAttrWildcard = fullForAttr.split("-")[DATA_FOR_X_IDX];
            Object result = evaluateJS(context, scope, element.attr(fullForAttr));
            NativeJavaObject nativeJavaObject = (NativeJavaObject)result;
            List<String> unwrappedVal = (List<String>)nativeJavaObject.unwrap();
            element.removeAttr(fullForAttr);

            unwrappedVal.stream().forEach(forElement -> {
                String elemHTML = element.outerHtml();
                elemHTML = elemHTML.replace(EXPR_PREFIX + forAttrWildcard + EXPR_SUFFIX, forElement);
                element.before(elemHTML);
            });
            element.remove();
        });
    }

    /**
     * Evaluates the $-expressions. To evaluate this expressions, the HTML code is analyzed to find
     * the occurrences of elements with the {@code ${x}} pattern. Each ocurrence is evaluated using
     * the Javascript engine and its value is placed in the document.
     *
     * @param htmlDoc HTML document requested by the user
     * @param context Javascript evaluation context
     * @param scope Javascript evaluation scope
     *
     * @return A string representation of the HTML document with the $-expressions replaced by their
     *         actual values.
     */
    String evaluateExpressions(Document htmlDoc, Context context, ScriptableObject scope)
    {
        String html = htmlDoc.html();
        String[] expressions = StringUtils.split(html, EXPR_PREFIX);
        for (int i = 1; i < expressions.length; i++)
        {
            String expression = expressions[i].substring(0, expressions[i].indexOf(EXPR_SUFFIX));
            Object result = evaluateJS(context, scope, expression);
            Object unwrappedVal =
                result instanceof NativeJavaObject ? ((NativeJavaObject)result).unwrap() : result;
            expressions[i] =
                expressions[i].replace(expression + EXPR_SUFFIX, unwrappedVal.toString());
        }

        return StringUtils.join(expressions);
    }

    /**
     * Method executed when the servlet receives a POST request. Right now it'll always execute the
     * {@link #processRequest(HttpServletRequest, HttpServletResponse)} method.
     *
     * @param request Object containing the request information
     * @param response Object containing the response information
     * @throws ServletException If there's any exception not treated during the request processing.
     * @throws IOException If there's any problem trying to load the specified HTML document.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Method executed when the servlet receives a GET request. Right now it'll always execute the
     * {@link #processRequest(HttpServletRequest, HttpServletResponse)} method.
     *
     * @param request Object containing the request information
     * @param response Object containing the response information
     * @throws ServletException If there's any exception not treated during the request processing.
     * @throws IOException If there's any problem trying to load the specified HTML document.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Helper method that loads the file located in the given path according to the servlet context.
     *
     * @param servletContext Context to locate the file
     * @param filePath Path for the required file in the given context
     *
     * @return A {@link File} object referencing the required file.
     *
     * @throws URISyntaxException If the given file path or context lead to a wrong URI.
     * @throws MalformedURLException It the given path is not valid
     */
    File loadHTMLFile(ServletContext servletContext, String filePath)
    throws URISyntaxException, MalformedURLException
    {
        return new File(servletContext.getResource(filePath).toURI());
    }

    /**
     * Helper method that initializes an instance of {@link ImporterTopLevel} which can be used as
     * scope for the JS engine. This instance is required in order to be able to access the methods
     * to interact with Java classes from JS code like "importClass".
     *
     * @param context JS engine execution context.
     *
     * @return An instance of {@link ScriptableObject}
     */
    ScriptableObject createScope(Context context)
    {
        return new ImporterTopLevel(context);
    }

    /**
     * Executes the given JS expression, in the JS engine, using the given context and scope.
     *
     * @param context JS engine execution context. This object should come from the execution of the
     *                {@link Context#enter()} method.
     * @param scope Scope used to execute the JS code.
     * @param jsCode JS code to be executed.
     *
     * @return An object containing the result of the execution.
     */
    Object evaluateJS(Context context, ScriptableObject scope, String jsCode)
    {
        return context.evaluateString(scope, jsCode, JS_SOURCE_NAME, 1, null);
    }

    /**
     * Prints the servlet response.
     *
     * @param response Object containing the response information
     * @param text Text to be printed as response.
     *
     * @throws IOException If there's any problem trying to print the response.
     */
    private void printResponse(HttpServletResponse response, String text)
    throws IOException
    {
        response.setContentType(RESPONSE_CONTENT_TYPE);
        try (PrintWriter out = response.getWriter())
        {
            out.println(text);
        }
    }
}
