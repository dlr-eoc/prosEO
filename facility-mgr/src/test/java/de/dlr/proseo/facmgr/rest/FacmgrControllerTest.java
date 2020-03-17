package de.dlr.proseo.facmgr.rest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.facmgr.FacilityManager;
import de.dlr.proseo.facmgr.FacilitymgrSecurityConfig;
import de.dlr.proseo.facmgr.rest.model.RestFacility;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.service.RepositoryService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FacilityManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@AutoConfigureTestEntityManager
public class FacmgrControllerTest {
	/* The base URI of the Orders */
	private static String FACILITY_BASE_URI = "/proseo/facility-mgr/v0.1";

	@LocalServerPort
	private int port;
	
	@Autowired
	EntityManagerFactory emf;
	
	/** Test configuration */
	@Autowired
	FacmgrTestConfiguration config;
	
	/** The security environment for this test */
	@Autowired
	FacilitymgrSecurityConfig facmgrSecurityConfig;
	
	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FacmgrControllerTest.class);
	
	/* Test facility */
	private static String[][] testFacilityData = {
			// id, version, name, desc,processingEngineUrl, storageMangerUrl
			{ "0", "0", "TestFacility 1", "Processing Facility 1", "https://www.prosEO-ProcFac1.de/kubernetes","https://www.prosEO-ProcFac1.de/proseo/storage-mgr/v1.0"},
			{ "11", "11", "TestFacility 2", "Processing Facility 2", "https://www.prosEO-ProcFac2.de/kubernetes","https://www.prosEO-ProcFac2.de/proseo/storage-mgr/v1.0"},
			{ "12", "12", "TestFacility 3", "Processing Facility 3", "https://www.prosEO-ProcFac3.de/kubernetes","https://www.prosEO-ProcFac3.de/proseo/storage-mgr/v1.0"}
		};
	
	/**
	 * Create a facility from a data array
	 * 
	 * @param testData an array of Strings representing the facility to create
	 * @return a facility with its attributes set to the input data
	 */
	private ProcessingFacility createFacility(String[] testData) {	
		logger.info("... creating facility ");
		
		ProcessingFacility testFacility = new ProcessingFacility();
		if (null != RepositoryService.getFacilityRepository().findByName(testData[2])) {
			logger.info("Found test facility {}", testFacility.getId());
			return testFacility = RepositoryService.getFacilityRepository().findByName(testData[2]);	
		}
		else{
			testFacility.setId(Long.parseLong(testData[0]));
			testFacility.setName(testData[2]);
			testFacility.setDescription(testData[3]);
			testFacility.setProcessingEngineUrl(testData[4]);
			testFacility.setStorageManagerUrl(testData[5]);
			
			testFacility = RepositoryService.getFacilityRepository().save(testFacility);
		
		}
		logger.info("Created test facility {}", testFacility.getId());
		return testFacility;
	}
	
	/**
	 * Create test facilities in the database
	 * 
	 * @return a list of facilities generated
	 */
	
	private List<ProcessingFacility> createTestFacilities() {
		logger.info("Creating test facilities");
		List<ProcessingFacility> testFacilities = new ArrayList<>();		
		logger.info("Creating test facility of length: "+  testFacilityData.length);

		for (int i = 0; i < testFacilityData.length; ++i) {
			logger.info("Creating test order: "+ i +" "+ testFacilityData[i][2]);

			testFacilities.add(createFacility(testFacilityData[i]));
		}
		return testFacilities;
	}
	
	/**
	 * Remove all (remaining) test facilities
	 * 
	 * @param testFacilities a list of test facilities to delete 
	 */
	private void deleteTestFacilities(List<ProcessingFacility> testFacilities) {
		for (ProcessingFacility testFacility: testFacilities) {
			RepositoryService.getFacilityRepository().delete(testFacility);
		}
	}
	
	
	
	/**
	 * Test method for { @link de.dlr.proseo.facmgr.rest.FacmgrControllerImpl.deleteFacilityById(Long))}.
	 * 
	 * Test: Delete a facility by ID
	 * Precondition: A facility in the database
	 */
	@Test
	public final void deleteFacilityById() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		logger.info ("Coming here");

		
		List<ProcessingFacility> testFacilities = transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public List<ProcessingFacility> doInTransaction(TransactionStatus status) {
				
				List<ProcessingFacility> createFacilities = new ArrayList<ProcessingFacility>();				
				for (int i = 0; i < testFacilityData.length; ++i) {
					ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(testFacilityData[i][2]);
					if (facility == null)
						createFacilities.add(createFacility(testFacilityData[i]));
					else
						createFacilities.add(facility);
				}
				return createFacilities;
			}
		});
		ProcessingFacility facilityToDelete = testFacilities.get(0);
		
		// Delete the first test order
		String testUrl = "http://localhost:" + this.port + FACILITY_BASE_URI + "/facilities/" + facilityToDelete.getId();
		logger.info("Testing URL {} / DELETE", testUrl);
		
		new TestRestTemplate(config.getUserName(), config.getUserPassword()).delete(testUrl);
		
		// Test that the order is gone
		ResponseEntity<RestFacility> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestFacility.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, entity.getStatusCode());
		
		// Clean up database
		transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				deleteTestFacilities(testFacilities);
				return null;
			}
		});
		logger.info("Test OK: Delete Order By ID");
	}
	

}
