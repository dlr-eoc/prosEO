/**
 * DownloadManagerTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import de.dlr.proseo.api.aipclient.AipClientApplication;
import de.dlr.proseo.api.aipclient.AipClientConfiguration;
import de.dlr.proseo.api.aipclient.AipClientTestConfiguration;
import de.dlr.proseo.api.aipclient.rest.model.RestProduct;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.ProductVisibility;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Test product downloads from remote Long-term Archives
 * <br>
 * Inspiration for REST service mocking from:
 * https://www.baeldung.com/spring-mock-rest-template#using-spring-test
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AipClientApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional
public class DownloadManagerTest {
	
	// Database test data
	private static final String TEST_MISSION_NAME = "Unit Test Mission";
	private static final String TEST_FACILITY = "testfac";
	private static final String TEST_PRODUCT_TYPE = "L0__ODB_1_";
	private static final String TEST_FILE_CLASS = "OPER";
	private static final Instant TEST_DB_START_TIME_1 = Instant.parse("2017-01-01T12:00:00Z");
	private static final Instant TEST_DB_STOP_TIME_1 = Instant.parse("2017-01-01T12:10:00Z");
	private static final Instant TEST_DB_START_TIME_2 = Instant.parse("2017-01-01T12:00:00Z");
	private static final Instant TEST_DB_STOP_TIME_2 = Instant.parse("2017-01-01T12:10:00Z");
	private static final String TEST_DB_FILENAME_1 = "S5P_OPER_L0__ODB_1__20170101T120000_20170101T121000_00180_01.RAW";
	//private static final String TEST_DB_FILENAME_2 = "S5P_OPER_L0__ODB_1__20170101T120000_20170101T121000_00180_01.RAW";
	
	// Mock LTA test data
	private static final String TEST_FILENAME_1 = "S5P_OPER_L0__ODB_1__20210101T000510_20210101T002508_16680_03.RAW";
	private static final String TEST_FILENAME_2 = "S5P_OPER_L0__ODB_1__20210101T001510_20210101T003508_16680_04.RAW";
	private static final String TEST_START_TIME = "2021-01-01T00:05:10.123";
	private static final String TEST_STOP_TIME = "2021-01-01T00:25:08.234";
	private static final String TEST_UUID_1 = "3cdc5b00-0050-4c21-925e-4806b59db629";
	private static final String TEST_UUID_2 = "3cdc5b00-0050-4c21-925e-4806b59db73a";
	
	private static final String URL_LTA1 = "/lta1/odata/v1/";
	private static final String URL_LTA2 = "/lta2/odata/v1/";
	
	private static final String LTA_QUERY_BY_NAME = 
			"Products?$filter=Name%20eq%20" 
				+ TEST_FILENAME_1
				+ "&$top=1&$expand=Attributes"; 
	private static final String LTA_QUERY_BY_TIME = 
			"Products?$filter=ContentDate/Start%20eq%20" 
				+ TEST_START_TIME 
				+ "Z%20and%20ContentDate/End%20eq%20" 
				+ TEST_STOP_TIME 
				+ "Z%20and%20Attributes/OData.CSC.StringAttribute/any(att:att/Name%20eq%20'productType'%20" 
				+ "and%20att/OData.CSC.StringAttribute/Value%20eq%20'" 
				+ TEST_PRODUCT_TYPE 
				+ "')&$top=1&$expand=Attributes";
	private static final String LTA_QUERY_ALL_BY_TIME = 
			"Products?$filter=ContentDate/Start%20le%20" 
				+ TEST_STOP_TIME 
				+ "Z%20and%20ContentDate/End%20ge%20" 
				+ TEST_START_TIME 
				+ "Z%20and%20Attributes/OData.CSC.StringAttribute/any(att:att/Name%20eq%20'productType'%20" 
				+ "and%20att/OData.CSC.StringAttribute/Value%20eq%20'" 
				+ TEST_PRODUCT_TYPE 
				+ "')&$top=1000&$expand=Attributes";
	
	private static final String LTA_PRODUCT_1 = 
			"{\"Id\":\"" +  TEST_UUID_1 + "\","
			+ "\"Name\":\"" + TEST_FILENAME_1 + "\"," 
			+ "\"ContentType\":\"application/octet-stream\"," 
			+ "\"ContentLength\":119955025," 
			+ "\"OriginDate\":\"1970-01-01T00:00:00Z\"," 
			+ "\"PublicationDate\":\"2022-06-26T14:53:11.138Z\"," 
			+ "\"EvictionDate\":\"9999-12-31T23:59:59.999Z\"," 
			+ "\"Footprint\":null," 
			+ "\"Checksum\":[{"
				+ "\"Algorithm\":\"MD5\"," 
				+ "\"Value\":\"532DADE3B0D34C77ED8EA3909A99EB39\"," 
				+ "\"ChecksumDate\":\"2022-02-19T18:10:50Z\"}]," 
			+ "\"ContentDate\":{" 
				+ "\"Start\":\"" + TEST_START_TIME + "Z\"," 
				+ "\"End\":\"" + TEST_STOP_TIME + "Z\"}," 
			+ "\"ProductionType\":\"systematic_production\"," 
			+ "\"Attributes\":[{"
				+ "\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\","
				+ "\"Name\":\"beginningDateTime\","
				+ "\"ValueType\":\"DateTimeOffset\","
				+ "\"Value\":\"" + TEST_START_TIME + "Z\"},"
				+ "{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\","
				+ "\"Name\":\"endingDateTime\",\"ValueType\":\"DateTimeOffset\","
				+ "\"Value\":\"" + TEST_STOP_TIME + "Z\"},"
				+ "{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\","
				+ "\"Name\":\"processingDate\","
				+ "\"ValueType\":\"DateTimeOffset\",\"Value\":\"2021-01-01T00:25:08Z\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"platformShortName\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"SENTINEL-5P\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processorName\","
				+ "\"ValueType\":\"String\",\"Value\":\"\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processorVersion\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processingLevel\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"L0\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processingMode\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"productType\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"" + TEST_PRODUCT_TYPE + "\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processingCenter\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"DLR\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"productClass\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"OPER\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"slice\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"03\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"orbitNumber\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"16680\"}]}";

	private static final String LTA_PRODUCT_2 = 
			"{\"Id\":\"" +  TEST_UUID_2 + "\","
			+ "\"Name\":\"" + TEST_FILENAME_2 + "\"," 
			+ "\"ContentType\":\"application/octet-stream\"," 
			+ "\"ContentLength\":119955025," 
			+ "\"OriginDate\":\"1970-01-01T00:00:00Z\"," 
			+ "\"PublicationDate\":\"2022-06-26T14:53:11.138Z\"," 
			+ "\"EvictionDate\":\"9999-12-31T23:59:59.999Z\"," 
			+ "\"Footprint\":null," 
			+ "\"Checksum\":[{"
				+ "\"Algorithm\":\"MD5\"," 
				+ "\"Value\":\"532DADE3B0D34C77ED8EA3909A99EB39\"," 
				+ "\"ChecksumDate\":\"2022-02-19T18:10:50Z\"}]," 
			+ "\"ContentDate\":{" 
				+ "\"Start\":\"2021-01-01T00:15:10.345Z\"," 
				+ "\"End\":\"2021-01-01T00:35:08.456Z\"}," 
			+ "\"ProductionType\":\"systematic_production\"," 
			+ "\"Attributes\":[{"
				+ "\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\","
				+ "\"Name\":\"beginningDateTime\","
				+ "\"ValueType\":\"DateTimeOffset\","
				+ "\"Value\":\"2021-01-01T00:15:10.345Z\"},"
				+ "{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\","
				+ "\"Name\":\"endingDateTime\",\"ValueType\":\"DateTimeOffset\","
				+ "\"Value\":\"2021-01-01T00:35:08.456Z\"},"
				+ "{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\","
				+ "\"Name\":\"processingDate\","
				+ "\"ValueType\":\"DateTimeOffset\",\"Value\":\"2021-01-01T00:35:08Z\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"platformShortName\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"SENTINEL-5P\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processorName\","
				+ "\"ValueType\":\"String\",\"Value\":\"\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processorVersion\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processingLevel\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"L0\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processingMode\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"productType\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"" + TEST_PRODUCT_TYPE + "\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"processingCenter\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"DLR\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"productClass\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"OPER\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"slice\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"04\"},"
				+ "{\"@odata.type\":\"#OData.CSC.StringAttribute\","
				+ "\"Name\":\"orbitNumber\","
				+ "\"ValueType\":\"String\","
				+ "\"Value\":\"16680\"}]}";
			
	private static final String LTA_RESPONSE_BY_NAME_1 = 
			"{\"@odata.context\":\"$metadata#Products(Attributes())\",\"@odata.count\":1,\"value\":[" 
				+ LTA_PRODUCT_1
				+ "]}";
	
	private static final String LTA_RESPONSE_BY_TIME = LTA_RESPONSE_BY_NAME_1;
	
	private static final String LTA_RESPONSE_ALL_BY_TIME = 
			"{\"@odata.context\":\"$metadata#Products(Attributes())\",\"@odata.count\":2,\"value\":[" 
				+ LTA_PRODUCT_1
				+ ","
				+ LTA_PRODUCT_2
			+ "]}";
		
	private static final String LTA_DOWNLOAD_BY_UUID_1 = 
			"Products(" + TEST_UUID_1 + ")/$value"; 
	private static final String LTA_DOWNLOAD_BY_UUID_2 = 
			"Products(" + TEST_UUID_2 + ")/$value"; 

	private static final String TEST_PRODUCT_DATA_1 = "testdata1 for UUID " + TEST_UUID_1;
	private static final String TEST_PRODUCT_DATA_2 = "testdata2 for UUID " + TEST_UUID_2;
	
	/** AIP Client configuration */
	@Autowired
	private AipClientConfiguration config;
	
	/** Test configuration */
	@Autowired
	private AipClientTestConfiguration testConfig;

	/** Download Manager */
	@Autowired
	private DownloadManager downloadManager;

	/** Database access via JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	/** The REST Template object to intercept */
    @Autowired
    private RestTemplate restTemplate;

    /** A mock LTA service */
	private static int WIREMOCK_PORT = 9876;
	private static WireMockServer mockLtaService;
    
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DownloadManagerTest.class);
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> setUpBeforeClass()");
		
		// Validate test data
		ObjectMapper om = new ObjectMapper();
		boolean failed = false;
		try {
			om.readTree(LTA_RESPONSE_BY_NAME_1);
		} catch (Exception e) {
			logger.error("LTA response by name not parseable (cause: {} / {})", e.getClass(), e.getMessage());
			failed = true;
		}
		try {
			om.readTree(LTA_RESPONSE_BY_TIME);
		} catch (Exception e) {
			logger.error("LTA response by time not parseable (cause: {} / {})", e.getClass(), e.getMessage());
			failed = true;
		}
		try {
			om.readTree(LTA_RESPONSE_ALL_BY_TIME);
		} catch (Exception e) {
			logger.error("LTA response all by time not parseable (cause: {} / {})", e.getClass(), e.getMessage());
			failed = true;
		}

		if (failed) {
			throw new RuntimeException("Test data validation failed");
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> tearDownAfterClass()");

	}
	
	/**
	 * Create test processing facility and test products for internal query satisfaction in local database
	 * 
	 * @throws Exception if any error condition arises
	 */
	private void setUpDatabase() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> setUpDatabase()");
		
		// Ensure test user is set up from data.sql file
		String query = "select * from users;";
		List<?> queryResult = em.createNativeQuery(query).getResultList();
		assertTrue("No user created in database", 0 < queryResult.size());

		// Create test processing facility
		ProcessingFacility facility = new ProcessingFacility();
		facility.setName(TEST_FACILITY);
		facility = RepositoryService.getFacilityRepository().save(facility);
		
		// Create test mission and product class
		Mission mission = new Mission();
		mission.setCode(testConfig.getTestMission());
		mission.setName(TEST_MISSION_NAME);
		mission.getFileClasses().add(TEST_FILE_CLASS);
		mission.setProductFileTemplate("N/A");
		mission = RepositoryService.getMissionRepository().save(mission);
		
		ProductClass productClass = new ProductClass();
		productClass.setProductType(TEST_PRODUCT_TYPE);
		productClass.setMission(mission);
		productClass.setVisibility(ProductVisibility.PUBLIC);
		productClass = RepositoryService.getProductClassRepository().save(productClass);
		
		// Create test products (outside sensing time range for mock LTA)
		Product product1 = new Product();
		product1.setProductClass(productClass);
		product1.setFileClass(TEST_FILE_CLASS);
		product1.setSensingStartTime(TEST_DB_START_TIME_1);
		product1.setSensingStopTime(TEST_DB_STOP_TIME_1);
		product1.setGenerationTime(TEST_DB_STOP_TIME_1);
		product1.setUuid(UUID.randomUUID());
		product1 = RepositoryService.getProductRepository().save(product1);
		
		Product product2 = new Product();
		product2.setProductClass(productClass);
		product2.setFileClass(TEST_FILE_CLASS);
		product2.setSensingStartTime(TEST_DB_START_TIME_2);
		product2.setSensingStopTime(TEST_DB_STOP_TIME_2);
		product2.setGenerationTime(TEST_DB_STOP_TIME_2);
		product2.setUuid(UUID.randomUUID());
		product2 = RepositoryService.getProductRepository().save(product2);
		
		// Only the first product gets a product file, so the second one must never be returned
		ProductFile productFile1 = new ProductFile();
		productFile1.setProduct(product1);
		productFile1.setProcessingFacility(facility);
		productFile1.setProductFileName(TEST_DB_FILENAME_1);
		productFile1.setStorageType(StorageType.POSIX);
		productFile1.setFilePath("/");
		productFile1.setFileSize(123L);
		productFile1 = RepositoryService.getProductFileRepository().save(productFile1);
		
		product1.getProductFile().add(productFile1);
		product1 = RepositoryService.getProductRepository().save(product1);
		
		// Create test LTAs
		// TODO Requires new entity "ProductArchive"
	}

	/**
	 * Set up query and download stubs for mock LTA service
	 * 
	 * @throws URISyntaxException if an invalid URI was used
	 */
	private void setUpMockLtaService() throws URISyntaxException {
		if (logger.isTraceEnabled()) logger.trace(">>> setUpMockLtaService()");
		
		mockLtaService = new WireMockServer(WIREMOCK_PORT);
		mockLtaService.start();
		
		mockLtaService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_LTA1 + LTA_QUERY_BY_NAME))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
						.withBody(LTA_RESPONSE_BY_NAME_1)));
		
		mockLtaService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_LTA2 + LTA_QUERY_BY_TIME))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
						.withBody(LTA_RESPONSE_BY_TIME)));
		
		mockLtaService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_LTA2 + LTA_QUERY_ALL_BY_TIME))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
						.withBody(LTA_RESPONSE_ALL_BY_TIME)));
		
		mockLtaService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_LTA1 + LTA_DOWNLOAD_BY_UUID_1))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString())
						.withBody(TEST_PRODUCT_DATA_1)));
		
		mockLtaService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_LTA2 + LTA_DOWNLOAD_BY_UUID_1))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString())
						.withBody(TEST_PRODUCT_DATA_1)));
		
		mockLtaService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_LTA2 + LTA_DOWNLOAD_BY_UUID_2))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString())
						.withBody(TEST_PRODUCT_DATA_2)));
		
	}

	/**
	 * Set up test data and mock LTA service on first run
	 * 
	 * @throws java.lang.Exception if any error condition arises
	 */
	@Before
	public void setUp() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> setUp()");
		
		// Create test processing facility and test products in local database
		setUpDatabase();
		
		// Set up mock LTA for external product query
		setUpMockLtaService();
		
		// Ensure files to download are not present
		Files.deleteIfExists(Path.of(config.getIngestorSourceDir(), TEST_FILENAME_1));
		Files.deleteIfExists(Path.of(config.getIngestorSourceDir(), TEST_FILENAME_2));
		
		// Mock authentication
//		Authentication auth = new TestingAuthenticationToken(config.getAuxipUser(), config.getAuxipPassword());
//		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	/**
	 * Remove downloaded files
	 * 
	 * @throws java.lang.Exception if any error condition arises
	 */
	@After
	public void tearDown() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> tearDown()");
		
		// Stop mock LTA service
		mockLtaService.stop();
		
		// Remove downloaded files
		Files.deleteIfExists(Path.of(config.getIngestorSourceDir(), TEST_FILENAME_1));
		Files.deleteIfExists(Path.of(config.getIngestorSourceDir(), TEST_FILENAME_2));

	}

	/**
	 * Test method for {@link de.dlr.proseo.api.aipclient.rest.DownloadManager#downloadByName(java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testDownloadByName() {
		if (logger.isTraceEnabled()) logger.trace(">>> testDownloadByName()");

		// Correct test data
		String filename = TEST_FILENAME_1;
		String facility = TEST_FACILITY;
		
		
		// Test invalid facility name: Not UTF-8-compliant
		byte[] nonUtf8String = { 0x0A, 0x0B, 0x0C, 0x00, 0x03, 0x05 };
		facility = new String(nonUtf8String);
		
		try {
			downloadManager.downloadByName(filename, facility);
			fail("Non-UTF-8 facility name not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-UTF-8 facility name");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-UTF-8 facility name: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		// Test invalid facility name: Not existing
		facility = "non-existent facility";
		
		try {
			downloadManager.downloadByName(filename, facility);
			fail("Non-existent facility name not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-existent facility name");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existent facility name: " + e.getClass().getName() + "/" + e.getMessage());
		}

		facility = TEST_FACILITY;

		
		// Test non-existing filename
		filename = "non-existent-filename";
		
		try {
			downloadManager.downloadByName(filename, facility);
			fail("Non-existent filename not detected");
		} catch (NoResultException e) {
			logger.info("NoResultException received as expected for non-existent filename");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existent filename: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		filename = TEST_FILENAME_1;
	
		
		// Test successful download
		try {
			RestProduct restProduct = downloadManager.downloadByName(filename, facility);
			
			assertEquals("Unexpected filename", filename, restProduct.getProductFile().get(0).getProductFileName());
			
			// Wait for download completion and check existence of file
			Thread.sleep(2000L);
			assertTrue("File " + TEST_FILENAME_1 + " not present", Files.exists(Path.of(config.getIngestorSourceDir(), TEST_FILENAME_1)));
			
		} catch (Exception e) {
			fail("Unexpected exception when testing successful product download: " + e.getClass().getName() + "/" + e.getMessage());
		}
	}

	/**
	 * Test method for {@link de.dlr.proseo.api.aipclient.rest.DownloadManager#downloadBySensingTime(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testDownloadBySensingTime() {
		if (logger.isTraceEnabled()) logger.trace(">>> testDownloadBySensingTime()");

		// Correct test data
		String productType = TEST_PRODUCT_TYPE;
		String sensingStartTime = TEST_START_TIME;
		String sensingStopTime = TEST_STOP_TIME;
		String facility = TEST_FACILITY;
		
		
		// Test invalid facility name: Not UTF-8-compliant
		byte[] nonUtf8String = { 0x0A, 0x0B, 0x0C, 0x00, 0x03, 0x05 };
		facility = new String(nonUtf8String);

		try {
			downloadManager.downloadBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Non-UTF-8 facility name not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-UTF-8 facility name");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-UTF-8 facility name: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		facility = TEST_FACILITY;
		
		
		// Test invalid facility name: Not existing
		facility = "non-existent facility";
		
		try {
			downloadManager.downloadBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Non-existent facility name not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-existent facility name");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existent facility name: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		facility = TEST_FACILITY;
		
		
		// --- Test REST service stub ---
		try {
			RestProduct product = downloadManager.downloadBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			assertEquals("Unexpected product class when testing REST service stub:", TEST_PRODUCT_TYPE, product.getProductClass());
		} catch (Exception e) {
			fail("Unexpected exception when testing REST service stub: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		
		// Test invalid sensing start time
		sensingStartTime = "illegal-start-time";
		
		try {
			downloadManager.downloadBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Invalid sensing start time not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for invalid sensing start time");
		} catch (Exception e) {
			fail("Unexpected exception when testing invalid sensing start time: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		sensingStartTime = TEST_START_TIME;

		
		// Test invalid sensing stop time
		sensingStopTime = "illegal-stop-time";
		
		try {
			downloadManager.downloadBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Invalid sensing stop time not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for invalid sensing stop time");
		} catch (Exception e) {
			fail("Unexpected exception when testing invalid sensing stop time: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		sensingStopTime = TEST_STOP_TIME;
		
		
		// Test non-existing product type
		productType = "nonexistent-product-type";
		
		try {
			downloadManager.downloadBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Non-existent product type not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-existent product type");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existent product type: " + e.getClass().getName() + "/" + e.getMessage());
		}
		productType = TEST_PRODUCT_TYPE;
		
		
		// Test non-existing time range
		sensingStartTime = "2000-01-01T00:00:00.000000";
		
		try {
			downloadManager.downloadBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Non-existing time range not detected");
		} catch (NoResultException e) {
			logger.info("NoResultException received as expected for non-existing time range");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existing time range: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		sensingStartTime = TEST_START_TIME;

		
		// Test successful product download
		try {
			RestProduct restProduct = downloadManager.downloadBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			
			assertEquals("Unexpected product type", productType, restProduct.getProductClass());
			assertEquals("Unexpected start time", sensingStartTime, restProduct.getSensingStartTime());
			assertEquals("Unexpected stop time", sensingStopTime, restProduct.getSensingStopTime());
			
			// Wait for download completion and check existence of file
			Thread.sleep(2000L);
			assertTrue("File " + TEST_FILENAME_1 + " not present", Files.exists(Path.of(config.getIngestorSourceDir(), TEST_FILENAME_1)));
			
		} catch (Exception e) {
			fail("Unexpected exception when testing successful product download: " + e.getClass().getName() + "/" + e.getMessage());
		}
	}

	/**
	 * Test method for {@link de.dlr.proseo.api.aipclient.rest.DownloadManager#downloadAllBySensingTime(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testDownloadAllBySensingTime() {
		if (logger.isTraceEnabled()) logger.trace(">>> testDownloadAllBySensingTime()");

		// Correct test data
		String productType = TEST_PRODUCT_TYPE;
		String sensingStartTime = TEST_START_TIME;
		String sensingStopTime = TEST_STOP_TIME;
		String facility = TEST_FACILITY;

		
		// Test invalid facility name: Not UTF-8-compliant
		byte[] nonUtf8String = { 0x0A, 0x0B, 0x0C, 0x00, 0x03, 0x05 };
		facility = new String(nonUtf8String);

		try {
			downloadManager.downloadAllBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Non-UTF-8 facility name not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-UTF-8 facility name");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-UTF-8 facility name: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		facility = TEST_FACILITY;
		
		
		// Test invalid facility name: Not existing
		facility = "non-existent facility";
		
		try {
			downloadManager.downloadAllBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Non-existent facility name not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-existent facility name");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existent facility name: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		facility = TEST_FACILITY;
		
		
		// Test invalid sensing start time
		sensingStartTime = "illegal-start-time";
		
		try {
			downloadManager.downloadAllBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Invalid sensing start time not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for invalid sensing start time");
		} catch (Exception e) {
			fail("Unexpected exception when testing invalid sensing start time: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		sensingStartTime = TEST_START_TIME;

		
		// Test invalid sensing stop time
		sensingStopTime = "illegal-stop-time";
		
		try {
			downloadManager.downloadAllBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Invalid sensing stop time not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for invalid sensing stop time");
		} catch (Exception e) {
			fail("Unexpected exception when testing invalid sensing stop time: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		sensingStopTime = TEST_STOP_TIME;
		
		
		// Test non-existing product type
		productType = "nonexistent-product-type";
		
		try {
			downloadManager.downloadAllBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Non-existent filename not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-existent filename");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existent filename: " + e.getClass().getName() + "/" + e.getMessage());
		}
		productType = TEST_PRODUCT_TYPE;
		
		
		// Test non-existing time range
		sensingStartTime = "2000-01-01T00:00:00.000000";
		sensingStopTime = "2000-01-01T01:00:00.000000";
		
		try {
			downloadManager.downloadAllBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			fail("Non-existing time range not detected");
		} catch (NoResultException e) {
			logger.info("NoResultException received as expected for non-existing time range");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existing time range: " + e.getClass().getName() + "/" + e.getMessage());
		}
		
		sensingStartTime = TEST_START_TIME;
		sensingStartTime = TEST_STOP_TIME;

		
		// Test successful product download
		try {
			List<RestProduct> restProducts = downloadManager.downloadAllBySensingTime(productType, sensingStartTime, sensingStopTime, facility);
			
			for (RestProduct restProduct: restProducts) {
				assertEquals("Unexpected product type", productType, restProduct.getProductClass());
				assertTrue("Unexpected start time", sensingStartTime.compareTo(restProduct.getSensingStopTime()) <= 0); // hopefully it's as easy as that ...
				assertEquals("Unexpected stop time", sensingStopTime.compareTo(restProduct.getSensingStartTime()) >= 0);
			}
			
			// Wait for download completion and check existence of file
			Thread.sleep(2000L);
			assertTrue("File " + TEST_FILENAME_1 + " not present", Files.exists(Path.of(config.getIngestorSourceDir(), TEST_FILENAME_1)));
			assertTrue("File " + TEST_FILENAME_2 + " not present", Files.exists(Path.of(config.getIngestorSourceDir(), TEST_FILENAME_2)));
			
		} catch (Exception e) {
			fail("Unexpected exception when testing successful product download: " + e.getClass().getName() + "/" + e.getMessage());
		}
	}

}
