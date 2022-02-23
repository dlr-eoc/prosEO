package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryService;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.ordermgr.OrderManager;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
//@DirtiesContext
//@Transactional
@AutoConfigureTestEntityManager
public class MissionControllerTest {
	
	/* The base URI of the Missions */
	private static String MISSION_BASE_URI = "/proseo/order-mgr/v0.1";

	@LocalServerPort
	private int port;
	
	@Autowired
	EntityManagerFactory emf;
	
	/** Test configuration */
	@Autowired
	OrdermgrTestConfiguration config;
	
	/** The security environment for this test */
	//@Autowired
	//OrdermgrSecurityConfig ordermgrSecurityConfig;
	
	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MissionControllerTest.class);
	
	/* Test missions */
	private static String[][] testMissionData = {
			// id, version, mission_code, mission_name,fileClass, processinMode, spacecraft_version,spacecraft_code,spacecraft_name
			{ "0", "0", "ABCe", "ABCD Testing", "TEST","NRTI","1","S_TDX1","Tandom-X"},
			{ "11", "11", "DEFg", "DefrostMission", "OPER","OFFL","2","S_TDX2","Tandom-X"},
			{ "12", "12", "XY1Z", "XYZ Testing","TEST","OFFL", "3","S_TDX3","Tandom-X" }
	};
	
	/**
	 * Create a mission from a data array
	 * 
	 * @param testData an array of Strings representing the mission to create
	 * @return a mission with its attributes set to the input data
	 */
	private Mission createMission(String[] testData) {
		Mission testMission = RepositoryService.getMissionRepository().findByCode(testData[2]);
		if (null != testMission) {
			return testMission;
		}
		
		testMission = new Mission();
		Spacecraft testSpacecraft = new Spacecraft();

		logger.info("... creating mission ");
		//Adding mission parameters
		testMission.setCode(testData[2]);
		testMission.setName(testData[3]);
		testMission.getFileClasses().clear();
		testMission.getFileClasses().add(testData[4]);
		testMission.getProcessingModes().add(testData[5]);
		
	    //TBD : for testing just using a constant string
	    String template = "S5P_${fileClass}_${productClass.missionType}.nc";
		testMission.setProductFileTemplate(template);
		testMission = RepositoryService.getMissionRepository().save(testMission);		
		
		//adding Spacecraft parameters
		testSpacecraft.setMission(testMission);
		testSpacecraft.setCode(testData[7]);
		testSpacecraft.setName(testData[8]);
		testSpacecraft = RepositoryService.getSpacecraftRepository().save(testSpacecraft);

		testMission.getSpacecrafts().add(testSpacecraft);
		testMission = RepositoryService.getMissionRepository().save(testMission);
		
		logger.info("Created test mission {}", testMission.getId());
		return testMission;
	}
	
	/**
	 * Create test missions in the database
	 * 
	 * @return a list of missions generated
	 */
	
	private List<Mission> createTestMissions() {
		logger.info("Creating test missions");
		List<Mission> testMissions = new ArrayList<>();		
		logger.info("Creating test missions length: "+  testMissionData.length);

		for (int i = 0; i < testMissionData.length; ++i) {
			logger.info("Creating test missions: "+ i +" "+ testMissionData[i][2]);

			testMissions.add(createMission(testMissionData[i]));
		}
		return testMissions;
	}
	
	/**
	 * Remove all (remaining) test missions
	 * 
	 * @param testMissions a list of test missions to delete 
	 */
	private void deleteTestMissions(List<Mission> testMissions) {
		for (Mission testMission: testMissions) {
			testMission = RepositoryService.getMissionRepository().findByCode(testMission.getCode());
			RepositoryService.getSpacecraftRepository().deleteAll(testMission.getSpacecrafts());
			RepositoryService.getMissionRepository().deleteById(testMission.getId());
		}
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl.createMission(Mission)}.
	 * 
	 * Test: Create a new mission
	 */
//	@Test
	public final void testCreateMission() {
		// Create a mission in the database
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		Mission missionToCreate = transactionTemplate.execute((status) -> {
			return createMission(testMissionData[1]);
		});

		RestMission restMission = MissionUtil.toRestMission(missionToCreate);

		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions";
		logger.info("Testing URL {} / POST", testUrl);
		
		ResponseEntity<RestMission> postEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.postForEntity(testUrl, restMission, RestMission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, postEntity.getStatusCode());
		restMission = postEntity.getBody();

		assertNotEquals("Id should not be 0 (zero): ", 0L, restMission.getId().longValue());

		// Test that the mission exists
		testUrl += "/" + restMission.getId();
		ResponseEntity<Mission> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Mission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
	
		// Clean up database
		ArrayList<Mission> testMission = new ArrayList<>();
		testMission.add(missionToCreate);
		transactionTemplate.execute((status) -> {
			deleteTestMissions(testMission);
			return null;
		});

		logger.info("Test OK: Create mission");		
	}	

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl.getMissions()}.
	 * 
	 * Test: List of all missions
	 * 
	 */
	@Test
	public final void testGetMissions() {
		// Make sure test missions exist
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		List<Mission> testMissions = transactionTemplate.execute((status) -> {
			return createTestMissions();
		});
				
		// Get missions using different selection criteria (also combined)
		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions";
		logger.info("Testing URL {} / GET, no params, with user {} and password {}", testUrl, config.getUserName(), config.getUserPassword());

		RestTemplate restTemplate = rtb.basicAuthentication(config.getUserName(), config.getUserPassword()).build();
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = restTemplate.getForEntity(testUrl, List.class);
		
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		
		
		// Test that the correct missions provided above are in the results
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> body = entity.getBody();
		assertTrue("Too few missions", entity.getBody().size() >= testMissions.size());
		logger.info("Found {} missions", body.size());
		
		boolean[] missionFound = new boolean[testMissions.size()];
		Arrays.fill(missionFound, false);
		for (Map<String, Object> mission: body) {
			// Check, if any of the test missions was returned
			long missionId = (Integer) mission.get("id");
			logger.info("... found mission with ID {}", missionId);
			for (int i = 0; i < testMissions.size(); ++i) {
				Mission testMission = testMissions.get(i);
				if (missionId == testMission.getId()) {
					missionFound[i] = true;
					assertEquals("Wrong code for test mission " + i, testMission.getCode(), mission.get("code"));
					assertEquals("Wrong name for test mission " + i, testMission.getName(), mission.get("name"));
				}
			}
		}
		boolean[] expectedMissionFound = new boolean[testMissions.size()];
		Arrays.fill(expectedMissionFound, true);
		assertArrayEquals("Not all missions found", expectedMissionFound, missionFound);
		
		// TODO Tests with different selection criteria
		
		// Clean up database
		transactionTemplate.execute((status) -> {
			deleteTestMissions(testMissions);
			return null;
		});

		logger.info("Test OK: Get Missions");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl.getMissionById(Long)}.
	 * 
	 * Test: Get a mission by ID
	 * Precondition: At least one mission with a known ID is in the database
	 */
//	@Test
	public final void testGetMissionById() {
		// Make sure test missions exist
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		List<Mission> testMissions = transactionTemplate.execute((status) -> {
			return createTestMissions();
		});
				
		Mission missionToFind = testMissions.get(0);

		// Test that a mission can be read
		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions/" + missionToFind.getId();
		logger.info("Testing URL {} / GET", testUrl);

		ResponseEntity<RestMission> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestMission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong mission ID: ", missionToFind.getId(), getEntity.getBody().getId().longValue());
		
		// Clean up database
		transactionTemplate.execute((status) -> {
			deleteTestMissions(testMissions);
			return null;
		});

		logger.info("Test OK: Get Mission By ID");
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl.deleteMissionById(Long)}.
	 * 
	 * Test: Delete a mission by ID
	 * Precondition: A mission in the database
	 */
//	@Test
	public final void testDeleteMissionById() {
//		// Make sure test missions exist
//		List<de.dlr.proseo.model.Mission> testMissions = createTestMissions();
//		de.dlr.proseo.model.Mission missionToDelete = testMissions.get(0);
//		testMissions.remove(0);
//		
//		// Delete the first test mission
//		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions/" + missionToDelete.getId();
//		logger.info("Testing URL {} / DELETE", testUrl);
//		
//		new TestRestTemplate(config.getUserName(), config.getUserPassword()).delete(testUrl);
//		
//		// Test that the mission is gone
//		ResponseEntity<Mission> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
//				.getForEntity(testUrl, Mission.class);
//		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, entity.getStatusCode());
//		
//		// Clean up database
//		deleteTestMissions(testMissions);
		
		logger.warn("NOT IMPLEMENTED: Test delete mission by ID");

		logger.info("Test OK: Delete Mission By ID");
	}
	
	/**
	 * Test method for {@linkde.dlr.proseo.ordermgr.rest.MissionControllerImpl.modifyMission(Long, Mission)}.
	 * 
	 * Test: Update a mission by ID
	 * Precondition: At least one mission with a known ID is in the database 
	 */
//	@Test
	public final void testModifyMission() {
		// Make sure test missions exist
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		List<Mission> testMissions = transactionTemplate.execute((status) -> {
			return createTestMissions();
		});

		Mission missionToModify = testMissions.get(0);
	
		// Update a mission attribute and a spacecraft attribute
		missionToModify.setCode("MOD Code");
	
		for (Spacecraft spacecraft : missionToModify.getSpacecrafts()) {
			if (spacecraft.getCode().equals(testMissionData[0][7])) {
				missionToModify.getSpacecrafts().remove(spacecraft);
				spacecraft.setCode("MOD_SP_CODE");
				missionToModify.getSpacecrafts().add(spacecraft);

			}
		}
		
		RestMission restMission = MissionUtil.toRestMission(missionToModify);
		logger.info("RestMission modified Code: "+restMission.getCode());
		
		String testUrl = "http://localhost:" + this.port + MISSION_BASE_URI + "/missions/" + missionToModify.getId();
		logger.info("Testing URL {} / PATCH", testUrl);

		restMission = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.patchForObject(testUrl, restMission, RestMission.class);
		assertNotNull("Modified mission not set", restMission);

		// Test that the mission attribute was changed as expected
		ResponseEntity<Mission> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, Mission.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong Code: ", missionToModify.getCode(), getEntity.getBody().getCode());
		
		// Clean up database
		transactionTemplate.execute((status) -> {
			deleteTestMissions(testMissions);
			return null;
		});

		logger.info("Test OK: Modify mission");
	}

}
