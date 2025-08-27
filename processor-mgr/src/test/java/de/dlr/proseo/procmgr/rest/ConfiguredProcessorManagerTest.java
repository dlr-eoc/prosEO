/**
 * ConfiguredProcessorManagerTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import de.dlr.proseo.procmgr.rest.model.ConfiguredProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.RestConfiguredProcessor;

/**
 * Testing the service methods required to create, modify and delete configured
 * processors in the prosEO database, and to query the database about such
 * configured processors
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProcessorManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@AutoConfigureTestEntityManager
@Transactional
public class ConfiguredProcessorManagerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfiguredProcessorManagerTest.class);

	/** The configuration manager under test */
	@Autowired
	ConfiguredProcessorManager configuredProcessorMgr;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
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
	}

	/**
	 *
	 * Deleting test data from the database
	 *
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Filling the database with some initial data for testing purposes
	 *
	 * @param mission the mission to be referenced by the data filled in the
	 *                database
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

		ConfigurationInputFile configInputFile0 = new ConfigurationInputFile();
		configInputFile0.setFileType(testStaticInputFile[0]);
		configInputFile0.setFileNameType(testStaticInputFile[1]);
		configInputFile0.getFileNames().add(testStaticInputFile[2]);
		configuration0.getStaticInputFiles().add(configInputFile0);

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

		ConfigurationInputFile configInputFile1 = new ConfigurationInputFile();
		configInputFile1.setFileType(testStaticInputFile[0]);
		configInputFile1.setFileNameType(testStaticInputFile[1]);
		configInputFile1.getFileNames().add(testStaticInputFile[2]);
		configuration1.getStaticInputFiles().add(configInputFile1);
		
		configuration1.getConfiguredProcessors().add(RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(testMissionData[0], testConfiguredProcessors[1]));
		configuration1.getDockerRunParameters().put(testDockerRunParameter[0], testDockerRunParameter[1]);

		configuration1.setId(RepositoryService.getConfigurationRepository().save(configuration1).getId());

		logger.debug("... adding configured processors");
		ConfiguredProcessor configProc0 = new ConfiguredProcessor();
		configProc0.setProcessor(processor0);
		configProc0.setConfiguration(configuration0);
		configProc0.setIdentifier(testConfiguredProcessors[0]);
		configProc0.setUuid(UUID.randomUUID());
		configProc0.setId(RepositoryService.getConfiguredProcessorRepository().save(configProc0).getId());

		ConfiguredProcessor configProc1 = new ConfiguredProcessor();
		configProc1.setProcessor(processor1);
		configProc1.setConfiguration(configuration1);
		configProc1.setIdentifier(testConfiguredProcessors[1]);
		configProc1.setUuid(UUID.randomUUID());
		configProc1.setId(RepositoryService.getConfiguredProcessorRepository().save(configProc1).getId());

	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfiguredProcessorMgr#countConfiguredProcessors(de.dlr.proseo.procmgr.rest.model.RestConfiguredProcessor)}.
	 */
	@Test
	public final void testCountConfiguredProcessors() {
		logger.trace(">>> testCountConfiguredProcessors()");

		fillDatabase();

		// Count configuredProcessors and assert success.
		assertEquals("Wrong configuredProcessor count.", "2",
				configuredProcessorMgr.countConfiguredProcessors("UTM", null, null, null, null, null, null));
		assertEquals("Wrong configuredProcessor count.", "0",
				configuredProcessorMgr.countConfiguredProcessors("UTM", null, testConfigurationData[0][1], null, null, null, null));
		assertEquals("Wrong configuredProcessor count.", "1",
				configuredProcessorMgr.countConfiguredProcessors("UTM", null, null, null, null, testConfigurationData[0][2], null));
		assertEquals("Wrong configuredProcessor count.", "1",
				configuredProcessorMgr.countConfiguredProcessors("UTM", null, null, null, testConfigurationData[0][3], null, null));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfiguredProcessorMgr#createConfiguredProcessor(de.dlr.proseo.procmgr.rest.model.RestConfiguredProcessor)}.
	 */
	@Test
	public final void testCreateConfiguredProcessor() {
		logger.trace(">>> testCreateConfiguredProcessor()");
		
		fillDatabase();

		// TODO implement method
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfiguredProcessorMgr#deleteConfiguredProcessorById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteConfiguredProcessorById() {
		logger.trace(">>> testDeleteConfiguredProcessorById()");

		fillDatabase();

		// Get a test configuredProcessor to delete.
		ConfiguredProcessor testConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().findAll()
				.get(0);

		// Delete configuredProcessor and assert success.
		RestConfiguredProcessor restConfiguredProcessor = ConfiguredProcessorUtil
				.toRestConfiguredProcessor(testConfiguredProcessor);
		configuredProcessorMgr.deleteConfiguredProcessorById(restConfiguredProcessor.getId());
		assertTrue("The configuredProcessor was not deleted.", RepositoryService.getConfiguredProcessorRepository()
				.findById(restConfiguredProcessor.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfiguredProcessorMgr#getConfiguredProcessorById(java.lang.Long)}.
	 */
	@Test
	public final void testGetConfiguredProcessorById() {
		logger.trace(">>> testGetConfiguredProcessorById()");

		fillDatabase();

		// Get a test configuredProcessor to retrieve.
		RestConfiguredProcessor testConfiguredProcessor = ConfiguredProcessorUtil
				.toRestConfiguredProcessor(RepositoryService.getConfiguredProcessorRepository().findAll().get(0));

		// Retrieve configuredProcessor and assert success.
		assertNotNull("No configuredProcessor was retrieved.",
				configuredProcessorMgr.getConfiguredProcessorById(testConfiguredProcessor.getId()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfiguredProcessorMgr#modifyConfiguredProcessor(java.lang.Long, de.dlr.proseo.procmgr.rest.model.RestConfiguredProcessor)}.
	 */
	@Test
	public final void testModifyConfiguredProcessor() {
		logger.trace(">>> testModifyConfiguredProcessor()");
		
		fillDatabase();

		// TODO implement method
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ConfiguredProcessorMgr#getConfiguredProcessors(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetConfiguredProcessors() {
		logger.trace(">>> testGetConfiguredProcessors()");

		fillDatabase();

		/*
		 * Using values from the test configuredProcessor data which was used to
		 * initialize the configuredProcessor repository means that the query must
		 * return at least one configuredProcessor. If no mission was specified, it is
		 * acquired from the security service. Not specifying additional parameters
		 * returns all configuredProcessors for the given mission.
		 */
		
		assertTrue("More or less configuredProcessors retrieved than expected.",
				configuredProcessorMgr.getConfiguredProcessors(null, null, null, null, null, null, null, 0, 10, null).size() == 2);
		assertTrue("More or less configuredProcessors retrieved than expected.", configuredProcessorMgr
				.getConfiguredProcessors(testMissionData[0], null, null, null, null, null, null, 0, 100, null).size() == 2);
//		assertTrue("More or less configuredProcessors retrieved than expected.",
//				configuredProcessorMgr.getConfiguredProcessors(testMissionData[0], null, testConfigurationData[0][1],
//						null, null, null, null, null, null, null).size() == 0);
		assertTrue("More or less configuredProcessors retrieved than expected.",
				configuredProcessorMgr.getConfiguredProcessors(testMissionData[0], null, null,
						null, testConfigurationData[0][3], null, null, null, null, null).size() == 1);
		assertTrue("More or less configuredProcessors retrieved than expected.",
				configuredProcessorMgr.getConfiguredProcessors(testMissionData[0], null, null, null, null,
						testConfigurationData[0][2], null, null, null, null).size() == 1);
	}

}