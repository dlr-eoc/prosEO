/**
 * DownloadManagerTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient.rest;

import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.api.aipclient.AipClientApplication;
import de.dlr.proseo.api.aipclient.AipClientTestConfiguration;
import de.dlr.proseo.api.aipclient.rest.model.RestProduct;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Test product downloads from remote Long-term Archives
 * 
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AipClientApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class DownloadManagerTest {
	
	private static final String TEST_FACILITY = "testfac";
	private static final String TEST_FILENAME = "testfilename";
	private static final String TEST_PRODUCT_TYPE = "testproducttype";
	private static final String TEST_START_TIME = "2022-11-17T09:43:22.123456";
	private static final String TEST_STOP_TIME = "2022-11-17T10:54:33.234567";
	
	/** Password encoder */
	private static final BCryptPasswordEncoder pwEncoder = new BCryptPasswordEncoder();
	
	/** Test configuration */
	@Autowired
	private AipClientTestConfiguration config;

	/** Download Manager */
	@Autowired
	private DownloadManager downloadManager;

	/** Database access via JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DownloadManagerTest.class);
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> setUpBeforeClass()");
		
		// Set up mock LTA for external product "download"

	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> tearDownAfterClass()");

	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> setUp()");
		
		String query = "select * from users;";
		List<?> queryResult = em.createNativeQuery(query).getResultList();
		logger.info("Found {} user entries", queryResult.size());

		// Create test products in local database
		ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(TEST_FACILITY);
		if (null == facility) {
			facility = new ProcessingFacility();
			facility.setName(TEST_FACILITY);
			RepositoryService.getFacilityRepository().save(facility);
		}
		
		// TODO
				
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> tearDown()");

	}

	/**
	 * Test method for {@link de.dlr.proseo.api.aipclient.rest.DownloadManager#downloadByName(java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testDownloadByName() {
		if (logger.isTraceEnabled()) logger.trace(">>> testDownloadByName()");

		// Correct test data
		String filename = TEST_FILENAME;
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
		
		filename = TEST_FILENAME;
	
		
		// Test successful download
		try {
			RestProduct restProduct = downloadManager.downloadByName(filename, facility);
			
			assertEquals("Unexpected filename", filename, restProduct.getProductFile().get(0).getProductFileName());
			
			// TODO Wait for download completion and check existence of file
			
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
			fail("Non-existent filename not detected");
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException received as expected for non-existent filename");
		} catch (Exception e) {
			fail("Unexpected exception when testing non-existent filename: " + e.getClass().getName() + "/" + e.getMessage());
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
			
			// TODO Wait for download completion and check existence of file
			
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
			
			// TODO Wait for download completion and check existence of files
			
		} catch (Exception e) {
			fail("Unexpected exception when testing successful product download: " + e.getClass().getName() + "/" + e.getMessage());
		}
	}

}
