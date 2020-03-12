/**
 * OrbitRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

import java.time.Instant;
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

import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Unit test cases for OrbitRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class OrbitRepositoryTest {

	private static final String TEST_SC_CODE = "$XYZ$";
	private static final int TEST_ORBIT_NUMBER = 47122174;
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.396521"));
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrbitRepositoryTest.class);
	
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
		Spacecraft spacecraft = new Spacecraft();
		spacecraft.setCode(TEST_SC_CODE);
		spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
		
		Orbit orbit = new Orbit();
		orbit.setSpacecraft(spacecraft);
		orbit.setOrbitNumber(TEST_ORBIT_NUMBER);
		orbit.setStartTime(TEST_START_TIME);
		orbit = RepositoryService.getOrbitRepository().save(orbit);
		
		spacecraft.getOrbits().add(orbit);
		RepositoryService.getSpacecraftRepository().save(spacecraft);
		
		// Test findBySpacecraftCodeAndOrbitNumber
		orbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(TEST_SC_CODE, TEST_ORBIT_NUMBER);
		assertNotNull("Find by spacecraft code and orbit number failed for Orbit", orbit);
		
		logger.info("OK: Test for findBySpacecraftCodeAndOrbitNumber completed");
		
		// Test findBySpacecraftCodeAndOrbitNumberBetween
		List<Orbit> orbits = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumberBetween(
				TEST_SC_CODE, TEST_ORBIT_NUMBER, TEST_ORBIT_NUMBER + 1);
		assertFalse("Find by spacecraft code and orbit number between failed for Orbit", orbits.isEmpty());
		
		logger.info("OK: Test for findBySpacecraftCodeAndOrbitNumberBetween completed");
		
		// Test findBySpacecraftCodeAndStartTimeBetween
		orbits = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndStartTimeBetween(
				TEST_SC_CODE, TEST_START_TIME, TEST_START_TIME.plusSeconds(600));
		assertFalse("Find by spacecraft code and start time between failed for Orbit", orbits.isEmpty());
		
		logger.info("OK: Test for findBySpacecraftCodeAndStartTimeBetween completed");
		
	}

}
