/**
 * CadipMonitorTest.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.cadipmon.rest;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.joda.time.Instant;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import de.dlr.proseo.api.cadipmon.CadipMonitor;
import de.dlr.proseo.api.cadipmon.CadipMonitorApplication;
import de.dlr.proseo.api.cadipmon.CadipMonitorConfiguration;

/**
 * Test session and CADU downloads from remote CADU Interface Delivery Point
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CadipMonitorApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class CadipMonitorTest {
	
	// Test reference data
	private static final String TEST_REFERENCE_TIME = "2023-06-05T00:00:00.000Z";

	// Mock CADIP test data
	private static final String TEST_DCS_PREFIX = "DCS_";
	
	private static final String TEST_SESSION_ID = "PTS_20230605102135012345";
	private static final String TEST_SESSION_UUID = "2b17b57d-fff4-4645-b539-91f305c27c69";
	private static final Integer TEST_SESSION_NUMCH = 2;
	private static final String TEST_SESSION_PUBDATE = "2023-06-05T10:22:36.789Z";
	private static final String TEST_SESSION_SAT = "PTS";
	private static final String TEST_SESSION_SUID = "01";
	private static final Integer TEST_SESSION_ORBIT = 12345;
	
	private static final String TEST_SESSION_PATH = TEST_DCS_PREFIX + TEST_SESSION_SUID + "_" + TEST_SESSION_ID + "_dat";
	
	private static final String TEST_FILE1_UUID = "be4862b5-d4d7-4975-a97d-d7d4fdadab31";
	private static final String TEST_FILE1_NAME = 
			TEST_DCS_PREFIX + TEST_SESSION_SUID + "_" + TEST_SESSION_ID + "_ch1_DSDB_00001.raw";
	private static final Integer TEST_FILE1_CH = 1;
	private static final Integer TEST_FILE1_BLOCK = 1;
	private static final Boolean TEST_FILE1_FINAL = false;
	private static final String TEST_FILE1_DATA = "testdata1 for UUID " + TEST_FILE1_UUID;
	private static final Integer TEST_FILE1_SIZE = TEST_FILE1_DATA.length();
	
	private static final String TEST_FILE2_UUID = "ab6e429e-f942-4591-a10d-4dd5ff6df7a9";
	private static final String TEST_FILE2_NAME = 
			TEST_DCS_PREFIX + TEST_SESSION_SUID + "_" + TEST_SESSION_ID + "_ch2_DSDB_00001.raw";
	private static final Integer TEST_FILE2_CH = 2;
	private static final Integer TEST_FILE2_BLOCK = 1;
	private static final Boolean TEST_FILE2_FINAL = true;
	private static final String TEST_FILE2_DATA = "testdata2 for UUID " + TEST_FILE2_UUID;
	private static final Integer TEST_FILE2_SIZE = TEST_FILE2_DATA.length();

	private static final String URL_ODATA = "/odata/v1/";
	private static final String URL_CADIP = "/cadip" + URL_ODATA;
	
	private static final String CADU_SESSION =
			"{"
			+ "\"Id\":\"" + TEST_SESSION_UUID + "\","
			+ "\"SessionId\":\"" + TEST_SESSION_ID + "\","
			+ "\"NumChannels\":" + TEST_SESSION_NUMCH + ","
			+ "\"PublicationDate\":\"" + TEST_SESSION_PUBDATE + "\","
			+ "\"Satellite\":\"" + TEST_SESSION_SAT + "\","
			+ "\"StationUnitId\":\"" + TEST_SESSION_SUID + "\","
			+ "\"DownlinkOrbit\":" + TEST_SESSION_ORBIT + ","
			+ "\"AcquisitionId\":\"415_01\","
			+ "\"AntennaId\":\"SIV\","
			+ "\"FrontEndId\":\"FEP1\","
			+ "\"Retransfer\":false,"
			+ "\"AntennaStatusOK\":true,"
			+ "\"FrontEndStatusOK\":true,"
			+ "\"PlannedDataStart\":\"2023-06-05T10:21:45.456Z\","
			+ "\"PlannedDataStop\":\"2023-06-05T10:29:56.123Z\","
			+ "\"DownlinkStart\":\"2023-06-05T10:21:45.456Z\","
			+ "\"DownlinkStop\":\"2023-06-05T10:29:56.123Z\","
			+ "\"DownlinkStatusOK\":true,"
			+ "\"DeliveryPushOK\":true"
			+ "}";
	
	private static final String CADU_FILE_1 =
			"{"
			+ "\"Id\":\"" + TEST_FILE1_UUID +"\","
			+ "\"Name\":\"" + TEST_FILE1_NAME +"\","
			+ "\"SessionId\":\"" + TEST_SESSION_ID +"\","
			+ "\"Channel\":" + TEST_FILE1_CH +","
			+ "\"BlockNumber\":" + TEST_FILE1_BLOCK +","
			+ "\"FinalBlock\":" + TEST_FILE1_FINAL +","
			+ "\"PublicationDate\":\"" + TEST_SESSION_PUBDATE +"\","
			+ "\"EvictionDate\":\"2023-06-05T10:21:45.456Z\","
			+ "\"Size\":" + TEST_FILE1_SIZE +","
			+ "\"Retransfer\":false"
			+ "}";
	
	private static final String CADU_FILE_2 =
			"{"
			+ "\"Id\":\"" + TEST_FILE2_UUID +"\","
			+ "\"Name\":\"" + TEST_FILE2_NAME +"\","
			+ "\"SessionId\":\"" + TEST_SESSION_ID +"\","
			+ "\"Channel\":" + TEST_FILE2_CH +","
			+ "\"BlockNumber\":" + TEST_FILE2_BLOCK +","
			+ "\"FinalBlock\":" + TEST_FILE2_FINAL +","
			+ "\"PublicationDate\":\"" + TEST_SESSION_PUBDATE +"\","
			+ "\"EvictionDate\":\"2023-06-05T10:21:45.456Z\","
			+ "\"Size\":" + TEST_FILE2_SIZE +","
			+ "\"Retransfer\":false"
			+ "}";
	
	private static final String CADIP_QUERY_SESSIONS =
			"Sessions?%24filter=Satellite%20eq%20'" + TEST_SESSION_SAT + "'"
			+ "%20and%20"
			+ "Retransfer%20eq%20false"
			+ "%20and%20"
			+ "PublicationDate%20gt%20" + TEST_REFERENCE_TIME.replaceAll(":", "%3A")
			+ "&%24count=true&%24top=1000&%24orderby=PublicationDate%20asc";
	
	private static final String CADIP_RESPONSE_SESSIONS =
			"{\"@odata.context\":\"$metadata#Sessions\",\"@odata.count\":1,\"value\":[" 
			+ CADU_SESSION
			+ "]}";
	
	private static final String CADIP_QUERY_FILES =
			"Files?%24filter=SessionId%20eq%20'" + TEST_SESSION_ID + "'"
			+ "&%24count=true&%24top=1000&%24orderby=PublicationDate%20asc";
	
	private static final String CADIP_RESPONSE_FILES =
			"{\"@odata.context\":\"$metadata#Files\",\"@odata.count\":2,\"value\":[" 
			+ CADU_FILE_1
			+ ","
			+ CADU_FILE_2
			+ "]}";
	
	private static final String CADIP_DOWNLOAD_BY_UUID_1 =
			"Files(" + TEST_FILE1_UUID + ")/$value";
	
	private static final String CADIP_DOWNLOAD_BY_UUID_2 =
			"Files(" + TEST_FILE2_UUID + ")/$value";
	
	/** CADIP monitor configuration */
	@Autowired
	private CadipMonitorConfiguration config;
	
	/** CADIP Monitor */
	@Autowired
	private CadipMonitor cadipMonitor;

	/** A mock CADIP service */
	private static int WIREMOCK_PORT = 9876;
	private static WireMockServer mockCadipService;
    
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CadipMonitorTest.class);
	
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
			om.readTree(CADIP_RESPONSE_SESSIONS);
		} catch (Exception e) {
			logger.error("CADU sessions response not parseable (cause: {} / {})", e.getClass(), e.getMessage());
			failed = true;
		}
		try {
			om.readTree(CADIP_RESPONSE_FILES);
		} catch (Exception e) {
			logger.error("CADU files response not parseable (cause: {} / {})", e.getClass(), e.getMessage());
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
	}
	
	/**
	 * Configures a mock CADIP service
	 */
	private void setUpMockCadipService() {
		if (logger.isTraceEnabled()) logger.trace(">>> setUpMockCadipService()");
		
		mockCadipService = new WireMockServer(WIREMOCK_PORT);
		mockCadipService.start();
		
		// CADIP Query
		
		mockCadipService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_CADIP + CADIP_QUERY_SESSIONS))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
						.withBody(CADIP_RESPONSE_SESSIONS)));
		
		mockCadipService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_CADIP + CADIP_QUERY_FILES))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
						.withBody(CADIP_RESPONSE_FILES)));
		
		// CADIP Download
		
		mockCadipService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_CADIP + CADIP_DOWNLOAD_BY_UUID_1))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString())
						.withBody(TEST_FILE1_DATA)));
		
		mockCadipService.stubFor(WireMock.get(WireMock.urlEqualTo(URL_CADIP + CADIP_DOWNLOAD_BY_UUID_2))
				.willReturn(
						WireMock.aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString())
						.withBody(TEST_FILE2_DATA)));
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> setUp()");
		
		// Set up mock CADIP service
		setUpMockCadipService();
		
		// Ensure target directory for product downloads exists
		FileUtils.forceMkdir(new File(config.getL0CaduDirectoryPath()));
		
		// Ensure directories and files to download are not present
		Files.walk(Path.of(config.getL0CaduDirectoryPath()))
			.forEach(path -> {
					try {
						if (!path.equals(Path.of(config.getL0CaduDirectoryPath()))) {
							Files.delete(path);
						}
					} catch (IOException e) {
						logger.error("Cannot delete path {}", path);
					} 
				});
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> tearDown()");
		
		// Stop mock CADIP service
		mockCadipService.stop();
		
	}

	/**
	 * Test method for {@link de.dlr.proseo.api.basemon.BaseMonitor#run(java.lang.Integer)}.
	 */
	@Test
	public final void testRunInteger() {
		if (logger.isTraceEnabled()) logger.trace(">>> testRunInteger()");

		// Create history file
		try (FileWriter historyFile = new FileWriter(config.getCadipHistoryPath())) {
			historyFile.write(
					Instant.parse(TEST_REFERENCE_TIME).toString()
					+ ";"
					+ TEST_DCS_PREFIX + TEST_SESSION_SUID + "PTS_20230605000000012340"
					+ "\n");
		} catch (IOException e) {
			logger.error("Cannot create history file (cause: {} / {})", e.getClass(), e.getMessage());
			fail("Test data validation failed");
		}
		
		// Run the monitor
		logger.info("Sleeping for 10 s in test method to allow monitor to do some work");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			logger.info("Test method interrupted!");
		}
		
//		cadipMonitor.run(1);
		
		logger.info("CADIP Monitor run completed, checking outcome");
		
		// Check download result
		assertTrue("Session directory missing", 
				Files.isDirectory(Path.of(config.getL0CaduDirectoryPath(), TEST_SESSION_PATH)));
		assertTrue("Session ch.1 directory missing", 
				Files.isDirectory(Path.of(config.getL0CaduDirectoryPath(), TEST_SESSION_PATH, "ch_1")));
		assertTrue("Session ch.2 directory missing", 
				Files.isDirectory(Path.of(config.getL0CaduDirectoryPath(), TEST_SESSION_PATH, "ch_1")));
		assertTrue("Session file 1 missing", 
				Files.exists(Path.of(config.getL0CaduDirectoryPath(), TEST_SESSION_PATH, "ch_1", TEST_FILE1_NAME)));
		assertTrue("Session file 2 missing", 
				Files.exists(Path.of(config.getL0CaduDirectoryPath(), TEST_SESSION_PATH, "ch_2", TEST_FILE2_NAME)));

		try {
			assertEquals("Session file 1 has wrong size", TEST_FILE1_SIZE.longValue(),
					Files.size(Path.of(config.getL0CaduDirectoryPath(), TEST_SESSION_PATH, "ch_1", TEST_FILE1_NAME)));
			assertEquals("Session file 2 has wrong size", TEST_FILE2_SIZE.longValue(),
					Files.size(Path.of(config.getL0CaduDirectoryPath(), TEST_SESSION_PATH, "ch_2", TEST_FILE2_NAME)));
		} catch (IOException e) {
			logger.error("Cannot check file size (cause: {} / {})", e.getClass(), e.getMessage());
			fail("File size validation failed");
		}

		logger.info("CADIP Monitor test successfully completed");
		
	}
}
