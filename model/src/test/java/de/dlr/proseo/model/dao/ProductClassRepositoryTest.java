/**
 * ProductClassRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

import java.util.List;

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

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for ProcessorRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ProductClassRepositoryTest {

	private static final String TEST_CODE = "$ABC$";
	private static final String TEST_PRODUCT_TYPE = "$FRESCO$";
	private static final String TEST_MISSION_TYPE = "$L2__FRESCO_$";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassRepositoryTest.class);
	
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
		Mission mission = new Mission();
		mission.setCode(TEST_CODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		ProductClass prodClass = new ProductClass();
		prodClass.setMission(mission);
		prodClass.setMissionType(TEST_MISSION_TYPE);
		prodClass.setProductType(TEST_PRODUCT_TYPE);
		prodClass = RepositoryService.getProductClassRepository().save(prodClass);
		
		mission.getProductClasses().add(prodClass);
		RepositoryService.getMissionRepository().save(mission);
		
		
		// Test findByMissionCode
		List<ProductClass> prodClasses = RepositoryService.getProductClassRepository().findByMissionCode(TEST_CODE);
		assertFalse("Find by mission code failed for ProductClass", prodClasses.isEmpty());
		
		logger.info("OK: Test for findByMissionCode completed");
		
		// Test findByProductType
		prodClasses = RepositoryService.getProductClassRepository().findByProductType(TEST_PRODUCT_TYPE);
		assertFalse("Find by product type failed for ProductClass", prodClasses.isEmpty());
		
		logger.info("OK: Test for findByProductType completed");
		
		// Test findByMissionType
		prodClasses = RepositoryService.getProductClassRepository().findByMissionType(TEST_MISSION_TYPE);
		assertFalse("Find by mission type failed for ProductClass", prodClasses.isEmpty());
		
		logger.info("OK: Test for findByMissionType completed");
		
		// Test findByMissionCodeAndProductType
		prodClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(TEST_CODE, TEST_PRODUCT_TYPE);
		assertNotNull("Find by mission code and product type failed for ProcessingOrder", prodClass);
		
		logger.info("OK: Test for findByMissionCodeAndProductType completed");
		
		// Test findByMissionCodeAndMissionType
		prodClass = RepositoryService.getProductClassRepository().findByMissionCodeAndMissionType(TEST_CODE, TEST_MISSION_TYPE);
		assertNotNull("Find by mission code and mission type failed for ProcessingOrder", prodClass);
		
		logger.info("OK: Test for findByMissionCodeAndMissionType completed");
		
	}

}
