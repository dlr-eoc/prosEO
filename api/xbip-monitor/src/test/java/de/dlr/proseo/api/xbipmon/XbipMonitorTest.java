/**
 * XbipMonitorTest.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.xbipmon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import de.dlr.proseo.api.xbipmon.rest.model.RestInterfaceStatus;

/**
 * @author thomas
 *
 */
@SpringBootTest(classes = XbipMonitorApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class XbipMonitorTest {
	
	private static final String TEST_XBIP_MONITOR_ID = "S1B-SGS-01";
	private static final String XBIP_MONITOR_BASE_URI = "/proseo/xbip-monitor/v0.1/" + TEST_XBIP_MONITOR_ID;
	private static final String TEST_DATA_DIR = "target/test";
	private static final String MSG_UNABLE_TO_DELETE_DIRECTORY = "Unable to delete directory/file path {} (cause: {})";

	@LocalServerPort
	private int port;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
 		
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(XbipMonitorTest.class);
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		// Make sure target directory does not exist
		Path testDataPath = Path.of(TEST_DATA_DIR);
		if (!Files.exists(testDataPath)) {
			// The data directory was never created, so it does not need to be cleaned up
			return;
		}
		try {
			Files.walkFileTree(testDataPath, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					// Nothing to do
					if (logger.isTraceEnabled()) logger.trace("... before visiting directory " + dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						if (logger.isTraceEnabled()) logger.trace("... deleting file " + file);
						Files.delete(file);
					} catch (IOException e) {
						logger.error(MSG_UNABLE_TO_DELETE_DIRECTORY, file.toString(), e.getMessage());
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					logger.error(MSG_UNABLE_TO_DELETE_DIRECTORY, file.toString(), "Call to visitFileFailed");
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					try {
						if (logger.isTraceEnabled()) logger.trace("... deleting directory " + dir);
						Files.delete(dir);
					} catch (IOException e) {
						logger.error(MSG_UNABLE_TO_DELETE_DIRECTORY, dir.toString(), e.getMessage());
					}
					return FileVisitResult.CONTINUE;
				}});
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(MSG_UNABLE_TO_DELETE_DIRECTORY, TEST_DATA_DIR, e.getMessage());
		}
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public final void test() {
		logger.info("Sleeping for 5 s in test method to allow monitor to do some work");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			logger.info("Test method interrupted!");
		}
		
		String testUrl = "http://localhost:" + this.port + XBIP_MONITOR_BASE_URI + "/status";
		
		
		logger.info(String.format("Status request returned Id %s, availability %s, performance %G", 
				restInterfaceStatus.getBody().getId(),
				restInterfaceStatus.getBody().getAvailable(),
				restInterfaceStatus.getBody().getPerformance()));
		
		assertEquals(TEST_XBIP_MONITOR_ID, restInterfaceStatus.getBody().getId(), "Unexpected XBIP Monitor identifier: ");
		assertEquals(true, restInterfaceStatus.getBody().getAvailable(), "Unexpected availability: ");
		assertTrue(Double.valueOf(0.0) < restInterfaceStatus.getBody().getPerformance(), "Unexpected performance: ");
		
		Path historyFilePath = Paths.get(TEST_DATA_DIR + File.separator + "history.file");
		assertTrue(Files.exists(historyFilePath), "History file not found");
		try {
			assertEquals(106, Files.size(historyFilePath), "History file has unexpected size: ");
		} catch (IOException e) {
			fail("Cannot access history file, cause: " + e.getMessage());
		}
		assertTrue(Files.exists(Paths.get(TEST_DATA_DIR, "cadu/DCS_01_S1B_20210726001122345678_dat/ch_1", "DCS_01_S1B_20210726001122345678_ch1_DSIB.xml")), "DSIB file for ch. 1 of first session not found");
	}

}
