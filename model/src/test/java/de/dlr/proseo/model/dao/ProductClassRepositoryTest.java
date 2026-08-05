/**
 * ProductClassRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
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
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class ProductClassRepositoryTest {

	private static final String TEST_CODE = "$ABC$";
	private static final String TEST_PRODUCT_TYPE = "$L2__FRESCO_$";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassRepositoryTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeEach
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	/**
	 * Test the additional repository methods
	 */
	@Test
	public final void test() {
		Mission mission = new Mission();
		mission.setCode(TEST_CODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		ProductClass prodClass = new ProductClass();
		prodClass.setMission(mission);
		prodClass.setProductType(TEST_PRODUCT_TYPE);
		prodClass = RepositoryService.getProductClassRepository().save(prodClass);
		
		mission.getProductClasses().add(prodClass);
		RepositoryService.getMissionRepository().save(mission);
		
		
		// Test findByMissionCode
		List<ProductClass> prodClasses = RepositoryService.getProductClassRepository().findByMissionCode(TEST_CODE);
		assertFalse(prodClasses.isEmpty(), "Find by mission code failed for ProductClass");
		
		logger.info("OK: Test for findByMissionCode completed");
		
		// Test findByProductType
		prodClasses = RepositoryService.getProductClassRepository().findByProductType(TEST_PRODUCT_TYPE);
		assertFalse(prodClasses.isEmpty(), "Find by product type failed for ProductClass");
		
		logger.info("OK: Test for findByProductType completed");
		
		// Test findByMissionCodeAndProductType
		prodClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(TEST_CODE, TEST_PRODUCT_TYPE);
		assertNotNull(prodClass, "Find by mission code and product type failed for ProcessingOrder");
		
		logger.info("OK: Test for findByMissionCodeAndProductType completed");		
	}

}
