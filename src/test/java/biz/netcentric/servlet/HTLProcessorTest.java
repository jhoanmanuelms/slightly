package biz.netcentric.servlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptableObject;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test cases for the {@link HTLProcessor} class.
 *
 * @author Jhoan Muñoz
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jsoup.class, Context.class})
public class HTLProcessorTest
{
    private static final String TEST_DATA_FOR_ATTR_NAME = "data-for-x";
    private static final String TEST_DATA_FOR_ATTR_VAL = "x.collection";
    private static final String TEST_JS_CODE = "importClass(Packages.biz.netcentric.entity.Person)";
    private static final String TEST_RESPONSE = "Test Response";

    @Mock private ServletConfig mockServletConfig;
    @Mock private HttpServletRequest mockRequest;
    @Mock private HttpServletResponse mockResponse;
    @Mock private Document mockHTMLDoc;
    @Mock private Context mockContext;
    @Mock private PrintWriter mockPrintWriter;
    @Mock private Object mockJSEvalResult;
    @Mock private ScriptableObject mockScope;
    private HTLProcessor testInstance;

    @Before
    public void setUp()
    throws IOException, ServletException, URISyntaxException
    {
        MockitoAnnotations.initMocks(this);
        mockStatic(Jsoup.class);
        mockStatic(Context.class);
        testInstance = spy(new HTLProcessor());

        Elements mockElements = mock(Elements.class);
        File mockFile = mock(File.class);
        ServletContext mockServletContext = mock(ServletContext.class);
        when(mockServletConfig.getServletContext()).thenReturn(mockServletContext);
        when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
        when(mockElements.html()).thenReturn(TEST_JS_CODE);
        when(mockRequest.getPathInfo()).thenReturn(HTLProcessor.DEFAULT_PATH);
        doReturn(mockFile).when(testInstance).loadHTMLFile(any(ServletContext.class), anyString());
        doReturn(mockScope).when(testInstance).createScope(any(Context.class));
        when(mockHTMLDoc.getElementsByAttributeValue(
            HTLProcessor.JS_ATTR_NAME, HTLProcessor.JS_ATTR_VAL)).thenReturn(mockElements);

        testInstance.init(mockServletConfig);
        PowerMockito.when(Context.enter()).thenReturn(mockContext);
        PowerMockito.when(
            Jsoup.parse(any(File.class), eq(HTLProcessor.CHARSET_NAME))).thenReturn(mockHTMLDoc);
    }

    @Test
    public void testProcessResponse()
    throws IOException, ServletException
    {
        doNothing().when(testInstance).evaluateIfExpressions(
            any(Document.class), any(Context.class), any(ScriptableObject.class));
        doNothing().when(testInstance).evaluateForExpressions(
            any(Document.class), any(Context.class), any(ScriptableObject.class));
        doReturn(TEST_RESPONSE).when(testInstance).evaluateExpressions(
            any(Document.class), any(Context.class), any(ScriptableObject.class));
        doReturn(mockJSEvalResult).when(testInstance).evaluateJS(
            any(Context.class), any(ScriptableObject.class), anyString());

        testInstance.processRequest(mockRequest, mockResponse);
        verify(mockScope).put(HTLProcessor.REQUEST_OBJ_KEY, mockScope, mockRequest);
        verify(testInstance).evaluateJS(mockContext, mockScope, TEST_JS_CODE);
        verifyEvaluations(1);
        verifyPrintResponse(TEST_RESPONSE);
    }

    @Test
    public void testRequestedFileNotFound()
    throws IOException, ServletException, URISyntaxException
    {
        doThrow(FileNotFoundException.class).when(
            testInstance).loadHTMLFile(any(ServletContext.class), anyString());

        testInstance.processRequest(mockRequest, mockResponse);
        verify(mockScope, never()).put(HTLProcessor.REQUEST_OBJ_KEY, mockScope, mockRequest);
        verify(testInstance, never()).evaluateJS(mockContext, mockScope, TEST_JS_CODE);
        verifyEvaluations(0);
        verifyPrintResponse("The requested HTML file doesn't exist");
    }

    @Test
    public void testMalformedURIForRequestedFile()
    throws IOException, ServletException, URISyntaxException
    {
        doThrow(URISyntaxException.class).when(
            testInstance).loadHTMLFile(any(ServletContext.class), anyString());

        testInstance.processRequest(mockRequest, mockResponse);
        verify(mockScope, never()).put(HTLProcessor.REQUEST_OBJ_KEY, mockScope, mockRequest);
        verify(testInstance, never()).evaluateJS(mockContext, mockScope, TEST_JS_CODE);
        verifyEvaluations(0);
        verifyPrintResponse("The provided URL is not correctly formed");
    }

    @Test
    public void testExecuteJSCodeWithErrors()
    throws IOException, ServletException, URISyntaxException
    {
        doThrow(JavaScriptException.class).when(
            testInstance).evaluateJS(mockContext, mockScope, TEST_JS_CODE);

        testInstance.processRequest(mockRequest, mockResponse);
        verify(mockScope).put(HTLProcessor.REQUEST_OBJ_KEY, mockScope, mockRequest);
        verify(testInstance).evaluateJS(mockContext, mockScope, TEST_JS_CODE);

        verifyEvaluations(0);
        verifyPrintResponse("There's an error in the javascript code in the requested page.");
    }

    @Test
    public void testEvaluateIfExpressions()
    {
        Element mockElement1 = mock(Element.class);
        Element mockElement2 = mock(Element.class);
        Document mockDocument = mock(Document.class);
        Elements mockElements = new Elements(Arrays.asList(mockElement1, mockElement2));

        when(mockElement1.attr(HTLProcessor.DATA_IF_ATTR_NAME)).thenReturn(String.valueOf(true));
        when(mockElement2.attr(HTLProcessor.DATA_IF_ATTR_NAME)).thenReturn(String.valueOf(false));
        when(mockDocument.getElementsByAttributeStarting(
            HTLProcessor.DATA_IF_ATTR_NAME)).thenReturn(mockElements);
        doReturn(true).when(
            testInstance).evaluateJS(mockContext, mockScope, String.valueOf(true));
        doReturn(false).when(
            testInstance).evaluateJS(mockContext, mockScope, String.valueOf(false));

        testInstance.evaluateIfExpressions(mockDocument, mockContext, mockScope);

        verify(mockElement2).remove();
        verify(mockElement2).removeAttr(HTLProcessor.DATA_IF_ATTR_NAME);
        verify(mockElement1).removeAttr(HTLProcessor.DATA_IF_ATTR_NAME);
    }

    @Test
    public void testEvaluateForExpressions()
    {
        Attributes attributes = new Attributes();
        Element mockElement1 = mock(Element.class);
        Document mockDocument = mock(Document.class);
        NativeJavaObject mockResult = mock(NativeJavaObject.class);
        Elements mockElements = new Elements(Arrays.asList(mockElement1));
        List<String> testCollection = new ArrayList<>();
        testCollection.add("testElem1");
        testCollection.add("testElem2");

        attributes.put(TEST_DATA_FOR_ATTR_NAME, TEST_DATA_FOR_ATTR_VAL);
        when(mockResult.unwrap()).thenReturn(testCollection);
        when(mockElement1.attr(TEST_DATA_FOR_ATTR_NAME)).thenReturn(TEST_DATA_FOR_ATTR_VAL);
        when(mockElement1.attributes()).thenReturn(attributes);
        when(mockElement1.outerHtml()).thenReturn("Element: ${x}");
        when(mockDocument.getElementsByAttributeStarting(
            HTLProcessor.DATA_FOR_ATTR_PREFIX)).thenReturn(mockElements);
        doReturn(mockResult).when(
            testInstance).evaluateJS(mockContext, mockScope, TEST_DATA_FOR_ATTR_VAL);

        testInstance.evaluateForExpressions(mockDocument, mockContext, mockScope);

        verify(mockElement1).removeAttr(TEST_DATA_FOR_ATTR_NAME);
        verify(mockElement1, times(2)).outerHtml();
        verify(mockElement1).before("Element: testElem1");
        verify(mockElement1).before("Element: testElem2");
        verify(mockElement1).remove();
    }

    @Test
    public void testEvaluateExpressions()
    {
        Document mockDocument = mock(Document.class);
        String testExpression1 = "test.expression1";
        String testExpression2 = "test.expression2";
        String testResult1 = "Expression 1";
        String testResult2 = "Expression 2";
        String htmlTemplate =
            "<body><h1>${" + testExpression1 + "}</h1><h1>${" + testExpression2 + "}</h2></body>";
        String expectedHTML =
            "<body><h1>" + testResult1 + "</h1><h1>" + testResult2 + "</h2></body>";
        NativeJavaObject mockResult1 = mock(NativeJavaObject.class);
        NativeJavaObject mockResult2 = mock(NativeJavaObject.class);

        when(mockResult1.unwrap()).thenReturn(testResult1);
        when(mockResult2.unwrap()).thenReturn(testResult2);
        when(mockDocument.html()).thenReturn(htmlTemplate);
        doReturn(mockResult1).when(testInstance).evaluateJS(mockContext, mockScope, testExpression1);
        doReturn(mockResult2).when(testInstance).evaluateJS(mockContext, mockScope, testExpression2);

        assertEquals(
            "Not expected HTML",
            expectedHTML,
            testInstance.evaluateExpressions(mockDocument, mockContext, mockScope));
    }

    private void verifyEvaluations(int times)
    {
        verify(
            testInstance, times(times)).evaluateIfExpressions(mockHTMLDoc, mockContext, mockScope);
        verify(
            testInstance, times(times)).evaluateForExpressions(mockHTMLDoc, mockContext, mockScope);
        verify(testInstance, times(times)).evaluateExpressions(mockHTMLDoc, mockContext, mockScope);
    }

    private void verifyPrintResponse(String response)
    {
        verify(mockResponse).setContentType(HTLProcessor.RESPONSE_CONTENT_TYPE);
        verify(mockPrintWriter).println(startsWith(response));
    }
}
