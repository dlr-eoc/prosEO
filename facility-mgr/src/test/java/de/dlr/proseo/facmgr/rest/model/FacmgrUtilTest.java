package de.dlr.proseo.facmgr.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.enums.StorageType;



/**
 * @author Ranjitha Vignesh
 *
 */

public class FacmgrUtilTest {
	
	/* Test facility */
	private static String[][] testFacilityData = {
			// id, version, name, desc,processingEngineUrl, storageMangerUrl
			{ "0", "0", "TestFacility 1", "Processing Facility 1", "https://www.prosEO-ProcFac1.de/kubernetes","https://www.prosEO-ProcFac1.de/proseo/storage-mgr/v1.0", "S3"},
			{ "11", "11", "TestFacility 2", "Processing Facility 2", "https://www.prosEO-ProcFac2.de/kubernetes","https://www.prosEO-ProcFac2.de/proseo/storage-mgr/v1.0", "POSIX"},
			{ "12", "12", "TestFacility 3", "Processing Facility 3", "https://www.prosEO-ProcFac3.de/kubernetes","https://www.prosEO-ProcFac3.de/proseo/storage-mgr/v1.0", "OTHER"}
		};

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FacmgrUtilTest.class);
	
	/**
	 * Create a ProcessingFacility from a data array
	 * 
	 * @param testData an array of Strings representing the processing Facility to create
	 * @return a Processing Facility with its attributes set to the input data
	 */
	private de.dlr.proseo.model.ProcessingFacility createFacility(String[] testData) {
		de.dlr.proseo.model.ProcessingFacility testFacility = new de.dlr.proseo.model.ProcessingFacility();

		testFacility.setId(Long.parseLong(testData[0]));
		testFacility.setName(testData[2]);
		testFacility.setDescription(testData[3]);
		testFacility.setProcessingEngineUrl(testData[4]);
		testFacility.setStorageManagerUrl(testData[5]);
		testFacility.setDefaultStorageType(StorageType.valueOf(testData[6]));
		
		logger.info("Created test facility {}", testFacility.getId());
		return testFacility;
	}
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public final void test() {
		// Create an empty facility
		ProcessingFacility modelFacility = new ProcessingFacility();
		RestProcessingFacility restFacility = FacmgrUtil.toRestFacility(modelFacility);
		assertNull("Unexpected name for new facility: ",  restFacility.getName());
		assertNull("Unexpected description for new facility: ", restFacility.getDescription());
		assertNull("Unexpected Processing ENgine Url for new facility: ", restFacility.getProcessingEngineUrl());
		assertNull("Unexpected Storage Manager Url for new facility: ", restFacility.getStorageManagerUrl());
		assertNull("Unexpected Default Storage Type for new facility: ", restFacility.getDefaultStorageType());

		logger.info("Test copy empty facility OK");
		
		
		// Copy a facility from model to REST
		modelFacility = createFacility(testFacilityData[0]);
		restFacility = FacmgrUtil.toRestFacility(modelFacility);
		assertEquals("Unexpected ID: ", modelFacility.getId(), restFacility.getId().longValue());
		assertEquals("Unexpected facility description: ", modelFacility.getDescription(),restFacility.getDescription());
		assertEquals("Unexpected facility name: ", modelFacility.getName(),restFacility.getName());
		assertEquals("Unexpected  Processing ENgine Url: ", modelFacility.getProcessingEngineUrl(),restFacility.getProcessingEngineUrl());
		assertEquals("Unexpected  Storage Manager Url: ", modelFacility.getStorageManagerUrl(),restFacility.getStorageManagerUrl());
		assertEquals("Unexpected  Default Storage Type: ", modelFacility.getDefaultStorageType().toString(),restFacility.getDefaultStorageType());

		logger.info("Test copy model to REST OK");
		
		// Copy a facility from REST to model
		ProcessingFacility copiedModelFacility = FacmgrUtil.toModelFacility(restFacility);
		assertEquals("Description not preserved: ", modelFacility.getDescription(), copiedModelFacility.getDescription());
		assertEquals("Name not preserved: ", modelFacility.getName(), copiedModelFacility.getName());
		assertEquals("Processing ENgine Url not preserved: ", modelFacility.getProcessingEngineUrl(),copiedModelFacility.getProcessingEngineUrl());
		assertEquals("Storage Manager Url not preserved: ", modelFacility.getStorageManagerUrl(),copiedModelFacility.getStorageManagerUrl());
		assertEquals("Unexpected  Default Storage Type: ", modelFacility.getDefaultStorageType(),copiedModelFacility.getDefaultStorageType());

		logger.info("Test copy REST to model OK");

				
		
	}

}
