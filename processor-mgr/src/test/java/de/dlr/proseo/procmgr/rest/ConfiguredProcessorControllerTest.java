/**
 * ConfiguredProcessorControllerTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfigurationFile;
import de.dlr.proseo.model.ConfigurationInputFile;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.ConfiguredProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.RestConfiguredProcessor;

/**
 * Testing ConfiguredProcessorControllerImpl.class.
 *
 * TODO test invalid REST requests
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProcessorManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@AutoConfigureTestEntityManager
@Transactional
public class ConfiguredProcessorControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfiguredProcessorControllerTest.class);

	/** The ConfiguredProcessorControllerImpl under test */
	@Autowired
	private ConfiguredProcessorControllerImpl cci;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
	private static String[] testSpacecraftData =
			// code, name
			{ "S_TDX1", "Tandom-X" };
	private static String[][] testConfigurationData = {
			// mission code, processor name, configuration version, processor version
			{ "UTM", "KNMI L2", "2019-03-30", "1.0" }, { "UTM", "KNMI L3", "2019-04-27", "3.7" } };
	private static String[][] testDynProcParameters = {
			// key, value
			{ "logging.root", "notice" }, { "logging.root", "error" } };
	private static String[][] testConfigurationFiles = {
			// fileVersion, fileName
			{ "1.0", "/mnt/sw/MPC/config/UTM_OPER_CFG_MPC_L2_00000000T000000_99999999T999999_20190215T172140.xml" },
			{ "1.0", "/mnt/sw/MPC/config/UTM_OPER_CFG_MPC_L3_00000000T000000_99999999T999999_20190215T172140.xml" } };
	private static String[] testStaticInputFile = {
			// type, name type, file name
			"directory.lib", "DIRECTORY", "/mnt/sw/IPF_KNMI_L2/config/lib" };
	private static String[] testDockerRunParameter = {
			// key, value
			"-v", "/my/source/directory:/container/target/directory" };
	private static String[] testConfiguredProcessors = {
			// identifier
			"KNMI L2 01.03.02 2019-03-30", "KNMI L3 01.03.02 2019-04-27" };

	/**
	 *
	 * Create a test mission, a test spacecraft and test orders in the database.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.trace(">>> Starting to create test data in the database");

		createMissionAndSpacecraft(testMissionData, testSpacecraftData);
		fillDatabase(RepositoryService.getMissionRepository().findByCode(testMissionData[0]));

		logger.trace("<<< Finished creating test data in database");
	}

	/**
	 *
	 * Deleting test data from the database
	 *
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		logger.trace(">>> Starting to delete test data in database");
		RepositoryService.getConfiguredProcessorRepository().deleteAll();
		RepositoryService.getConfigurationRepository().deleteAll();
		RepositoryService.getProcessorRepository().deleteAll();
		RepositoryService.getProcessorClassRepository().deleteAll();
		RepositoryService.getSpacecraftRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
		logger.trace("<<< Finished deleting test data in database");
	}

	/**
	 * Create a test mission and a test spacecraft in the database
	 *
	 * @param missionData    The data from which to create the mission
	 * @param spacecraftData The data from which to create the spacecraft
	 */
	private static void createMissionAndSpacecraft(String[] missionData, String[] spacecraftData) {
		if (null != RepositoryService.getMissionRepository().findByCode(missionData[2])) {
			return;
		}

		Mission testMission = new Mission();
		Spacecraft testSpacecraft = new Spacecraft();

		logger.trace("... creating mission {}", missionData[0]);

		// adding mission attributes
		testMission.setCode(missionData[0]);
		testMission.setName(missionData[1]);
		testMission.getProcessingModes().add(missionData[2]);
		testMission.getFileClasses().add(missionData[3]);
		testMission.setProductFileTemplate(missionData[4]);

		// saving mission in the database
		testMission = RepositoryService.getMissionRepository().save(testMission);

		logger.trace("... creating spacecraft {}", spacecraftData[1]);

		// adding spacecraft attributes
		testSpacecraft.setMission(testMission);
		testSpacecraft.setCode(spacecraftData[0]);
		testSpacecraft.setName(spacecraftData[1]);

		// saving spacecraft in the database
		testSpacecraft = RepositoryService.getSpacecraftRepository().save(testSpacecraft);

		// assigning the spacecraft to the mission
		testMission.getSpacecrafts().clear();
		testMission.getSpacecrafts().add(testSpacecraft);

		RepositoryService.getMissionRepository().save(testMission);
	}

	/**
	 * Filling the database with some initial data for testing purposes
	 *
	 * @param mission the mission to be referenced by the data filled in the
	 *                database
	 */
	private static void fillDatabase(Mission mission) {
		logger.debug("... adding processor classes");
		ProcessorClass processorClass0 = new ProcessorClass();
		processorClass0.setMission(mission);
		processorClass0.setProcessorName(testConfigurationData[0][1]);
		processorClass0.setId(RepositoryService.getProcessorClassRepository().save(processorClass0).getId());

		ProcessorClass processorClass1 = new ProcessorClass();
		processorClass1.setMission(mission);
		processorClass1.setProcessorName(testConfigurationData[1][1]);
		processorClass1.setId(RepositoryService.getProcessorClassRepository().save(processorClass1).getId());

		logger.debug("... adding processors");
		Processor processor0 = new Processor();
		processor0.setProcessorClass(processorClass0);
		processor0.setProcessorVersion(testConfigurationData[0][3]);
		processor0.setId(RepositoryService.getProcessorRepository().save(processor0).getId());
			
		Processor processor1 = new Processor();
		processor1.setProcessorClass(processorClass1);
		processor1.setProcessorVersion(testConfigurationData[1][3]);
		processor1.setId(RepositoryService.getProcessorRepository().save(processor1).getId());
		
		logger.debug("... adding configurations");
		Configuration configuration0 = new Configuration();

		configuration0.setProcessorClass(RepositoryService.getProcessorClassRepository()
				.findByMissionCodeAndProcessorName(testMissionData[0], testConfigurationData[0][1]));
		configuration0.setConfigurationVersion(testConfigurationData[0][2]);
		configuration0.getDynProcParameters().put(testDynProcParameters[0][0],
				new Parameter().init(ParameterType.STRING, testDynProcParameters[0][1]));

		ConfigurationFile configFile0 = new ConfigurationFile();
		configFile0.setFileName(testConfigurationFiles[0][1]);
		configFile0.setFileVersion(testConfigurationFiles[0][0]);
		configuration0.getConfigurationFiles().add(configFile0);

		ConfigurationInputFile configInputFile = new ConfigurationInputFile();
		configInputFile.setFileType(testStaticInputFile[0]);
		configInputFile.setFileNameType(testStaticInputFile[1]);
		configInputFile.getFileNames().add(testStaticInputFile[2]);
		configuration0.getStaticInputFiles().add(configInputFile);

		configuration0.getConfiguredProcessors().add(RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(testMissionData[0], testConfiguredProcessors[0]));
		configuration0.getDockerRunParameters().put(testDockerRunParameter[0], testDockerRunParameter[1]);

		configuration0.setId(RepositoryService.getConfigurationRepository().save(configuration0).getId());

		Configuration configuration1 = new Configuration();

		configuration1.setProcessorClass(RepositoryService.getProcessorClassRepository()
				.findByMissionCodeAndProcessorName(testMissionData[0], testConfigurationData[1][1]));
		configuration1.setConfigurationVersion(testConfigurationData[1][2]);
		configuration1.getDynProcParameters().put(testDynProcParameters[1][1],
				new Parameter().init(ParameterType.STRING, testDynProcParameters[1][1]));

		ConfigurationFile configFile1 = new ConfigurationFile();
		configFile1.setFileName(testConfigurationFiles[1][1]);
		configFile1.setFileVersion(testConfigurationFiles[1][0]);
		configuration1.getConfigurationFiles().add(configFile1);

		configuration1.getStaticInputFiles().add(configInputFile);
		configuration1.getConfiguredProcessors().add(RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(testMissionData[0], testConfiguredProcessors[1]));
		configuration1.getDockerRunParameters().put(testDockerRunParameter[0], testDockerRunParameter[1]);

		configuration1.setId(RepositoryService.getConfigurationRepository().save(configuration1).getId());

		logger.debug("... adding configured processors");
		ConfiguredProcessor configProc0 = new ConfiguredProcessor();
		configProc0.setProcessor(processor0);
		configProc0.setConfiguration(configuration0);
		configProc0.setIdentifier(testConfiguredProcessors[0]);
		configProc0.setId(RepositoryService.getConfiguredProcessorRepository().save(configProc0).getId());

		ConfiguredProcessor configProc1 = new ConfiguredProcessor();
		configProc1.setProcessor(processor1);
		configProc1.setConfiguration(configuration1);
		configProc1.setIdentifier(testConfiguredProcessors[1]);
		configProc1.setId(RepositoryService.getConfiguredProcessorRepository().save(configProc1).getId());

	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.rest.procmgrConfiguredProcessorControllerImpl#createConfiguredProcessor(de.dlr.proseo.model.rest.model.RestConfiguredProcessor)}.
	 */
	@Test
	public final void testCreateConfiguredProcessor() {
		logger.trace(">>> testCreateConfiguredProcessor()");

		// retrieve and delete the test configuredProcessor from the database
		RestConfiguredProcessor toBeCreated = ConfiguredProcessorUtil
				.toRestConfiguredProcessor(RepositoryService.getConfiguredProcessorRepository().findAll().get(0));
		RepositoryService.getConfiguredProcessorRepository().deleteById(toBeCreated.getId());

		// testing configuredProcessor creation with the configuredProcessor controller
		ResponseEntity<RestConfiguredProcessor> created = cci.createConfiguredProcessor(toBeCreated);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, created.getStatusCode());
		assertEquals("Error during configuredProcessor creation.", toBeCreated.getProcessorName(),
				created.getBody().getProcessorName());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.rest.procmgrConfiguredProcessorControllerImpl#countConfiguredProcessors(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testCountConfiguredProcessors() {
		logger.trace(">>> testCountConfiguredProcessors()");

		// count all configuredProcessors from the database, as all were created with
		// the
		// same mission
		List<ConfiguredProcessor> expectedConfiguredProcessors = RepositoryService.getConfiguredProcessorRepository()
				.findAll();

		// count all configuredProcessors with the same mission as the test
		// configuredProcessors
		// from the database via the configuredProcessor controller
		ResponseEntity<String> retrievedConfiguredProcessors = cci.countConfiguredProcessors(testMissionData[0], null,
				null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedConfiguredProcessors.getStatusCode());
		assertTrue("Wrong number of configuredProcessors retrieved.", Integer
				.toUnsignedString(expectedConfiguredProcessors.size()).equals(retrievedConfiguredProcessors.getBody()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.rest.procmgrConfiguredProcessorControllerImpl#getConfiguredProcessors(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testGetConfiguredProcessors() {
		logger.trace(">>> testGetConfiguredProcessors()");

		// retrieve all configuredProcessors from the database, as all were created with
		// the
		// same
		// mission
		List<ConfiguredProcessor> expectedConfiguredProcessors = RepositoryService.getConfiguredProcessorRepository()
				.findAll();

		// retrieve all configuredProcessors with the same mission as the test
		// configuredProcessors
		// from the
		// database via the configuredProcessor controller
		ResponseEntity<List<RestConfiguredProcessor>> retrievedConfiguredProcessors = cci
				.getConfiguredProcessors(testMissionData[0], null, null, null, null, null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedConfiguredProcessors.getStatusCode());
		assertTrue("Wrong number of configuredProcessors retrieved.",
				expectedConfiguredProcessors.size() == retrievedConfiguredProcessors.getBody().size());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.rest.procmgrConfiguredProcessorControllerImpl#getConfiguredProcessorById(java.lang.Long)}.
	 */
	@Test
	public final void testGetConfiguredProcessorById() {
		logger.trace(">>> testGetConfiguredProcessorById()");

		// retrieve a test configuredProcessor from the database
		ConfiguredProcessor expectedConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().findAll()
				.get(0);

		// retrieve a configuredProcessor with the configuredProcessor controller by
		// using the id
		// from the
		// test configuredProcessor
		ResponseEntity<RestConfiguredProcessor> retrievedConfiguredProcessor = cci
				.getConfiguredProcessorById(expectedConfiguredProcessor.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedConfiguredProcessor.getStatusCode());
		assertTrue("Wrong configuredProcessor retrieved.",
				expectedConfiguredProcessor.getProcessor().getProcessorClass().getProcessorName()
						.equals(retrievedConfiguredProcessor.getBody().getProcessorName()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.rest.procmgrConfiguredProcessorControllerImpl#deleteConfiguredProcessorById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteConfiguredProcessorById() {
		logger.trace(">>> testDeleteConfiguredProcessorById()");

		// chose one configuredProcessor from the database for deletion
		ConfiguredProcessor toBeDeleted = RepositoryService.getConfiguredProcessorRepository().findAll().get(0);

		// delete the chosen configuredProcessor via the configuredProcessor controller
		ResponseEntity<?> entity = cci.deleteConfiguredProcessorById(toBeDeleted.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, entity.getStatusCode());

		// assert that the configuredProcessor was deleted
		assertTrue("ConfiguredProcessor not deleted.",
				RepositoryService.getConfiguredProcessorRepository().findById(toBeDeleted.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.rest.procmgrConfiguredProcessorControllerImpl#modifyConfiguredProcessor(java.lang.Long, de.dlr.proseo.model.rest.model.RestConfiguredProcessor)}.
	 */
	@Test
	public final void testModifyConfiguredProcessor() {
		logger.trace(">>> testModifyConfiguredProcessor()");

		ConfiguredProcessor inRepository = RepositoryService.getConfiguredProcessorRepository()
				.findAll().get(0);
		RestConfiguredProcessor toBeModified = ConfiguredProcessorUtil.toRestConfiguredProcessor(inRepository);
		String previousIdentifier = toBeModified.getIdentifier();
		toBeModified.setIdentifier("newIdentifier");

		ResponseEntity<RestConfiguredProcessor> entity = cci.modifyConfiguredProcessor(toBeModified.getId(),
				toBeModified);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		assertTrue("Modification unsuccessfull", toBeModified.getVersion() + 1 == entity.getBody().getVersion());
		assertNotEquals("Modification unsuccessfull", previousIdentifier, entity.getBody().getIdentifier());
	}

}
