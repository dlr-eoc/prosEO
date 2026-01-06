/**
 * OrderTriggerRepositoryTest.java
 * 
 * (c) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.CalendarOrderTrigger;
import de.dlr.proseo.model.DataDrivenOrderTrigger;
import de.dlr.proseo.model.DatatakeOrderTrigger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.OrbitOrderTrigger;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.TimeIntervalOrderTrigger;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for all *OrderTriggerRepository classes
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class OrderTriggerRepositoryTest {

	private static final String TEST_CODE_1 = "$ABC$";
	private static final String TEST_CODE_2 = "$DEF$";
	private static final String TEST_PRODUCT_CLASS_1 = "$PC1$";
	private static final String TEST_PRODUCT_CLASS_2 = "$PC2$";
	private static final String TEST_PRODUCT_CLASS_3 = "$PC3$";
	private static final String TEST_WORKFLOW_1 = "$WF1$";
	private static final String TEST_WORKFLOW_2 = "$WF2$";
	private static final String TEST_WORKFLOW_3 = "$WF3$";
	private static final String TEST_TRIGGER_1 = "$TR1$";
	private static final String TEST_TRIGGER_2 = "$TR2$";
	private static final String TEST_TRIGGER_3 = "$TR3$";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderTriggerRepositoryTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test the additional repository methods
	 */
	@Test
	public final void test() {
		Mission mission1 = new Mission();
		mission1.setCode(TEST_CODE_1);
		mission1 = RepositoryService.getMissionRepository().save(mission1);
		
		Mission mission2 = new Mission();
		mission2.setCode(TEST_CODE_2);
		mission2 = RepositoryService.getMissionRepository().save(mission2);
		
		/*
		 * Test DataDrivenOrderTriggerRepository
		 */
		ProductClass productClass1 = new ProductClass();
		productClass1.setMission(mission1);
		productClass1.setProductType(TEST_PRODUCT_CLASS_1);
		productClass1 = RepositoryService.getProductClassRepository().save(productClass1);
		
		ProductClass productClass2 = new ProductClass();
		productClass2.setMission(mission2);
		productClass2.setProductType(TEST_PRODUCT_CLASS_2);
		productClass2 = RepositoryService.getProductClassRepository().save(productClass2);
		
		ProductClass productClass3 = new ProductClass();
		productClass3.setMission(mission1);
		productClass3.setProductType(TEST_PRODUCT_CLASS_3);
		productClass3 = RepositoryService.getProductClassRepository().save(productClass3);
		
		Workflow workflow1 = new Workflow();
		workflow1.setMission(mission1);
		workflow1.setName(TEST_WORKFLOW_1);
		workflow1.setUuid(UUID.randomUUID());
		workflow1.setWorkflowVersion("1");
		workflow1.setInputProductClass(productClass1);
		workflow1 = RepositoryService.getWorkflowRepository().save(workflow1);
		
		Workflow workflow2 = new Workflow();
		workflow2.setMission(mission2);
		workflow2.setName(TEST_WORKFLOW_2);
		workflow2.setUuid(UUID.randomUUID());
		workflow2.setWorkflowVersion("1");
		workflow2.setInputProductClass(productClass2);
		workflow2 = RepositoryService.getWorkflowRepository().save(workflow2);
		
		Workflow workflow3 = new Workflow();
		workflow3.setMission(mission1);
		workflow3.setName(TEST_WORKFLOW_3);
		workflow3.setUuid(UUID.randomUUID());
		workflow3.setWorkflowVersion("1");
		workflow3.setInputProductClass(productClass3);
		workflow3 = RepositoryService.getWorkflowRepository().save(workflow3);
		
		// Test save
		DataDrivenOrderTrigger trigger1 = new DataDrivenOrderTrigger();
		trigger1.setMission(mission1);
		trigger1.setName(TEST_TRIGGER_1);
		trigger1.setWorkflow(workflow1);
		trigger1 = RepositoryService.getDataDrivenOrderTriggerRepository().save(trigger1);
		assertNotNull("DataDrivenOrderTrigger 1 not created correctly", trigger1);
		
		DataDrivenOrderTrigger trigger2 = new DataDrivenOrderTrigger();
		trigger2.setMission(mission2);
		trigger2.setName(TEST_TRIGGER_2);
		trigger2.setWorkflow(workflow2);
		trigger2 = RepositoryService.getDataDrivenOrderTriggerRepository().save(trigger2);
		assertNotNull("DataDrivenOrderTrigger 2 not created correctly", trigger2);
		
		DataDrivenOrderTrigger trigger3 = new DataDrivenOrderTrigger();
		trigger3.setMission(mission1);
		trigger3.setName(TEST_TRIGGER_3);
		trigger3.setWorkflow(workflow3);
		trigger3 = RepositoryService.getDataDrivenOrderTriggerRepository().save(trigger3);
		assertNotNull("DataDrivenOrderTrigger 3 not created correctly", trigger3);

		logger.info("OK: Test DataDrivenOrderTriggerRepository::save");
		
		// Test findAll
		List<DataDrivenOrderTrigger> ddTriggerList = RepositoryService.getDataDrivenOrderTriggerRepository().findAll();
		assertEquals("Wrong number of triggers returned", 3, ddTriggerList.size());
		
		logger.info("OK: Test DataDrivenOrderTriggerRepository::findAll");
		
		// Test findByMissionCode
		ddTriggerList = RepositoryService.getDataDrivenOrderTriggerRepository().findByMissionCode(TEST_CODE_1);
		assertEquals("Wrong number of triggers returned", 2, ddTriggerList.size());
		
		logger.info("OK: Test DataDrivenOrderTriggerRepository::findByMissionCode");
		
		// Test findByMissionCodeAndName
		DataDrivenOrderTrigger ddTrigger = RepositoryService.getDataDrivenOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_1);
		assertNotNull("Trigger 1 (" + TEST_CODE_1 + "/" + TEST_TRIGGER_1 + ") not found", ddTrigger);
		
		logger.info("OK: Test DataDrivenOrderTriggerRepository::findByMissionCodeAndName");
		
		// Test findByMissionCodeAndProductClass
		ddTriggerList = RepositoryService.getDataDrivenOrderTriggerRepository()
				.findByMissionCodeAndProductClass(TEST_CODE_1, productClass3);
		assertEquals("Wrong number of triggers returned", 1, ddTriggerList.size());
		
		logger.info("OK: Test DataDrivenOrderTriggerRepository::findByMissionCodeAndProductClass");
		
		// Test delete
		RepositoryService.getDataDrivenOrderTriggerRepository().delete(trigger3);
		ddTrigger = RepositoryService.getDataDrivenOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNull("DataDrivenOrderTrigger 3 not deleted", ddTrigger);
		
		logger.info("--- Test for DataDrivenOrderTriggerRepository completed ---");
		
		
		/*
		 * Test CalendarOrderTriggerRepository
		 */
		CalendarOrderTrigger trigger4 = new CalendarOrderTrigger();
		trigger4.setMission(mission1);
		trigger4.setName(TEST_TRIGGER_3);
		trigger4.setWorkflow(workflow3);
		trigger4 = RepositoryService.getCalendarOrderTriggerRepository().save(trigger4);
		assertNotNull("CalendarOrderTrigger 4 not created correctly", trigger4);

		logger.info("OK: Test CalendarOrderTriggerRepository::save");
		
		// Test findByMissionCode
		List<CalendarOrderTrigger> coTriggerList = RepositoryService.getCalendarOrderTriggerRepository().findByMissionCode(TEST_CODE_1);
		assertEquals("Wrong number of triggers returned", 1, coTriggerList.size());
		
		logger.info("OK: Test CalendarOrderTriggerRepository::findByMissionCode");
		
		// Test findByMissionCodeAndName
		CalendarOrderTrigger coTrigger = RepositoryService.getCalendarOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNotNull("CalendarOrderTrigger 4 (" + TEST_CODE_1 + "/" + TEST_TRIGGER_3 + ") not found", coTrigger);
		
		logger.info("OK: Test CalendarOrderTriggerRepository::findByMissionCodeAndName");
		
		// Test delete
		RepositoryService.getCalendarOrderTriggerRepository().delete(trigger4);
		coTrigger = RepositoryService.getCalendarOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNull("CalendarOrderTrigger 4 not deleted", coTrigger);
		
		logger.info("--- Test for CalendarOrderTriggerRepository completed ---");
		
		
		/*
		 * Test DatatakeOrderTriggerRepository
		 */
		DatatakeOrderTrigger trigger5 = new DatatakeOrderTrigger();
		trigger5.setMission(mission1);
		trigger5.setName(TEST_TRIGGER_3);
		trigger5.setWorkflow(workflow3);
		trigger5 = RepositoryService.getDatatakeOrderTriggerRepository().save(trigger5);
		assertNotNull("DatatakeOrderTrigger 5 not created correctly", trigger5);

		logger.info("OK: Test DatatakeOrderTriggerRepository::save");
		
		// Test findByMissionCode
		List<DatatakeOrderTrigger> dtTriggerList = RepositoryService.getDatatakeOrderTriggerRepository().findByMissionCode(TEST_CODE_1);
		assertEquals("Wrong number of triggers returned", 1, dtTriggerList.size());
		
		logger.info("OK: Test DatatakeOrderTriggerRepository::findByMissionCode");
		
		// Test findByMissionCodeAndName
		DatatakeOrderTrigger dtTrigger = RepositoryService.getDatatakeOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNotNull("DatatakeOrderTrigger 5 (" + TEST_CODE_1 + "/" + TEST_TRIGGER_3 + ") not found", dtTrigger);
		
		logger.info("OK: Test DatatakeOrderTriggerRepository::findByMissionCodeAndName");
		
		// Test delete
		RepositoryService.getDatatakeOrderTriggerRepository().delete(trigger5);
		dtTrigger = RepositoryService.getDatatakeOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNull("DatatakeOrderTrigger 5 not deleted", dtTrigger);
		
		logger.info("--- Test for DatatakeOrderTriggerRepository completed ---");
		
		
		/*
		 * Test OrbitOrderTriggerRepository
		 */
		OrbitOrderTrigger trigger6 = new OrbitOrderTrigger();
		trigger6.setMission(mission1);
		trigger6.setName(TEST_TRIGGER_3);
		trigger6.setWorkflow(workflow3);
		trigger6 = RepositoryService.getOrbitOrderTriggerRepository().save(trigger6);
		assertNotNull("OrbitOrderTrigger 6 not created correctly", trigger6);

		logger.info("OK: Test OrbitOrderTriggerRepository::save");
		
		// Test findByMissionCode
		List<OrbitOrderTrigger> ooTriggerList = RepositoryService.getOrbitOrderTriggerRepository().findByMissionCode(TEST_CODE_1);
		assertEquals("Wrong number of triggers returned", 1, ooTriggerList.size());
		
		logger.info("OK: Test OrbitOrderTriggerRepository::findByMissionCode");
		
		// Test findByMissionCodeAndName
		OrbitOrderTrigger ooTrigger = RepositoryService.getOrbitOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNotNull("OrbitOrderTrigger 6 (" + TEST_CODE_1 + "/" + TEST_TRIGGER_3 + ") not found", ooTrigger);
		
		logger.info("OK: Test OrbitOrderTriggerRepository::findByMissionCodeAndName");
		
		// Test delete
		RepositoryService.getOrbitOrderTriggerRepository().delete(trigger6);
		ooTrigger = RepositoryService.getOrbitOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNull("OrbitOrderTrigger 6 not deleted", ooTrigger);
		
		logger.info("--- Test for OrbitOrderTriggerRepository completed ---");
		
		
		/*
		 * Test TimeIntervalOrderTriggerRepository
		 */
		TimeIntervalOrderTrigger trigger7 = new TimeIntervalOrderTrigger();
		trigger7.setMission(mission1);
		trigger7.setName(TEST_TRIGGER_3);
		trigger7.setWorkflow(workflow3);
		trigger7 = RepositoryService.getTimeIntervalOrderTriggerRepository().save(trigger7);
		assertNotNull("TimeIntervalOrderTrigger 7 not created correctly", trigger7);

		logger.info("OK: Test TimeIntervalOrderTriggerRepository::save");
		
		// Test findByMissionCode
		List<TimeIntervalOrderTrigger> tiTriggerList = RepositoryService.getTimeIntervalOrderTriggerRepository().findByMissionCode(TEST_CODE_1);
		assertEquals("Wrong number of triggers returned", 1, tiTriggerList.size());
		
		logger.info("OK: Test TimeIntervalOrderTriggerRepository::findByMissionCode");
		
		// Test findByMissionCodeAndName
		TimeIntervalOrderTrigger tiTrigger = RepositoryService.getTimeIntervalOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNotNull("TimeIntervalOrderTrigger 7 (" + TEST_CODE_1 + "/" + TEST_TRIGGER_3 + ") not found", tiTrigger);
		
		logger.info("OK: Test TimeIntervalOrderTriggerRepository::findByMissionCodeAndName");
		
		// Test delete
		RepositoryService.getTimeIntervalOrderTriggerRepository().delete(trigger7);
		tiTrigger = RepositoryService.getTimeIntervalOrderTriggerRepository()
				.findByMissionCodeAndName(TEST_CODE_1, TEST_TRIGGER_3);
		assertNull("TimeIntervalOrderTrigger 7 not deleted", tiTrigger);
		
		logger.info("--- Test for TimeIntervalOrderTriggerRepository completed ---");
		
	}

}
