/**
 * ProductRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

import java.time.Instant;
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

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Unit test cases for ProductRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ProductRepositoryTest {

	private static final String TEST_CODE = "$ABC$";
	private static final String TEST_SC_CODE = "$XYZ$";
	private static final int TEST_ORBIT_NUMBER = 47111174;
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.396521"));
	private static final String TEST_PRODUCT_TYPE = "$L2__FRESCO_$";
	private static final UUID TEST_UUID = UUID.randomUUID();
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductRepositoryTest.class);
	
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
		Mission mission = new Mission();
		mission.setCode(TEST_CODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		Spacecraft spacecraft = new Spacecraft();
		spacecraft.setMission(mission);
		spacecraft.setCode(TEST_SC_CODE);
		spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
		mission.getSpacecrafts().add(spacecraft);
		
		Orbit orbit = new Orbit();
		orbit.setSpacecraft(spacecraft);
		orbit.setOrbitNumber(TEST_ORBIT_NUMBER);
		orbit.setStartTime(TEST_START_TIME);
		orbit = RepositoryService.getOrbitRepository().save(orbit);
		spacecraft.getOrbits().add(orbit);

		ProductClass prodClass = new ProductClass();
		prodClass.setMission(mission);
		prodClass.setProductType(TEST_PRODUCT_TYPE);
		prodClass = RepositoryService.getProductClassRepository().save(prodClass);
		mission.getProductClasses().add(prodClass);
		
		Product product = new Product();
		product.setUuid(TEST_UUID);
		product.setProductClass(prodClass);
		product.setOrbit(orbit);
		product.setSensingStartTime(TEST_START_TIME);
		product.setSensingStopTime(TEST_START_TIME.plusSeconds(900));
		product = RepositoryService.getProductRepository().save(product);
		
		RepositoryService.getProductClassRepository().save(prodClass);
		RepositoryService.getOrbitRepository().save(orbit);
		RepositoryService.getSpacecraftRepository().save(spacecraft);
		RepositoryService.getMissionRepository().save(mission);
		
		// Test findByMissionCodeAndProductTypeAndOrbitNumberBetween
		List<Product> products = RepositoryService.getProductRepository().findByMissionCodeAndProductTypeAndOrbitNumberBetween(
				TEST_CODE, TEST_PRODUCT_TYPE, TEST_ORBIT_NUMBER, TEST_ORBIT_NUMBER + 1);
		assertFalse("Find by mission code, product type and orbit failed for Product", products.isEmpty());
		
		logger.info("OK: Test for findByMissionCodeAndProductTypeAndOrbitNumberBetween completed");
		
		// Test findByMissionCodeAndProductTypeAndSensingStartTimeBetween
		products = RepositoryService.getProductRepository().findByMissionCodeAndProductTypeAndSensingStartTimeBetween(
				TEST_CODE, TEST_PRODUCT_TYPE, TEST_START_TIME, TEST_START_TIME.plusSeconds(200));
		assertFalse("Find by mission code, product type and start time failed for Product", products.isEmpty());
		
		logger.info("OK: Test for findByMissionCodeAndProductTypeAndSensingStartTimeBetween completed");
		
		// Test findByMissionCodeAndProductTypeAndSensingStartTimeLessAndSensingStopTimeGreater (testing intersection)
		products = RepositoryService.getProductRepository().findByMissionCodeAndProductTypeAndSensingStartTimeLessAndSensingStopTimeGreater(
				TEST_CODE, TEST_PRODUCT_TYPE, TEST_START_TIME.plusSeconds(1000), TEST_START_TIME.minusSeconds(200));
		assertFalse("Find by mission code, product type and start/stop time failed for Product", products.isEmpty());
		
		logger.info("OK: Test for findByMissionCodeAndProductTypeAndSensingStartTimeLessAndSensingStopTimeGreater completed");
		
		// Test findByUuid
		product = RepositoryService.getProductRepository().findByUuid(TEST_UUID);
		assertFalse("Find by UUID failed for Product", null == product);
		
		logger.info("OK: Test for findByUuid completed");
	}

}
