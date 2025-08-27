/**
 * ConfigurationControllerTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

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
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.ConfigurationUtil;
import de.dlr.proseo.procmgr.rest.model.RestConfiguration;
import de.dlr.proseo.procmgr.rest.model.RestConfigurationInputFile;

/**
 * Testing ConfigurationControllerImpl.class.
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
public class ConfigurationControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfigurationControllerTest.class);

	/** The ConfigurationControllerImpl under test */
	@Autowired
	private ConfigurationControllerImpl cci;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
	private static String[][] testConfigurationData = {
			// mission code, processor name, configuration version
			{ "UTM", "KNMI L2", "2019-03-30" }, { "UTM", "KNMI L3", "2019-04-27" } };
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

		fillDatabase();

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
		// Nothing to do, test data will be deleted by automatic rollback of test transaction
	}

	/**
	 * Create test configurations in the database
	 */
	private void createTestConfigurations() {
		logger.trace("... creating test configurations in the database");

		// add first test configuration
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

		ConfigurationInputFile configInputFile0 = new ConfigurationInputFile();
		configInputFile0.setFileType(testStaticInputFile[0]);
		configInputFile0.setFileNameType(testStaticInputFile[1]);
		configInputFile0.getFileNames().add(testStaticInputFile[2]);
		logger.debug("... created ConfigurationInputFile with ID {}", configInputFile0.getId());
		configuration0.getStaticInputFiles().add(configInputFile0);

		configuration0.getConfiguredProcessors().add(RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(testMissionData[0], testConfiguredProcessors[0]));
		configuration0.getDockerRunParameters().put(testDockerRunParameter[0], testDockerRunParameter[1]);

		RepositoryService.getConfigurationRepository().save(configuration0);

		// add second test configuration
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

		RepositoryService.getConfigurationRepository().save(configuration1);

		ConfigurationInputFile configInputFile1 = new ConfigurationInputFile();
		configInputFile1.setFileType(testStaticInputFile[0]);
		configInputFile1.setFileNameType(testStaticInputFile[1]);
		configInputFile1.getFileNames().add(testStaticInputFile[2]);
		logger.debug("... created ConfigurationInputFile with ID {}", configInputFile1.getId());
		configuration1.getStaticInputFiles().add(configInputFile1);

		configuration1.getConfiguredProcessors().add(RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(testMissionData[0], testConfiguredProcessors[1]));
		configuration1.getDockerRunParameters().put(testDockerRunParameter[0], testDockerRunParameter[1]);

		RepositoryService.getConfigurationRepository().save(configuration1);
	}

	/**
	 * Filling the database with some initial data for testing purposes
	 */
	private void fillDatabase() {
		logger.trace("... creating testMission {}", testMissionData[0]);
		Mission testMission = new Mission();
		testMission.setCode(testMissionData[0]);
		testMission.setName(testMissionData[1]);
		testMission.getProcessingModes().add(testMissionData[2]);
		testMission.getFileClasses().add(testMissionData[3]);
		testMission.setProductFileTemplate(testMissionData[4]);
		testMission.setId(RepositoryService.getMissionRepository().save(testMission).getId());

		logger.debug("... adding processor classes");
		ProcessorClass processorClass0 = new ProcessorClass();
		processorClass0.setMission(testMission);
		processorClass0.setProcessorName(testConfigurationData[0][1]);
		processorClass0.setId(RepositoryService.getProcessorClassRepository().save(processorClass0).getId());

		ProcessorClass processorClass1 = new ProcessorClass();
		processorClass1.setMission(testMission);
		processorClass1.setProcessorName(testConfigurationData[1][1]);
		processorClass1.setId(RepositoryService.getProcessorClassRepository().save(processorClass1).getId());

		logger.debug("... adding processors");
		Processor processor0 = new Processor();
		processor0.setProcessorClass(processorClass0);
		processor0.setId(RepositoryService.getProcessorRepository().save(processor0).getId());

		Processor processor1 = new Processor();
		processor1.setProcessorClass(processorClass1);
		processor1.setId(RepositoryService.getProcessorRepository().save(processor1).getId());

		logger.debug("... adding configured processors");
		ConfiguredProcessor configProc0 = new ConfiguredProcessor();
		configProc0.setProcessor(processor0);
		configProc0.setIdentifier(testConfiguredProcessors[0]);
		configProc0.setUuid(UUID.randomUUID());
		configProc0.setId(RepositoryService.getConfiguredProcessorRepository().save(configProc0).getId());

		ConfiguredProcessor configProc1 = new ConfiguredProcessor();
		configProc1.setProcessor(processor1);
		configProc1.setIdentifier(testConfiguredProcessors[1]);
		configProc1.setUuid(UUID.randomUUID());
		configProc1.setId(RepositoryService.getConfiguredProcessorRepository().save(configProc1).getId());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.configurationmgr.rest.ConfigurationControllerImpl#createConfiguration(de.dlr.proseo.model.rest.model.RestConfiguration)}.
	 */
	@Test
	public final void testCreateConfiguration() {
		logger.trace(">>> testCreateConfiguration()");

		createTestConfigurations();

		// retrieve and delete the test configuration from the database
		RestConfiguration toBeCreated = ConfigurationUtil
				.toRestConfiguration(RepositoryService.getConfigurationRepository().findAll().get(0));
		RepositoryService.getConfigurationRepository().deleteById(toBeCreated.getId());

		toBeCreated.setId(null);
		for (RestConfigurationInputFile input : toBeCreated.getStaticInputFiles()) {
			input.setId(null);
		}

		// testing configuration creation with the configuration controller
		ResponseEntity<RestConfiguration> created = cci.createConfiguration(toBeCreated);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, created.getStatusCode());
		assertEquals("Error during configuration creation.", toBeCreated.getProcessorName(), created.getBody().getProcessorName());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.configurationmgr.rest.ConfigurationControllerImpl#countConfigurations(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testCountConfigurations() {
		logger.trace(">>> testCountConfigurations()");

		createTestConfigurations();

		// count all configurations from the database, as all were created with the same mission
		List<Configuration> expectedConfigurations = RepositoryService.getConfigurationRepository().findAll();

		// count all configurations with the same mission as the test
		// configurations
		// from the database via the configuration controller
		ResponseEntity<String> retrievedConfigurations = cci.countConfigurations(testMissionData[0], null, null, null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedConfigurations.getStatusCode());
		assertTrue("Wrong number of configurations retrieved.",
				Integer.toUnsignedString(expectedConfigurations.size()).equals(retrievedConfigurations.getBody()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.configurationmgr.rest.ConfigurationControllerImpl#getConfigurations(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testGetConfigurations() {
		logger.trace(">>> testGetConfigurations()");

		createTestConfigurations();

		// retrieve all configurations from the database, as all were created with the same mission
		List<Configuration> expectedConfigurations = RepositoryService.getConfigurationRepository().findAll();

		// retrieve all configurations with the same mission as the test configurations from the
		// database via the configuration controller
		ResponseEntity<List<RestConfiguration>> retrievedConfigurations = cci.getConfigurations(testMissionData[0], null, null,
				null, null, null, null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedConfigurations.getStatusCode());
		assertTrue("Wrong number of configurations retrieved.",
				expectedConfigurations.size() == retrievedConfigurations.getBody().size());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.configurationmgr.rest.ConfigurationControllerImpl#getConfigurationById(java.lang.Long)}.
	 */
	@Test
	public final void testGetConfigurationById() {
		logger.trace(">>> testGetConfigurationById()");

		createTestConfigurations();

		// retrieve a test configuration from the database
		Configuration expectedConfiguration = RepositoryService.getConfigurationRepository().findAll().get(0);

		// retrieve a configuration with the configuration controller by using the id from the test configuration
		ResponseEntity<RestConfiguration> retrievedConfiguration = cci.getConfigurationById(expectedConfiguration.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedConfiguration.getStatusCode());
		assertTrue("Wrong configuration retrieved.", expectedConfiguration.getProcessorClass().getProcessorName()
				.equals(retrievedConfiguration.getBody().getProcessorName()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.configurationmgr.rest.ConfigurationControllerImpl#deleteConfigurationById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteConfigurationById() {
		logger.trace(">>> testDeleteConfigurationById()");

		createTestConfigurations();

		// chose one configuration from the database for deletion
		Configuration toBeDeleted = RepositoryService.getConfigurationRepository().findAll().get(0);

		// remove related configured processor to avoid inconsistencies
		toBeDeleted.getConfiguredProcessors().removeIf(c -> true);

		// delete the chosen configuration via the configuration controller
		ResponseEntity<?> entity = cci.deleteConfigurationById(toBeDeleted.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, entity.getStatusCode());

		// assert that the configuration was deleted
		assertTrue("Configuration not deleted.",
				RepositoryService.getConfigurationRepository().findById(toBeDeleted.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.configurationmgr.rest.ConfigurationControllerImpl#modifyConfiguration(java.lang.Long, de.dlr.proseo.model.rest.model.RestConfiguration)}.
	 */
	@Test
	public final void testModifyConfiguration() {
		logger.trace(">>> testModifyConfiguration()");

		createTestConfigurations();

		Configuration inRepository = RepositoryService.getConfigurationRepository().findAll().get(0);
		RestConfiguration toBeModified = ConfigurationUtil.toRestConfiguration(inRepository);
		String previousConfigurationVersion = toBeModified.getConfigurationVersion();
		toBeModified.setConfigurationVersion("10.1");

		ResponseEntity<RestConfiguration> entity = cci.modifyConfiguration(toBeModified.getId(), toBeModified);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		assertTrue("Modification unsuccessfull", toBeModified.getVersion() + 1 == entity.getBody().getVersion());
		assertNotEquals("Modification unsuccessfull", previousConfigurationVersion, entity.getBody().getConfigurationVersion());
	}

}
