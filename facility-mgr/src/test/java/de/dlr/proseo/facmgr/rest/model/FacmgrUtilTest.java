/**
 * FacmgrUtilTest.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.facmgr.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import de.dlr.proseo.facmgr.rest.FacmgrControllerTest;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.enums.StorageType;

/**
 * Testing FacmgrUtil.java
 *
 * @author Ranjitha Vignesh
 */
public class FacmgrUtilTest {

	/** Test facility data */
	private static String[][] testFacilityData = {
			// id, version, name, description, processingEngineUrl, storageMangerUrl
			{ "0", "0", "TestFacility 1", "Processing Facility 1", "https://www.prosEO-ProcFac1.de/kubernetes",
					"https://www.prosEO-ProcFac1.de/proseo/storage-mgr/v1.0", "S3" },
			{ "11", "11", "TestFacility 2", "Processing Facility 2", "https://www.prosEO-ProcFac2.de/kubernetes",
					"https://www.prosEO-ProcFac2.de/proseo/storage-mgr/v1.0", "POSIX" },
			{ "12", "12", "TestFacility 3", "Processing Facility 3", "https://www.prosEO-ProcFac3.de/kubernetes",
					"https://www.prosEO-ProcFac3.de/proseo/storage-mgr/v1.0", "OTHER" } };

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(FacmgrControllerTest.class);

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

		logger.trace("Created test facility {}", testFacility.getId());
		return testFacility;
	}

	@Test
	void test() {
		// Create an empty facility
		ProcessingFacility modelFacility = new ProcessingFacility();
		RestProcessingFacility restFacility = FacmgrUtil.toRestFacility(modelFacility);
		assertNull(restFacility.getName(), "Unexpected name for new facility: ");
		assertNull(restFacility.getDescription(), "Unexpected description for new facility: ");
		assertNull(restFacility.getProcessingEngineUrl(), "Unexpected Processing ENgine Url for new facility: ");
		assertNull(restFacility.getStorageManagerUrl(), "Unexpected Storage Manager Url for new facility: ");
		assertNull(restFacility.getDefaultStorageType(), "Unexpected Default Storage Type for new facility: ");

		logger.trace("Test copy empty facility OK");

		// Copy a facility from model to REST
		modelFacility = createFacility(testFacilityData[0]);
		restFacility = FacmgrUtil.toRestFacility(modelFacility);
		assertEquals(modelFacility.getId(), restFacility.getId(), "Unexpected ID: ");
		assertEquals(modelFacility.getDescription(), restFacility.getDescription(), "Unexpected facility description: ");
		assertEquals(modelFacility.getName(), restFacility.getName(), "Unexpected facility name: ");
		assertEquals(modelFacility.getProcessingEngineUrl(), restFacility.getProcessingEngineUrl(),
				"Unexpected Processing ENgine Url: ");
		assertEquals(modelFacility.getStorageManagerUrl(), restFacility.getStorageManagerUrl(), "Unexpected Storage Manager Url: ");
		assertEquals(modelFacility.getDefaultStorageType().toString(), restFacility.getDefaultStorageType(),
				"Unexpected Default Storage Type: ");

		logger.trace("Test copy model to REST OK");

		// Copy a facility from REST to model
		ProcessingFacility copiedModelFacility = FacmgrUtil.toModelFacility(restFacility);
		assertEquals(modelFacility.getDescription(), copiedModelFacility.getDescription(), "Description not preserved: ");
		assertEquals(modelFacility.getName(), copiedModelFacility.getName(), "Name not preserved: ");
		assertEquals(modelFacility.getProcessingEngineUrl(), copiedModelFacility.getProcessingEngineUrl(),
				"Processing Engine Url not preserved: ");
		assertEquals(modelFacility.getStorageManagerUrl(), copiedModelFacility.getStorageManagerUrl(),
				"Storage Manager Url not preserved: ");
		assertEquals(modelFacility.getDefaultStorageType(), copiedModelFacility.getDefaultStorageType(),
				"Unexpected Default Storage Type: ");

		logger.trace("Test copy REST to model OK");
	}

}