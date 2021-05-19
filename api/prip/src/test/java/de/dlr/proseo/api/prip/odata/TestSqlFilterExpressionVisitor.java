/**
 * TestSqlFilterExpressionVisitor.java
 */
package de.dlr.proseo.api.prip.odata;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.CustomQueryOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author thomas
 *
 */
public class TestSqlFilterExpressionVisitor {
	
	private static final String URI_PROTOCOL = "https:";
	private static final String URI_BASE = "//localhost/proseo/prip/odata/v1";
	private static final String URI_ODATA = "/Products";
	private static final String URI_QUERY = "$skip=100&$top=50&$filter=startswith(Name,'S3B_DO_0')";
	
	private static ODataRequest testRequest = null;
	private static ODataHttpHandler handler = null;
	private static ProductEdmProvider edmProvider = new ProductEdmProvider();
	private static TestEntityCollectionProcessor entityCollectionProcessor = new TestEntityCollectionProcessor();

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(TestSqlFilterExpressionVisitor.class);
	
	public static class TestEntityCollectionProcessor implements EntityCollectionProcessor {

//		/** The cached OData factory object */
//		private OData odata;
//		/** The cached metadata of the OData service */
//		private ServiceMetadata serviceMetadata;

		@Override
		public void init(OData odata, ServiceMetadata serviceMetadata) {
			if (logger.isTraceEnabled()) logger.trace(">>> init(OData, ServiceMetadata)");
			
//			this.odata = odata;
//			this.serviceMetadata = serviceMetadata;
		}

		@Override
		public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
				throws ODataApplicationException, ODataLibraryException {
			if (logger.isTraceEnabled()) logger.trace(">>> readEntityCollection(ODataRequest {}, ODataResponse, UriInfo {}, ContentType)",
					request.getRawRequestUri(), uriInfo);
			
			// Debug uriInfo
			logger.trace("URI info: Resource parts = {}", uriInfo.getUriResourceParts());
			logger.trace("URI info: Filter option = {}", uriInfo.getFilterOption());
			logger.trace("URI info: Order-by option = {}", uriInfo.getOrderByOption());
			logger.trace("URI info: Skip option = {}", uriInfo.getSkipOption());
			logger.trace("URI info: Top option = {}", uriInfo.getTopOption());
			logger.trace("URI info: Custom query options = [");
			for (CustomQueryOption option: uriInfo.getCustomQueryOptions()) {
				logger.trace("            {}", option.getText());
			}
			logger.trace("          ]");
			logger.trace("URI info: Entity set names = {}", uriInfo.getEntitySetNames());
			logger.trace("URI info: Kind = {}", uriInfo.getKind());
			logger.trace("URI info: Fragment = {}", uriInfo.getFragment());
			logger.trace("URI info: Aliases = {}", uriInfo.getAliases());

			// Test filter option
			FilterOption filterOption = uriInfo.getFilterOption();
			logger.trace("filterOption = " + (null == filterOption ? "null" : filterOption.getText()));
			Expression filterExpression = filterOption.getExpression();
			logger.trace("filterExpression = " + filterExpression);

			String result = null;
			try {
				SqlFilterExpressionVisitor expressionVisitor = new SqlFilterExpressionVisitor();
				Object visitorResult = filterExpression.accept(expressionVisitor);
				logger.trace("accept() returns [" + visitorResult + "]");
				assertNotNull("Unexpected null result from expressionVisitor");
				if (visitorResult instanceof String) {
					result = (String) visitorResult;
				} else {
					fail("Unexpected class " + visitorResult.getClass().getName() + " for return object from expressionVisitor");
				}
			} catch (ODataApplicationException | ExpressionVisitException e) {
				logger.error("Exception thrown: ", e);
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				return;
			}		

			// Test order option
//			OrderByOption orderByOption = uriInfo.getOrderByOption();
			
			// Test skip option
//			SkipOption skipOption = uriInfo.getSkipOption();
			
			// Test topOption
//			TopOption topOption = uriInfo.getTopOption();
			
			response.setContent((new ByteArrayInputStream(result.getBytes())));
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		}
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		OData odata = OData.newInstance();
		ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList<EdmxReference>());
		handler = odata.createHandler(edm);
		handler.register(entityCollectionProcessor);
		
//		Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//		if (rootLogger instanceof ch.qos.logback.classic.Logger) {
//			((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.ALL);
//			rootLogger.trace("Log level set to ALL");
//		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		testRequest = new ODataRequest();
		testRequest.setMethod(HttpMethod.GET);
		testRequest.setProtocol(URI_PROTOCOL);
		testRequest.setRawBaseUri(URI_BASE);
		testRequest.setRawODataPath(URI_ODATA);
		testRequest.setHeader(HttpHeader.ACCEPT, "*/*");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		testRequest = null;
	}

	/**
	 * Test method for {@link de.dlr.proseo.api.prip.odata.SqlFilterExpressionVisitor#visitMember(org.apache.olingo.server.api.uri.queryoption.expression.Member)}.
	 */
	@Test
	public final void testVisitMember() {
		String uriQuery = "$filter=ContentLength gt 0";
		String sqlQuery = "ppf.file_size > 0";
		
		testRequest.setRawRequestUri(URI_PROTOCOL + URI_BASE + URI_ODATA + "?" + uriQuery);
		testRequest.setRawQueryPath(uriQuery);
		
		logger.trace("Test request: Raw request URI = {}", testRequest.getRawRequestUri());
		logger.trace("Test request: Method = {}, protocol = {}", testRequest.getMethod(), testRequest.getProtocol());
		logger.trace("Test request: Base URI = {}, OData Path = {}", testRequest.getRawBaseUri(), testRequest.getRawODataPath());
		logger.trace("Test request: Query Path = {}, service resolution URI = {}", testRequest.getRawQueryPath(), testRequest.getRawServiceResolutionUri());
		logger.trace("Test request: Headers = {}", testRequest.getAllHeaders());
		
		ODataResponse response = handler.process(testRequest);
		
		String result = null;
		try {
			result = new String(response.getContent().readAllBytes());
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			fail("Cannot read OData response due to exception " + e.getMessage());
		}
		
		assertEquals("Unexpected where clause", sqlQuery, result);
	}

	/**
	 * Test method for {@link de.dlr.proseo.api.prip.odata.SqlFilterExpressionVisitor#visitMethodCall(
	 * org.apache.olingo.server.api.uri.queryoption.expression.MethodKind, java.util.List)}.
	 */
	@Test
	public final void testVisitMethodCall() {
		String uriQuery = "$filter=startswith(Name,'S3B_DO_0')";
		String sqlQuery = "(ppf.product_file_name LIKE 'S3B_DO_0%' OR ppf.zip_file_name LIKE 'S3B_DO_0%')"; 
		
		testRequest.setRawRequestUri(URI_PROTOCOL + URI_BASE + URI_ODATA + "?" + uriQuery);
		testRequest.setRawQueryPath(uriQuery);
		
		logger.trace("Test request: Raw request URI = {}", testRequest.getRawRequestUri());
		logger.trace("Test request: Method = {}, protocol = {}", testRequest.getMethod(), testRequest.getProtocol());
		logger.trace("Test request: Base URI = {}, OData Path = {}", testRequest.getRawBaseUri(), testRequest.getRawODataPath());
		logger.trace("Test request: Query Path = {}, service resolution URI = {}", testRequest.getRawQueryPath(), testRequest.getRawServiceResolutionUri());
		logger.trace("Test request: Headers = {}", testRequest.getAllHeaders());
		
		ODataResponse response = handler.process(testRequest);
		
		String result = null;
		try {
			result = new String(response.getContent().readAllBytes());
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			fail("Cannot read OData response due to exception " + e.getMessage());
		}
		
		assertEquals("Unexpected where clause", sqlQuery, result);
	}

	/**
	 * Test method for {@link de.dlr.proseo.api.prip.odata.SqlFilterExpressionVisitor#visitMember(org.apache.olingo.server.api.uri.queryoption.expression.Member)}.
	 */
	@Test
	public final void testVisitMemberComplex() {
		String uriQuery = "$filter=ContentDate/Start ge 2020-03-18T03:49:30.000Z and ContentDate/End le 2020-03-18T04:00:35.000Z";
		String sqlQuery = "(p.sensing_start_time >= '2020-03-18 03:49:30.000000 +0000' AND p.sensing_stop_time <= '2020-03-18 04:00:35.000000 +0000')"; 
		
		testRequest.setRawRequestUri(URI_PROTOCOL + URI_BASE + URI_ODATA + "?" + uriQuery);
		testRequest.setRawQueryPath(uriQuery);
		
		logger.trace("Test request: Raw request URI = {}", testRequest.getRawRequestUri());
		logger.trace("Test request: Method = {}, protocol = {}", testRequest.getMethod(), testRequest.getProtocol());
		logger.trace("Test request: Base URI = {}, OData Path = {}", testRequest.getRawBaseUri(), testRequest.getRawODataPath());
		logger.trace("Test request: Query Path = {}, service resolution URI = {}", testRequest.getRawQueryPath(), testRequest.getRawServiceResolutionUri());
		logger.trace("Test request: Headers = {}", testRequest.getAllHeaders());
		
		ODataResponse response = handler.process(testRequest);
		
		String result = null;
		try {
			result = new String(response.getContent().readAllBytes());
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			fail("Cannot read OData response due to exception " + e.getMessage());
		}
		
		assertEquals("Unexpected where clause", sqlQuery, result);
	}

	/**
	 * Test method for {@link de.dlr.proseo.api.prip.odata.SqlFilterExpressionVisitor#visitEnum(
	 * org.apache.olingo.commons.api.edm.EdmEnumType, java.util.List)}.
	 */
	@Test
	public final void testVisitEnum() {
		String uriQuery = "$filter=PublicationDate gt 2020-04-30T12:00:00.000Z and ProductionType eq OData.CSC.ProductionType'systematic_production'";
		String sqlQuery = "(p.generation_time > '2020-04-30 12:00:00.000000 +0000' AND p.production_type = 'SYSTEMATIC')"; 
		
		testRequest.setRawRequestUri(URI_PROTOCOL + URI_BASE + URI_ODATA + "?" + uriQuery);
		testRequest.setRawQueryPath(uriQuery);
		
		logger.trace("Test request: Raw request URI = {}", testRequest.getRawRequestUri());
		logger.trace("Test request: Method = {}, protocol = {}", testRequest.getMethod(), testRequest.getProtocol());
		logger.trace("Test request: Base URI = {}, OData Path = {}", testRequest.getRawBaseUri(), testRequest.getRawODataPath());
		logger.trace("Test request: Query Path = {}, service resolution URI = {}", testRequest.getRawQueryPath(), testRequest.getRawServiceResolutionUri());
		logger.trace("Test request: Headers = {}", testRequest.getAllHeaders());
		
		ODataResponse response = handler.process(testRequest);
		
		String result = null;
		try {
			result = new String(response.getContent().readAllBytes());
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			fail("Cannot read OData response due to exception " + e.getMessage());
		}
		
		assertEquals("Unexpected where clause", sqlQuery, result);
	}

	/**
	 * Test method for {@link de.dlr.proseo.api.prip.odata.SqlFilterExpressionVisitor#visitMember(org.apache.olingo.server.api.uri.queryoption.expression.Member)}.
	 */
	@Test
	public final void testVisitAttribute() {
		String uriQuery = "$filter=Attributes/OData.CSC.StringAttribute/any(att:att/Name eq 'productType' and att/OData.CSC.StringAttribute/Value eq 'MSI_L1C_TL')";
		String sqlQuery = "(pp1.parameter_name = 'productType' AND pp1.parameter_value = 'MSI_L1C_TL')"; 
		
		testRequest.setRawRequestUri(URI_PROTOCOL + URI_BASE + URI_ODATA + "?" + uriQuery);
		testRequest.setRawQueryPath(uriQuery);
		
		logger.trace("Test request: Raw request URI = {}", testRequest.getRawRequestUri());
		logger.trace("Test request: Method = {}, protocol = {}", testRequest.getMethod(), testRequest.getProtocol());
		logger.trace("Test request: Base URI = {}, OData Path = {}", testRequest.getRawBaseUri(), testRequest.getRawODataPath());
		logger.trace("Test request: Query Path = {}, service resolution URI = {}", testRequest.getRawQueryPath(), testRequest.getRawServiceResolutionUri());
		logger.trace("Test request: Headers = {}", testRequest.getAllHeaders());
		
		ODataResponse response = handler.process(testRequest);
		
		String result = null;
		try {
			result = new String(response.getContent().readAllBytes());
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			fail("Cannot read OData response due to exception " + e.getMessage());
		}
		
		assertEquals("Unexpected where clause", sqlQuery, result);
	}

}
