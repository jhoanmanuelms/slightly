package biz.netcentric.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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

public class HTLProcessor extends HttpServlet
{
    private static final String CHARSET_NAME = "UTF-8";
    private static final String DEFAULT_PATH = "/";
    private static final String DATA_FOR_ATTR_PREFIX = "data-for";
    private static final int DATA_FOR_X_IDX = 2;
    private static final String DATA_IF_ATTR_NAME = "data-if";
    private static final String EXPR_PREFIX = "${";
    private static final String EXPR_SUFFIX = "}";
    private static final String INDEX_PATH = "/index.html";
    private static final String JS_ATTR_VAL = "server/javascript";
    private static final String JS_SOURCE_NAME = "<code>";
    private static final String REQUEST_OBJ_KEY = "request";
    private static final String RESPONSE_CONTENT_TYPE = "text/html;charset=UTF-8";
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

    void evaluateIfExpressions(Document htmlDoc, Context context, ScriptableObject scope)
    {
        Elements dataIfElements = htmlDoc.getElementsByAttributeStarting(DATA_IF_ATTR_NAME);
        dataIfElements.stream().forEach((element) -> {
            Object result =
                context.evaluateString(
                    scope, element.attr(DATA_IF_ATTR_NAME), JS_SOURCE_NAME, 1, null);

            if (!Boolean.valueOf(result.toString()))
            {
                element.remove();
            }

            element.removeAttr(DATA_IF_ATTR_NAME);
        });
    }

    void evaluateForExpressions(Document htmlDoc, Context context, ScriptableObject scope)
    {
        Elements dataIfElements = htmlDoc.getElementsByAttributeStarting(DATA_FOR_ATTR_PREFIX);
        dataIfElements.stream().forEach((element) -> {
            Optional<Attribute> forAttr =
                element.attributes().asList().stream().filter(
                    attribute -> attribute.getKey().startsWith(DATA_FOR_ATTR_PREFIX)).findFirst();
            String fullForAttr = forAttr.get().getKey();
            String forAttrWildcard = fullForAttr.split("-")[DATA_FOR_X_IDX];
            Object result =
                context.evaluateString(scope, element.attr(fullForAttr), JS_SOURCE_NAME, 1, null);
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

    String evaluateExpressions(Document htmlDoc, Context context, ScriptableObject scope)
    {
        String html = htmlDoc.html();
        String[] expressions = StringUtils.split(html, EXPR_PREFIX);
        for (int i = 1; i < expressions.length; i++)
        {
            String expression = expressions[i].substring(0, expressions[i].indexOf(EXPR_SUFFIX));
            Object result = context.evaluateString(scope, expression, JS_SOURCE_NAME, 1, null);
            Object unwrappedVal =
                result instanceof NativeJavaObject ? ((NativeJavaObject)result).unwrap() : result;
            expressions[i] =
                expressions[i].replace(expression + EXPR_SUFFIX, unwrappedVal.toString());
        }

        return StringUtils.join(expressions);
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
        response.setContentType(RESPONSE_CONTENT_TYPE);
        try (PrintWriter out = response.getWriter())
        {
            out.println(text);
        }
    }
}
