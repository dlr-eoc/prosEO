/**
 * ProductQueryRepositoryTest.java
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
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for FacilityRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ProductQueryRepositoryTest {

	private static final String TEST_CODE = "$ABC$";
	private static final String TEST_PRODUCT_TYPE = "$L2__FRESCO_$";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductQueryRepositoryTest.class);
	
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
		prodClass.setProductType(TEST_PRODUCT_TYPE);
		prodClass = RepositoryService.getProductClassRepository().save(prodClass);
		
		mission.getProductClasses().add(prodClass);
		RepositoryService.getMissionRepository().save(mission);

		ProductQuery query = new ProductQuery();
		query.setIsSatisfied(false);
		query.setRequestedProductClass(prodClass);
		query = RepositoryService.getProductQueryRepository().save(query);
		
		// Test findUnsatisfiedByProductClass
		List<ProductQuery> queryList = RepositoryService.getProductQueryRepository().findUnsatisfiedByProductClass(prodClass.getId());
		assertFalse("Find unsatisfied by product class id failed for ProductQuery", queryList.isEmpty());
		
		logger.info("OK: Test for findUnsatisfiedByProductClass completed");
		
	}

}
