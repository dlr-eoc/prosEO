/**
 * ProductArchiveControllerTest.java
 *
 * (C) 2023 DLR
 */
package de.dlr.proseo.archivemgr.rest;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.archivemgr.ProductArchiveManagerApplication;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Testing ProductArchiveControllerImpl.class.
 *
 * TODO test invalid REST requests
 *
 * @author xxx
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProductArchiveManagerApplication.class)
@AutoConfigureTestEntityManager
@WithMockUser(username = "UTM-testuser")
public class ProductArchiveControllerTest {

	// Some users and passwords
	private static final String TEST_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6I…kJqHrdXRWtaUyFN7w";
	private static final String TEST_USER = "testUser";
	private static final String TEST_PASSWORD = "testPassword";

	/** The facility controller under test */
	@Autowired
	private ProductArchiveControllerImpl paci;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveControllerTest.class);

//	/* Test facility */
//	private static String[][] testFacilityData = {
//			// id, version, name, desc, processingEngineUrl, storageMangerUrl
//			{ "0", "0", "TestFacility 1", "Processing Facility 1", "https://www.prosEO-ProcFac1.de/kubernetes1",
//					"https://www.prosEO-ProcFac1.de/proseo/storage-mgr1/v1.0", "S3" },
//			{ "11", "11", "TestFacility 2", "Processing Facility 2", "https://www.prosEO-ProcFac2.de/kubernetes2",
//					"https://www.prosEO-ProcFac2.de/proseo/storage-mgr2/v1.0", "POSIX" },
//			{ "12", "12", "TestFacility 3", "Processing Facility 3", "https://www.prosEO-ProcFac3.de/kubernetes3",
//					"https://www.prosEO-ProcFac3.de/proseo/storage-mg3r/v1.0", "OTHER" } };
//
//	/**
//	 * @throws java.lang.Exception
//	 */
//	@Before
//	public void setUp() throws Exception {
//		createFacility(testFacilityData[0]);
//		createFacility(testFacilityData[1]);
//		createFacility(testFacilityData[2]);
//	}
//
//	/**
//	 * @throws java.lang.Exception
//	 */
//	@After
//	public void tearDown() throws Exception {
//		RepositoryService.getFacilityRepository().deleteAll();
//	}
//
//	/**
//	 * Create a facility from a data array
//	 *
//	 * @param testData an array of Strings representing the facility to create
//	 * @return a facility with its attributes set to the input data
//	 */
//	private ProcessingFacility createFacility(String[] testData) {
//		logger.trace("... creating facility ");
//
//		ProcessingFacility testFacility = new ProcessingFacility();
//		if (null != RepositoryService.getFacilityRepository().findByName(testData[2])) {
//			logger.trace("Found test facility {}", testFacility.getId());
//			return testFacility = RepositoryService.getFacilityRepository().findByName(testData[2]);
//		} else {
//			// testFacility.setId(Long.parseLong(testData[0]));
//			testFacility.setName(testData[2]);
//			testFacility.setDescription(testData[3]);
//			testFacility.setFacilityState(FacilityState.RUNNING);
//			testFacility.setProcessingEngineUrl(testData[4]);
//			testFacility.setProcessingEngineToken(TEST_TOKEN);
//			testFacility.setStorageManagerUrl(testData[5]);
//			testFacility.setLocalStorageManagerUrl(testData[5]);
//			testFacility.setExternalStorageManagerUrl(testData[5]);
//			testFacility.setStorageManagerUser(TEST_USER);
//			testFacility.setStorageManagerPassword(TEST_PASSWORD);
//			testFacility.setDefaultStorageType(StorageType.valueOf(testData[6]));
//
//			testFacility = RepositoryService.getFacilityRepository().save(testFacility);
//
//		}
//		logger.trace("Created test facility {}", testFacility.getId());
//		return testFacility;
//	}
//
//	/**
//	 * Test method for
//	 * {@link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.createFacility(RestFacility)}.
//	 */
//	@Transactional
//	@Test
//	public final void testCreateFacility() {
//		logger.trace(">>> testCreateFacility()");
//
//		// Get a test facility from the database
//		RestProcessingFacility restFacility = FacmgrUtil
//				.toRestFacility(RepositoryService.getFacilityRepository().findAll().get(0));
//
//		// Remove the test facility from the database
//		RepositoryService.getFacilityRepository().deleteById(restFacility.getId());
//
//		// Create a facility with the facility controller
//		ResponseEntity<RestProcessingFacility> getEntity = fci.createFacility(restFacility);
//		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, getEntity.getStatusCode());
//	}
//
//	/**
//	 * Test method for { @link
//	 * de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.deleteFacilityById(Long))}.
//	 */
//	@Test
//	public final void testDeleteFacilityById() {
//		logger.trace(">>> testDeleteFacilityById");
//
//		// Get a test facility from the database
//		ProcessingFacility testFacility = RepositoryService.getFacilityRepository().findAll().get(0);
//
//		// Delete the test facility with the facility controller
//		ResponseEntity<?> entity = fci.deleteFacilityById(testFacility.getId());
//		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, entity.getStatusCode());
//	}
//
//	/**
//	 * Test method for { @link
//	 * de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.getFacilityById(Long))}.
//	 */
//	@Test
//	public final void testGetFacilityById() {
//		logger.trace(">>> testGetFacilityById()");
//
//		// Get a test facility from the database
//		ProcessingFacility testFacility = RepositoryService.getFacilityRepository().findAll().get(0);
//
//		// Retrieve the test facility with the facility controller
//		ResponseEntity<RestProcessingFacility> getEntity = fci.getFacilityById(testFacility.getId());
//		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
//		assertEquals("Wrong facility: ", testFacility.getName(), getEntity.getBody().getName());
//	}
//
//	/**
//	 * Test method for
//	 * {@link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.getFacilities()}.
//	 */
//	@Test
//	public final void testGetFacilities() {
//		logger.trace(">>> testGetFacilities");
//
//		// Get test facilities from the database
//		List<ProcessingFacility> testFacilities = RepositoryService.getFacilityRepository().findAll();
//
//		// Get facilities using different selection criteria
//		ResponseEntity<List<RestProcessingFacility>> retrievedFacilities = fci.getFacilities(null);
//		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedFacilities.getStatusCode());
//		assertEquals("Wrong number of facilities retrieved: ", testFacilities.size(),
//				retrievedFacilities.getBody().size());
//
//		retrievedFacilities = fci.getFacilities("invalid");
//		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, retrievedFacilities.getStatusCode());
//
//		retrievedFacilities = fci.getFacilities(testFacilityData[0][2]);
//		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedFacilities.getStatusCode());
//		assertEquals("Wrong number of facilities retrieved: ", 1, retrievedFacilities.getBody().size());
//	}
//
//	/**
//	 * Test method for
//	 * {@link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.modifyFacility(Long,
//	 * RestFacility)}.
//	 */
//	@Test
//	public final void testModifyFacility() {
//		logger.trace(">>> testModifyFacility()");
//
//		// Modify facility with facility controller
//		RestProcessingFacility restFacility = FacmgrUtil
//				.toRestFacility(RepositoryService.getFacilityRepository().findAll().get(0));
//
//		// Modify facility with facility controller
//		restFacility.setName("Modified_Name");
//		ResponseEntity<RestProcessingFacility> getEntity = fci.modifyFacility(restFacility.getId(), restFacility);
//
//		// Test that the facility attribute was changed as expected
//		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
//		assertEquals("Wrong Name: ", restFacility.getName(), getEntity.getBody().getName());
//		assertEquals("Wrong Description: ", restFacility.getDescription(), getEntity.getBody().getDescription());
//		assertEquals("Wrong Processing Engine URL: ", restFacility.getProcessingEngineUrl(),
//				getEntity.getBody().getProcessingEngineUrl());
//		assertEquals("Wrong Deafult storage type value: ", restFacility.getDefaultStorageType().toString(),
//				getEntity.getBody().getDefaultStorageType());
//		assertEquals("Wrong Storage URL: ", restFacility.getStorageManagerUrl(),
//				getEntity.getBody().getStorageManagerUrl());
//	}

	@Test
	public final void test() {
	}
}
