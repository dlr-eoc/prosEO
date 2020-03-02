package de.dlr.proseo.facmgr.rest.model;

import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ProcessingFacility;



/**
 * @author Ranjitha Vignesh
 *
 */

public class FacmgrUtilTest {
	
	/* Test Missions */
	private static String[][] testFacilityData = {
			// id, version, name, desc,processingEngineUrl, storageMangerUrl
			{ "0", "0", "TestFacility 1", "Processing Facility 1", "https://www.prosEO-ProcFac1.de/kubernetes","https://www.prosEO-ProcFac1.de/proseo/storage-mgr/v1.0"},
			{ "11", "11", "TestFacility 2", "Processing Facility 2", "https://www.prosEO-ProcFac2.de/kubernetes","https://www.prosEO-ProcFac2.de/proseo/storage-mgr/v1.0"},
			{ "12", "12", "TestFacility 3", "Processing Facility 3", "https://www.prosEO-ProcFac3.de/kubernetes","https://www.prosEO-ProcFac3.de/proseo/storage-mgr/v1.0"}
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
		testFacility.setName(testData[3]);
		testFacility.setDescription(testData[4]);
		testFacility.setProcessingEngineUrl(testData[5]);
		testFacility.setStorageManagerUrl(testData[6]);

		
		logger.info("Created test mission {}", testFacility.getId());
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
		RestFacility restFacility = FacmgrUtil.toRestFacility(modelFacility);
		assertNull("Unexpected name for new mission: ",  restFacility.getName());
		assertNull("Unexpected code for new mission: ", restFacility.getDescription());
		logger.info("Test copy empty mission OK");
		
	}

}
