/**
 * ConfigurationMgrTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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

/**
 * Testing the service methods required to create, modify and delete
 * configurations in the prosEO database, and to query the database about such
 * configurations
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProcessorManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@AutoConfigureTestEntityManager
@Transactional
public class ConfigurationManagerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfigurationManagerTest.class);

	/** The configuration manager under test */
	@Autowired
	ConfigurationManager configurationMgr;

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
		createTestConfigurations();

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
		RepositoryService.getConfigurationRepository().deleteAll();
		RepositoryService.getConfiguredProcessorRepository().deleteAll();
		RepositoryService.getProcessorRepository().deleteAll();
		RepositoryService.getProcessorClassRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
		logger.trace("<<< Finished deleting test data in database");
	}

	/**
	 * Create test configurations in the database
	 */
	private static void createTestConfigurations() {
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

		ConfigurationInputFile configInputFile = new ConfigurationInputFile();
		configInputFile.setFileType(testStaticInputFile[0]);
		configInputFile.setFileNameType(testStaticInputFile[1]);
		configInputFile.getFileNames().add(testStaticInputFile[2]);
		configuration0.getStaticInputFiles().add(configInputFile);

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

		configuration1.getStaticInputFiles().add(configInputFile);
		configuration1.getConfiguredProcessors().add(RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(testMissionData[0], testConfiguredProcessors[1]));
		configuration1.getDockerRunParameters().put(testDockerRunParameter[0], testDockerRunParameter[1]);

		RepositoryService.getConfigurationRepository().save(configuration1);
	}

	/**
	 * Filling the database with some initial data for testing purposes
	 */
	private static void fillDatabase() {
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
	 * {@link de.dlr.proseo.procmgr.rest.ConfigurationMgr#countConfigurations(de.dlr.proseo.procmgr.rest.model.RestConfiguration)}.
	 */
	@Test
	public final void testCountConfigurations() {
		logger.trace(">>> testCountConfigurations()");

		String[] names = {testConfigurationData[0][1]};
		// Count configurations and assert success.
		assertEquals("Wrong configuration count.", "2", configurationMgr.countConfigurations("UTM", null, null, null, null, null));
		assertEquals("Wrong configuration count.", "1",
				configurationMgr.countConfigurations("UTM", null, names, null, null, null));
		assertEquals("Wrong configuration count.", "1",
				configurationMgr.countConfigurations("UTM", null, null, testConfigurationData[0][2], null, null));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfigurationMgr#createConfiguration(de.dlr.proseo.procmgr.rest.model.RestConfiguration)}.
	 */
	@Test
	public final void testCreateConfiguration() {
		logger.trace(">>> testCreateConfiguration()");
		// TODO implement method
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfigurationMgr#deleteConfigurationById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteConfigurationById() {
		logger.trace(">>> testDeleteConfigurationById()");

		// Get a test configuration to delete.
		Configuration testConfiguration = RepositoryService.getConfigurationRepository().findAll().get(0);

		// Remove related configured processor to avoid inconsistencies
		testConfiguration.getConfiguredProcessors().removeIf(cp -> true);

		// Delete configuration and assert success.
		RestConfiguration restConfiguration = ConfigurationUtil.toRestConfiguration(testConfiguration);
		configurationMgr.deleteConfigurationById(restConfiguration.getId());
		assertTrue("The configuration was not deleted.",
				RepositoryService.getConfigurationRepository().findById(restConfiguration.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfigurationMgr#getConfigurationById(java.lang.Long)}.
	 */
	@Test
	public final void testGetConfigurationById() {
		logger.trace(">>> testGetConfigurationById()");

		// Get a test configuration to retrieve.
		RestConfiguration testConfiguration = ConfigurationUtil
				.toRestConfiguration(RepositoryService.getConfigurationRepository().findAll().get(0));

		// Retrieve configuration and assert success.
		assertNotNull("No configuration was retrieved.",
				configurationMgr.getConfigurationById(testConfiguration.getId()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfigurationMgr#modifyConfiguration(java.lang.Long, de.dlr.proseo.procmgr.rest.model.RestConfiguration)}.
	 */
	@Test
	public final void testModifyConfiguration() {
		logger.trace(">>> testModifyConfiguration()");
		// TODO implement method
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfigurationMgr#getConfigurations(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetConfigurations() {
		logger.trace(">>> testGetConfigurations()");

		/*
		 * Using values from the test configuration data which was used to initialize
		 * the configuration repository means that the query must return at least one
		 * configuration. If no mission was specified, it is acquired from the security
		 * service. Not specifying additional parameters returns all configurations for
		 * the given mission.
		 */
		String[] names = {testConfigurationData[0][1]};
		assertTrue("More or less configurations retrieved than expected.",
				configurationMgr.getConfigurations(null, null, null, null, null, null, 0, 10, null).size() == 2);
		assertTrue("More or less configurations retrieved than expected.",
				configurationMgr.getConfigurations(testMissionData[0], null, null, null, null, null, 0, 100, null).size() == 2);
		assertTrue("More or less configurations retrieved than expected.", configurationMgr
				.getConfigurations(testMissionData[0], null, names, null, null, null, null, null, null).size() == 1);
		assertTrue("More or less configurations retrieved than expected.", configurationMgr
				.getConfigurations(testMissionData[0], null, null, testConfigurationData[0][2], null, null, null, null, null).size() == 1);
	}

}