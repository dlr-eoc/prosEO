package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.OrderManager;
import de.dlr.proseo.ordermgr.OrdermgrSecurityConfig;
import de.dlr.proseo.ordermgr.rest.model.Orbit;
import de.dlr.proseo.ordermgr.rest.model.OrbitUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
//@Transactional
@AutoConfigureTestEntityManager
public class OrbitControllerTest {
	/* The base URI of the Orbits */
	private static String ORBIT_BASE_URI = "/proseo/order-mgr/v0.1";

	@LocalServerPort
	private int port;
	
	@Autowired
	EntityManagerFactory emf;
	
	/** Test configuration */
	@Autowired
	OrdermgrTestConfiguration config;
	
	/** The security environment for this test */
	//@Autowired
	OrdermgrSecurityConfig ordermgrSecurityConfig;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	private static Logger logger = LoggerFactory.getLogger(OrbitControllerTest.class);
		
	/* Test orbits */
	private static String[][] testOrbitData = {
		//mission_id, mission_version, mission_code, mission_name,spacecraft_version,spacecraft_code,spacecraft_name, orbit_id, orbit_version, orbit_number, start_time, stop_time, 
		{ "0", "0", "ABCe", "ABCD Testing", "1","S_TDX1","Tandem-X", "0", "0", "14", "2019-08-29T22:49:21.074395", "2019-10-05T10:12:39.000000"},
		{ "11", "11", "DEFg", "DefrostMission", "2","S_TDX2","Tandem-X", "0", "0", "12", "2019-08-30T00:19:33.946628", "2019-10-05T10:13:22.000000"},
		{ "12", "12", "XY1Z", "XYZ Testing", "3","S_TDX3","Terrasar-X", "0", "0", "13", "2019-08-30T00:19:33.946628", "2019-10-05T10:13:22.000000"},
		
	};

	
	/**
	 * Create an orbit from a data array
	 * 
	 * @param testData an array of Strings representing the orbit to create
	 * @return an Orbit with its attributes set to the input data
	 */
	
	private de.dlr.proseo.model.Orbit createOrbit(String[] testData) {		
		logger.info("... creating orbit ");
		de.dlr.proseo.model.Mission testMission = new de.dlr.proseo.model.Mission();
		de.dlr.proseo.model.Spacecraft testSpacecraft = new de.dlr.proseo.model.Spacecraft();
		de.dlr.proseo.model.Orbit testOrbit = new de.dlr.proseo.model.Orbit();
		
		if (null != RepositoryService.getMissionRepository().findByCode(testData[2]))
			testMission = RepositoryService.getMissionRepository().findByCode(testData[2]);
		else {
			//Adding mission parameters
			testMission.setCode(testData[2]);
			testMission.setName(testData[3]);
			testMission = RepositoryService.getMissionRepository().save(testMission);			
		}
		
		if (null != RepositoryService.getSpacecraftRepository().findByCode(testData[5]))
		testSpacecraft = RepositoryService.getSpacecraftRepository().findByCode(testData[5]);
		else {
			//adding Spacecraft parameters
			testSpacecraft.setMission(testMission);
			testSpacecraft.incrementVersion();
			testSpacecraft.setCode(testData[5]);
			testSpacecraft.setName(testData[6]);
			testSpacecraft = RepositoryService.getSpacecraftRepository().save(testSpacecraft);				
		}

		if (null != RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(testData[5], Integer.valueOf(testData[9]))) {
			logger.info("Created test orbit {}", testOrbit.getId());

			return testOrbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(testData[5], Integer.valueOf(testData[9]));	
		}
			
		else{
			//Adding orbit parameters
			testOrbit.setOrbitNumber(Integer.valueOf(testData[9]));
			testOrbit.setStartTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[10])));
			testOrbit.setStopTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[11])));
			testOrbit.setSpacecraft(testSpacecraft);

			testOrbit = RepositoryService.getOrbitRepository().save(testOrbit);

			logger.info("Created test orbit {}", testOrbit.getId());
			return testOrbit;
				
			}
		
	}
	
	/**
	 * Create test orbits in the database
	 * 
	 * @return a list of test orbits generated
	 */
	private List<de.dlr.proseo.model.Orbit> createTestOrbits() {
		logger.info("Creating test orbits");
		List<de.dlr.proseo.model.Orbit> testOrbits = new ArrayList<>();		
		logger.info("Creating test orbits length: "+  testOrbitData.length);

		for (int i = 0; i < testOrbitData.length; ++i) {
			logger.info("Creating test orbits: "+ i +" "+ testOrbitData[i][2]);

			testOrbits.add(createOrbit(testOrbitData[i]));
		}
		return testOrbits;
	}
	/**
	 * Remove all (remaining) test orbits
	 * 
	 * @param testOrbits a list of test products to delete 
	 */
	private void deleteTestOrbits(List<de.dlr.proseo.model.Orbit> testOrbits) {
		
		//Session session = emf.unwrap(SessionFactory.class).openSession();		
		for (de.dlr.proseo.model.Orbit testOrbit: testOrbits) {	
			//testOrbit = (de.dlr.proseo.model.Orbit) session.merge(testOrbit);
			de.dlr.proseo.model.Spacecraft spacecraft = testOrbit.getSpacecraft();
			de.dlr.proseo.model.Mission mission = testOrbit.getSpacecraft().getMission();	
			
			RepositoryService.getSpacecraftRepository().delete(spacecraft);
			RepositoryService.getMissionRepository().delete(mission);
			RepositoryService.getOrbitRepository().delete(testOrbit);

		}
	}
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl.createOrbit(List<Orbit>)}.
	 * 
	 * Test: Create a new orbit
	 */
/*	@Test
	public final void testCreateOrbit() {
		// Create a mission in the database
		de.dlr.proseo.model.Orbit orbitToCreate = createOrbit(testOrbitData[0]);
		Orbit restOrbit = OrbitUtil.toRestOrbit(orbitToCreate);

		String testUrl = "http://localhost:" + this.port + ORBIT_BASE_URI + "/orbits";
		logger.info("Testing URL {} / POST", testUrl);
		
		List<Orbit> restOrbits = new ArrayList<Orbit>();
		
		ResponseEntity<List> postEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.postForEntity(testUrl, restOrbits, List.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, postEntity.getStatusCode());
	
		for (int i = 0 ; i < postEntity.getBody().size() ; i++) {
			restOrbit = (Orbit) postEntity.getBody().get(i);
			assertNotEquals("Id should not be 0 (zero): ", 0L, restOrbit.getId().longValue());
			assertEquals("Wrong start time: ", testOrbitData[0][10], restOrbit.getStartTime());
			assertEquals("Wrong stop time: ", testOrbitData[0][11], restOrbit.getStopTime());
			assertEquals("Wrong Orbit Number: ", Long.valueOf(testOrbitData[0][9]), restOrbit.getOrbitNumber());
		}

		// Test that the orbit exists
		testUrl += "/" + restOrbit.getId();
		ResponseEntity<Orbit> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Orbit.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		
		// Clean up database
		ArrayList<de.dlr.proseo.model.Orbit> testOrbit = new ArrayList<>();
		testOrbit.add(orbitToCreate);
		deleteTestOrbits(testOrbit);

		logger.info("Test OK: Create orbit");		
	}	

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl.deleteOrbitById(Long)}.
	 * 
	 * Test: Get an Orbit by ID
	 * Precondition: At least one orbit with a known ID is in the database
	 */
/*	@Test
	public final void testGetOrbitById() {
		// Make sure test orbits exist
		List<de.dlr.proseo.model.Orbit> testOrbits = createTestOrbits();
		de.dlr.proseo.model.Orbit orbitToFind = testOrbits.get(0);

		// Test that a orbits can be read
		String testUrl = "http://localhost:" + this.port + ORBIT_BASE_URI + "/orbits/" + orbitToFind.getId();
		logger.info("Testing URL {} / GET", testUrl);

		ResponseEntity<Orbit> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Orbit.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong orbit ID: ", orbitToFind.getId(), getEntity.getBody().getId().longValue());
		
		// Clean up database
		deleteTestOrbits(testOrbits);

		logger.info("Test OK: Get Orbit By ID");
	}
	
	/**
	 * Test method for { @link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl.deleteOrbitById(Long)}.
	 * 
	 * Test: Delete an Orbit by ID
	 * Precondition: An Orbit in the database
	 */
/*	@Test
	public final void testDeleteOrbitById() {
		// Make sure test orbits exist
		List<de.dlr.proseo.model.Orbit> testOrbits = createTestOrbits();
		de.dlr.proseo.model.Orbit orbitToDelete = testOrbits.get(0);
		testOrbits.remove(0);
		
		// Delete the first test orbit
		String testUrl = "http://localhost:" + this.port + ORBIT_BASE_URI + "/orbits/" + orbitToDelete.getId();
		logger.info("Testing URL {} / DELETE", testUrl);
		
		new TestRestTemplate(config.getUserName(), config.getUserPassword()).delete(testUrl);
		
		// Test that the orbit is gone
		ResponseEntity<Orbit> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Orbit.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, entity.getStatusCode());
		
		// Clean up database
		deleteTestOrbits(testOrbits);

		logger.info("Test OK: Delete Orbit By ID");
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl.modifyOrbit(Long, Orbit)}.
	 * 
	 * Test: Update an Orbit by ID
	 * Precondition: At least one orbit with a known ID is in the database 
	 */
/*	@Test
	public final void testModifyOrbit() {
		// Make sure test orbits exist
		List<de.dlr.proseo.model.Orbit> testOrbits = createTestOrbits();
		de.dlr.proseo.model.Orbit orbitToModify = testOrbits.get(0);
		
		// Update a orbit attribute
		orbitToModify.setStartTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse("2010-08-29T22:49:21.074395")));
		//orbitToModify.setOrbitNumber(Integer.valueOf("144"));
		
		Orbit restOrbit = OrbitUtil.toRestOrbit(orbitToModify);		
		String testUrl = "http://localhost:" + this.port + ORBIT_BASE_URI + "/orbits/" + orbitToModify.getId();
		logger.info("Testing URL {} / PATCH", testUrl);

		restOrbit = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.patchForObject(testUrl, restOrbit, Orbit.class);
		assertNotNull("Modified orbit not set", restOrbit);

		// Test that the orbit attribute was changed as expected
		ResponseEntity<Orbit> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Orbit.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong Start time: ", de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(orbitToModify.getStartTime()), getEntity.getBody().getStartTime());
		assertEquals("Wrong Stop time: ", de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(orbitToModify.getStopTime()), getEntity.getBody().getStopTime());
		assertEquals("Wrong orbit number: ", Long.valueOf(orbitToModify.getOrbitNumber()), getEntity.getBody().getOrbitNumber());

		// Clean up database
		deleteTestOrbits(testOrbits);

		logger.info("Test OK: Modify orbit");
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl.getOrbits(String, String, Long, Long, Date, Date)}.
	 * 
	 * Test: List of all orbits by mission, spacecraft, start time range, orbit number range
	 * Precondition: For all selection criteria orbits within and without a search value exist
	 */
	@Test
	public final void testGetOrbits() {
		// Make sure test products exist
		List<de.dlr.proseo.model.Orbit> testOrbits = createTestOrbits();
		
		//String testUrl = "http://localhost:" + this.port + ORBIT_BASE_URI + "/orbits?missionCode="+missionCode+"&spacecraftCode="+spacecraftCode;
		String testUrl = "http://localhost:" + this.port + ORBIT_BASE_URI + "/orbits";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
		// Build URI and Query parameters
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(testUrl)
		        // Add query parameter
		        .queryParam("spacecraftCode", "S_TDX1")
				.queryParam("orbitNumberFrom", 10)
				.queryParam("orbitNumberTo", 15)
				.queryParam("starttimefrom", testOrbitData[0][10])
				.queryParam("starttimeto", testOrbitData[0][11]);

		logger.info("Testing URL {} / GET, no params, with user {} and password {}", builder.buildAndExpand().toUri(), config.getUserName(), config.getUserPassword());
		
		RestTemplate restTemplate = rtb.basicAuthentication(config.getUserName(), config.getUserPassword()).build();	
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = restTemplate.exchange(builder.buildAndExpand().toUri(), HttpMethod.GET, requestEntity, List.class);
		
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		
		// Test that the correct orbits provided above are in the results
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> body = entity.getBody();
		logger.info("Found {} orbits", body.size());
		
		boolean[] orbitFound = new boolean[testOrbits.size()];
		Arrays.fill(orbitFound, false);
		for (Map<String, Object> orbit: body) {
			// Check, if any of the test orbits was returned
			long orbitId = (Integer) orbit.get("id");
			logger.info("... found orbit with ID {}", orbitId);
			for (int i = 0; i < testOrbits.size(); ++i) {
				de.dlr.proseo.model.Orbit testOrbit = testOrbits.get(i);
				if (orbitId == testOrbit.getId()) {
					orbitFound[i] = true;
					assertEquals("Wrong orbitnumber for test orbit " + i, testOrbit.getOrbitNumber(), orbit.get("orbitNumber"));
					assertEquals("Wrong start time for test orbit " + i,
							testOrbit.getStartTime(), Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse((String) orbit.get("startTime"))));
					assertEquals("Wrong stop time for test orbit " + i,
							testOrbit.getStopTime(), Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse((String) orbit.get("stopTime"))));
					assertEquals("Wrong spacecraft id for test orbit " + i,
							testOrbit.getSpacecraft().getCode(),orbit.get("spacecraftCode"));
				}
			}
		}
		
		boolean[] expectedOrbitFound = new boolean[body.size()];
		Arrays.fill(expectedOrbitFound, true);
		int actualLength = 0;
		for(int i=0;i<orbitFound.length;i++) {
			if(orbitFound[i])
				actualLength++;			
		}
		assertEquals(expectedOrbitFound.length, actualLength);
					
		// Clean up database
		deleteTestOrbits(testOrbits);

		logger.info("Test OK: Get Orbits");
	}

}
