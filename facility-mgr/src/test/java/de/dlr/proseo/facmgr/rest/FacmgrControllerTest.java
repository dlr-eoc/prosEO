/**
 * FacmgrControllerTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.facmgr.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.facmgr.FacilityManager;
import de.dlr.proseo.facmgr.rest.model.FacmgrUtil;
import de.dlr.proseo.facmgr.rest.model.RestProcessingFacility;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Testing FacmgrControllerImpl.class.
 *
 * @author Katharina Bassler
 */
@SpringBootTest(classes = FacilityManager.class)
@WithMockUser(username = "UTM-testuser")
@Transactional
public class FacmgrControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(FacmgrControllerTest.class);

	/** The facility controller under test */
	@Autowired
	private FacmgrControllerImpl fci;

	// Some users and passwords
	private static final String TEST_PASSWORD = "testPassword";
	private static final String TEST_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6I…kJqHrdXRWtaUyFN7w";
	private static final String TEST_USER = "testUser";

	/** Test facility data */
	private static String[][] testFacilityData = {
			// id, version, name, description, processingEngineUrl, storageMangerUrl
			{ "0", "0", "TestFacility 1", "Processing Facility 1", "https://www.prosEO-ProcFac1.de/kubernetes1",
					"https://www.prosEO-ProcFac1.de/proseo/storage-mgr1/v1.0", "S3" },
			{ "11", "11", "TestFacility 2", "Processing Facility 2", "https://www.prosEO-ProcFac2.de/kubernetes2",
					"https://www.prosEO-ProcFac2.de/proseo/storage-mgr2/v1.0", "POSIX" },
			{ "12", "12", "TestFacility 3", "Processing Facility 3", "https://www.prosEO-ProcFac3.de/kubernetes3",
					"https://www.prosEO-ProcFac3.de/proseo/storage-mg3r/v1.0", "OTHER" } };

	/**
	 * Create a facility from a data array
	 *
	 * @param testData an array of Strings representing the facility to create
	 * @return a facility with its attributes set to the input data
	 */
	private ProcessingFacility createFacility(String[] testData) {
		logger.trace("... creating facility ");

		ProcessingFacility testFacility = new ProcessingFacility();

		if (null != RepositoryService.getFacilityRepository().findByName(testData[2])) {
			logger.trace("Found test facility {}", testFacility.getId());

			return testFacility = RepositoryService.getFacilityRepository().findByName(testData[2]);
		} else {
			testFacility.setName(testData[2]);
			testFacility.setDescription(testData[3]);
			testFacility.setFacilityState(FacilityState.RUNNING);
			testFacility.setProcessingEngineUrl(testData[4]);
			testFacility.setProcessingEngineToken(TEST_TOKEN);
			testFacility.setStorageManagerUrl(testData[5]);
			testFacility.setLocalStorageManagerUrl(testData[5]);
			testFacility.setExternalStorageManagerUrl(testData[5]);
			testFacility.setStorageManagerUser(TEST_USER);
			testFacility.setStorageManagerPassword(TEST_PASSWORD);
			testFacility.setDefaultStorageType(StorageType.valueOf(testData[6]));

			testFacility = RepositoryService.getFacilityRepository().save(testFacility);
			testData[0] = String.valueOf(testFacility.getId());
		}

		logger.trace("Created test facility {}", testFacility.getId());

		return testFacility;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		logger.trace(">>> creating test data in the database");

		createFacility(testFacilityData[0]);
		createFacility(testFacilityData[1]);
		createFacility(testFacilityData[2]);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
		logger.trace(">>> clearing test data from the database");

		RepositoryService.getFacilityRepository().deleteAll();
	}

	/**
	 * Test method for {@link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.createFacility(RestFacility)}.
	 */
	@Test
	void testCreateFacility() {
		logger.trace(">>> testCreateFacility()");

		// Get a test facility from the database
		RestProcessingFacility restFacility = FacmgrUtil
			.toRestFacility(RepositoryService.getFacilityRepository().findByName(testFacilityData[0][2]));

		// Remove the test facility from the database
		RepositoryService.getFacilityRepository().deleteById(restFacility.getId());

		// Create a facility with the facility controller
		restFacility.setId(null);
		ResponseEntity<RestProcessingFacility> response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Wrong HTTP status: ");
		long idToDelete = response.getBody().getId();

		// Attempt to create a duplicate facility
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		RepositoryService.getFacilityRepository().deleteById(idToDelete);

		// Attempt creation without storage manager URL
		restFacility.setStorageManagerUrl(null);
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");

		// Attempt creation without storage manager URL
		restFacility.setStorageManagerUrl("");
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setStorageManagerUrl(testFacilityData[0][5]);

		// Attempt creation without storage manager user
		restFacility.setStorageManagerUser(null);
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");

		// Attempt creation without storage manager user
		restFacility.setStorageManagerUser("");
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setStorageManagerUser(TEST_USER);

		// Attempt creation without storage manager password
		restFacility.setStorageManagerPassword(null);
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");

		// Attempt creation without storage manager password
		restFacility.setStorageManagerPassword("");
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setStorageManagerPassword(TEST_USER);

		// Attempt creation without default storage type
		restFacility.setDefaultStorageType(null);
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");

		// Attempt creation without storage manager password
		restFacility.setDefaultStorageType("");
		response = fci.createFacility(restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setStorageManagerPassword(testFacilityData[0][6]);
	}

	/**
	 * Test method for { @link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.deleteFacilityById(Long))}.
	 */
	@Test
	void testDeleteFacilityById() {
		logger.trace(">>> testDeleteFacilityById");

		// Get a test facility from the database
		ProcessingFacility testFacility = RepositoryService.getFacilityRepository().findByName(testFacilityData[0][2]);

		// Delete the test facility with the facility controller
		ResponseEntity<?> response = fci.deleteFacilityById(testFacility.getId());
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "Wrong HTTP status: ");
	}

	/**
	 * Test method for {@link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.getFacilities()}.
	 */
	@Test
	void testGetFacilities() {
		logger.trace(">>> testGetFacilities");

		// Get test facilities from the database
		List<ProcessingFacility> testFacilities = RepositoryService.getFacilityRepository().findAll();

		// Get facilities using different selection criteria
		ResponseEntity<List<RestProcessingFacility>> retrievedFacilities = fci.getFacilities(null);
		assertEquals(HttpStatus.OK, retrievedFacilities.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(testFacilities.size(), retrievedFacilities.getBody().size(), "Wrong number of facilities retrieved: ");

		retrievedFacilities = fci.getFacilities("invalid");
		assertEquals(HttpStatus.NOT_FOUND, retrievedFacilities.getStatusCode(), "Wrong HTTP status: ");

		retrievedFacilities = fci.getFacilities(testFacilityData[0][2]);
		assertEquals(HttpStatus.OK, retrievedFacilities.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(1, retrievedFacilities.getBody().size(), "Wrong number of facilities retrieved: ");
	}

	/**
	 * Test method for { @link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.getFacilityById(Long))}.
	 */
	@Test
	void testGetFacilityById() {
		logger.trace(">>> testGetFacilityById()");

		// Get a test facility from the database
		ProcessingFacility testFacility = RepositoryService.getFacilityRepository().findByName(testFacilityData[0][2]);

		// Retrieve the test facility with the facility controller
		ResponseEntity<RestProcessingFacility> response = fci.getFacilityById(testFacility.getId());
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(testFacility.getName(), response.getBody().getName(), "Wrong facility: ");
	}

	/**
	 * Test method for {@link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.modifyFacility(Long, RestFacility)}.
	 */
	@Test
	void testModifyFacility() {
		logger.trace(">>> testModifyFacility()");

		// Modify facility with facility controller
		RestProcessingFacility restFacility = FacmgrUtil
			.toRestFacility(RepositoryService.getFacilityRepository().findByName(testFacilityData[0][2]));
		long id = restFacility.getId();

		// Modify facility with facility controller
		restFacility.setStorageManagerUrl(testFacilityData[1][5]);
		ResponseEntity<RestProcessingFacility> response = fci.modifyFacility(id, restFacility);

		// Test that the facility attribute was changed as expected
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(restFacility.getName(), response.getBody().getName(), "Wrong Name: ");
		assertEquals(restFacility.getDescription(), response.getBody().getDescription(), "Wrong Description: ");
		assertEquals(restFacility.getProcessingEngineUrl(), response.getBody().getProcessingEngineUrl(),
				"Wrong Processing Engine URL: ");
		assertEquals(restFacility.getDefaultStorageType().toString(), response.getBody().getDefaultStorageType(),
				"Wrong Deafult storage type value: ");
		assertEquals(restFacility.getStorageManagerUrl(), response.getBody().getStorageManagerUrl(), "Wrong Storage URL: ");

		// Attempt modification without storage manager URL
		restFacility.setStorageManagerUrl(null);
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");

		// Attempt modification without storage manager URL
		restFacility.setStorageManagerUrl("");
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setStorageManagerUrl(testFacilityData[0][5]);

		// Attempt modification without storage manager user
		restFacility.setStorageManagerUser(null);
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");

		// Attempt modification without storage manager user
		restFacility.setStorageManagerUser("");
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setStorageManagerUser(TEST_USER);

		// Attempt modification without storage manager password
		restFacility.setStorageManagerPassword(null);
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");

		// Attempt modification without storage manager password
		restFacility.setStorageManagerPassword("");
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setStorageManagerPassword(TEST_USER);

		// Attempt modification without default storage type
		restFacility.setDefaultStorageType(null);
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");

		// Attempt modification without storage manager password
		restFacility.setDefaultStorageType("");
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setStorageManagerPassword(testFacilityData[0][6]);

		// Attempt illegal state transition
		restFacility.setFacilityState(FacilityState.STARTING.toString());
		response = fci.modifyFacility(id, restFacility);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		restFacility.setFacilityState(FacilityState.RUNNING.toString());
	}
}