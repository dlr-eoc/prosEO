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
import de.dlr.proseo.archivemgr.rest.model.ProductArchiveModelMapper;
import de.dlr.proseo.archivemgr.rest.model.RestProductArchive;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.enums.ArchiveType;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Testing ProductArchiveControllerImpl.class.
 *
 * TODO test invalid REST requests
 *
 * @author Denys Chaykovskiy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProductArchiveManagerApplication.class)
@AutoConfigureTestEntityManager
@Transactional
@WithMockUser(username = "UTM-testuser")
public class ProductArchiveControllerTest {

	// Some users and passwords
	// private static final String TEST_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6I…kJqHrdXRWtaUyFN7w";
	private static final String TEST_USER = "testUser";
	private static final String TEST_PASSWORD = "testPassword";

	/** The Product Archive under test */
	@Autowired
	private ProductArchiveControllerImpl paci;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveControllerTest.class);

	/* Test product Archive */
	private static String[][] testArchiveData = {
			
			// id, archiveType(enum), baseUri, clientId, clientSecret, 
			// code, context, name, password, 
			// sendAuthinBody(boolean), tokenRequired(boolean), tokenUri, username
			
			{ "1", "AIP", "https://www.someurl1.de", "testClientID1", "testClientService1",
				"testCode1", "testContext1", "testName1", TEST_PASSWORD, 
				"true", "true", "https://www.sometokenurl1.de", TEST_USER  },
			
			{ "2", "AIP", "https://www.someurl2.de", "testClientID2", "testClientService2",
					"testCode2", "testContext2", "testName2", TEST_PASSWORD, 
					"true", "true", "https://www.sometokenurl2.de", TEST_USER  },
			
			{ "3", "AIP", "https://www.someurl3.de", "testClientID3", "testClientService3",
						"testCode", "testContext", "testName", TEST_PASSWORD, 
						"true", "true", "https://www.sometokenurl3.de", TEST_USER  } };

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		createProductArchive(testArchiveData[0]);
		createProductArchive(testArchiveData[1]);
		createProductArchive(testArchiveData[2]);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		RepositoryService.getProductArchiveRepository().deleteAll();
	}

	/**
	 * Create a product archive from a data array
	 *
	 * @param testData an array of Strings representing the product archive to create
	 * @return a product archive with its attributes set to the input data
	 */
	private ProductArchive createProductArchive(String[] testData) {
		
		logger.trace("... creating archive ");

		ProductArchive testArchive = new ProductArchive();

		String archiveCode = testData[5];
		
		if (null != RepositoryService.getProductArchiveRepository().findByCode(archiveCode)) {
			
			logger.trace("Found test product archive {}", testArchive.getId());
			return testArchive = RepositoryService.getProductArchiveRepository().findByCode(archiveCode);
			
		} else {
			// testArchive.setId(Long.parseLong(testData[0]));
			testArchive.setArchiveType(ArchiveType.valueOf(testData[1]));
			
			testArchive.setBaseUri(testData[2]);
			testArchive.setClientId(testData[3]);
			testArchive.setClientSecret(testData[4]);
			testArchive.setCode(testData[5]);
			testArchive.setContext(testData[6]);
			testArchive.setName(testData[7]);
			testArchive.setPassword(testData[8]);
			testArchive.setSendAuthInBody(Boolean.valueOf(testData[9]));
			testArchive.setTokenRequired(Boolean.valueOf(testData[10]));
			testArchive.setTokenUri(testData[11]);
			testArchive.setUsername(testData[12]);
			
			// Set<ProductClass> availableProductClasses = new HashSet<>();		
			// testArchive.setAvailableProductClasses(availableProductClasses);

			testArchive = RepositoryService.getProductArchiveRepository().save(testArchive);
			
		}
		logger.trace("Created test product archive {}", testArchive.getId());
		return testArchive;
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.archivemgr.rest.ProductArchiveControllerImpl.createArchive(RestProductArchive)}.
	 */
	@Transactional
	@Test
	public final void testCreateArchive() {
		
		logger.trace(">>> testCreateArchive()");

		// Get a test archive from the database
		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findAll().get(0);
		RestProductArchive restArchive = new ProductArchiveModelMapper(modelArchive).toRest();

		// Remove the test archive from the database
		RepositoryService.getProductArchiveRepository().deleteById(restArchive.getId());

		// Create a archive with the archive controller
		ResponseEntity<RestProductArchive> getEntity = paci.createArchive(restArchive);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, getEntity.getStatusCode());
	}

	/**
	 * Test method for { @link
	 * de.dlr.proseo.archivemgr.rest.ProductArchiveControllerImpl.deleteArchiveById(Long))}.
	 */
	@Test
	public final void testDeleteArchiveById() {
		
		logger.trace(">>> testDeleteArchiveById");

		// Get a test product archive from the database
		ProductArchive testArchive = RepositoryService.getProductArchiveRepository().findAll().get(0);
		
		System.out.println("Size=" + RepositoryService.getProductArchiveRepository().findAll().size());

		// Delete the test archive with the product archive controller
		ResponseEntity<?> entity = paci.deleteArchiveById(testArchive.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, entity.getStatusCode());
	}

	/**
	 * Test method for { @link
	 * de.dlr.proseo.archivemgr.rest.ProductArchiveControllerImpl.getArchiveById(Long))}.
	 */
	@Test
	public final void testGetArchiveById() {
		
		logger.trace(">>> testGetFacilityById()");

		// Get a test productArchive from the database
		ProductArchive testArchive = RepositoryService.getProductArchiveRepository().findAll().get(0);

		// Retrieve the test product archive with the product archive controller
		ResponseEntity<RestProductArchive> getEntity = paci.getArchiveById(testArchive.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong product archive: ", testArchive.getName(), getEntity.getBody().getName());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.archivemgr.rest.ProductArchiveControllerImpl.getArchives()}.
	 */
	@Test
	public final void testGetArchives() {
		
		logger.trace(">>> testGetArchives");

		// Get test archives from the database
		List<ProductArchive> testArchives = RepositoryService.getProductArchiveRepository().findAll();

		// Get archives using different selection criteria
		ResponseEntity<List<RestProductArchive>> retrievedArchives = paci.getArchives(null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedArchives.getStatusCode());
		assertEquals("Wrong number of archives retrieved: ", testArchives.size(),
				retrievedArchives.getBody().size());

		retrievedArchives = paci.getArchives("invalid");
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, retrievedArchives.getStatusCode());

		retrievedArchives = paci.getArchives(testArchiveData[0][5]);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedArchives.getStatusCode());
		assertEquals("Wrong number of archives retrieved: ", 1, retrievedArchives.getBody().size());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.archivemgr.rest.ProductArchiveControllerImpl.modifyArchive(Long,
	 * RestProductArchive)}.
	 */
	@Test
	public final void testModifyArchive() {
		
		logger.trace(">>> testModifyArchive()");

		// Modify product archive with product archive controller
		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findAll().get(0);
		RestProductArchive restArchive = new ProductArchiveModelMapper(modelArchive).toRest();

		restArchive.setName("Modified_Name");
		ResponseEntity<RestProductArchive> getEntity = paci.modifyArchive(restArchive.getId(), restArchive);

		// Test that the product archive attribute was changed as expected
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong Name: ", restArchive.getName(), getEntity.getBody().getName());
	}
}
