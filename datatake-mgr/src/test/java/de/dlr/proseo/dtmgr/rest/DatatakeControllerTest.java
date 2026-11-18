/**
 * DatatakeControllerTest.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.dtmgr.DatatakeManagerApplication;
import de.dlr.proseo.dtmgr.rest.model.DatatakeUtil;
import de.dlr.proseo.dtmgr.rest.model.RestDataSegment;
import de.dlr.proseo.dtmgr.rest.model.RestDatatake;
import de.dlr.proseo.dtmgr.rest.model.RestParameter;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.DataSegment;
import de.dlr.proseo.model.Datatake;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import jakarta.persistence.EntityManager;

/**
 * Testing the service methods required to create, modify and delete datatakes and segments
 * in the prosEO database, and to query the database about datatakes and segments
 * 
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = DatatakeManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("DatatakeControllerImpl")
class DatatakeControllerTest {

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
		{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
	private static String[] testMissionData2 =
			// code, name, processing_mode, file_class, product_file_template
		{ "XYZ", "XYZ Testing", "NTC", "TEST", "test_file_temp" };

	private static final String TEST_SC_CODE = "PROSEO";
	private static final String TEST_PAYLOAD = "payloadX";
	private static final String TEST_DATATAKE_ID = "dt-2026-001";
	private static final String TEST_DATATAKE_ID_INVALID = "dt-2026-001-invalid";
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.637000"));
	private static final Duration TEST_DURATION = Duration.ofSeconds(10);
	private static final String TEST_PARAM_KEY = "key1";
	private static final String TEST_PARAM_VALUE = "value1";

	// Test query parameters
	private static final String START_TIME_FROM = "2026-01-01T00:00:00";
	private static final String START_TIME_TO = "2026-01-31T23:59:59";
	private static final Long RECORD_FROM = 0L;
	private static final Long RECORD_TO = 10L;
	private static final String[] ORDER_BY = { "id DESC" };
	private static final Long DATATAKE_DB_ID_INVALID = 421L;


	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DatatakeControllerTest.class);

	/** The datatake controller under test */
	@Autowired
	DatatakeControllerImpl dci;

	/** Database interface */
	@Autowired
	private EntityManager em;

	/**
	 * Set up test data for all tests
	 * 
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeAll() throws Exception {
		logger.trace(">>> Starting to create test data in the database");

		createTestMission(testMissionData);

		logger.trace("<<< Finished creating test data in database");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterAll() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	/**
	 * Create a mission and spacecraft
	 * 
	 * @param missionData the mission data to use for the mission
	 */
	private static void createTestMission(String[] missionData) {
		
		logger.trace("... creating mission {}", missionData[0]);

		Mission mission = new Mission();
		mission.setCode(missionData[0]);
		mission.setName(missionData[1]);
		mission.getProcessingModes().add(missionData[2]);
		mission.getFileClasses().add(missionData[3]);
		mission.setProductFileTemplate(missionData[4]);
		mission = RepositoryService.getMissionRepository().save(mission);

		logger.trace("... adding spacecraft {}", TEST_SC_CODE);

		Spacecraft spacecraft = new Spacecraft();
		spacecraft.setCode(TEST_SC_CODE);
		spacecraft.setMission(mission);
		Payload payload = new Payload();
		payload.setName(TEST_PAYLOAD);
		spacecraft.getPayloads().add(payload);
		spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
	}
	
	/**
	 * Create a datatake for the given mission
	 * 
	 * @param missionCode the mission to create the datatake for
	 * @return the database ID of the newly created datatake
	 */
	private long createTestDatatake(String missionCode) {

		Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByMissionAndCode(missionCode, TEST_SC_CODE);

		logger.trace("... creating datatake {}", TEST_DATATAKE_ID);

		Datatake datatake = new Datatake();
		datatake.setDatatakeId(TEST_DATATAKE_ID);
		datatake.setSpacecraft(spacecraft);
		datatake.setPayload(spacecraft.getPayloads().get(0));
		datatake.setSensingStartTime(TEST_START_TIME);
		datatake.setDatatakeDuration(TEST_DURATION);
		Parameter dtParam = new Parameter().init(ParameterType.STRING, TEST_PARAM_VALUE);
		datatake.getParameters().put(TEST_PARAM_KEY, dtParam);
		datatake = RepositoryService.getDatatakeRepository().save(datatake);

		logger.trace("... adding data segments", TEST_DATATAKE_ID);

		DataSegment ds1 = new DataSegment();
		ds1.setSegmentNumber(2); // adding in wrong order!
		ds1.setDatatake(datatake);
		ds1.setSensingStartTime(TEST_START_TIME.plusSeconds(TEST_DURATION.dividedBy(2).getSeconds()));
		ds1.setSegmentDuration(TEST_DURATION.dividedBy(2));
		em.persist(ds1);
		datatake.getDataSegments().add(ds1);

		DataSegment ds2 = new DataSegment();
		ds2.setSegmentNumber(1); // adding in wrong order!
		ds2.setDatatake(datatake);
		ds2.setSensingStartTime(TEST_START_TIME);
		ds2.setSegmentDuration(TEST_DURATION.dividedBy(2));
		em.persist(ds2);
		datatake.getDataSegments().add(ds2);

		datatake = RepositoryService.getDatatakeRepository().save(datatake);

		return datatake.getId();
	}

	// ------------------------------------------------------------------
	// getDatatakes
	// ------------------------------------------------------------------

	@Nested
	@DisplayName("getDatatakes(...)")
	class GetDatatakeTests {

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#getDatatakes(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Long, java.lang.Long, java.lang.String[])}.
		 */
		@Test
		@DisplayName("get returns 200 OK with the datatake list from the manager")
		void getReturnsOkWithListFromManager() {
			createTestDatatake(testMissionData[0]);

			ResponseEntity<List<RestDatatake>> response =
					dci.getDatatakes(TEST_SC_CODE, TEST_PAYLOAD, null, START_TIME_FROM, START_TIME_TO,
							RECORD_FROM, RECORD_TO, ORDER_BY);

			assertEquals(HttpStatus.OK, response.getStatusCode(), "Unexpected HTTP status code");
			assertTrue(response.hasBody(), "Response body missing");
			assertEquals(1, response.getBody().size(), "Unexpected number of datatakes returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "GetDatatakeTests.getReturnsOkWithListFromManager");
		}

		@Test
		@DisplayName("get passes null for all optional criteria when no filter is given")
		void getPassesNullCriteriaToManager() {
			createTestDatatake(testMissionData[0]);

			ResponseEntity<List<RestDatatake>> response =
					dci.getDatatakes(null, null, null, null, null, null, null, null);

			assertEquals(HttpStatus.OK, response.getStatusCode(), "Unexpected HTTP status code");
			assertTrue(response.hasBody(), "Response body missing");
			assertEquals(1, response.getBody().size(), "Unexpected number of datatakes returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "GetDatatakeTests.getPassesNullCriteriaToManager");
		}

		@Test
		@DisplayName("get returns 400 BAD_REQUEST with error headers on IllegalArgumentException")
		void getReturnsBadRequestOnIllegalArgumentException() {
			createTestDatatake(testMissionData[0]);

			// wrong time interval selection
			ResponseEntity<List<RestDatatake>> response = dci.getDatatakes(
					TEST_SC_CODE, TEST_PAYLOAD, TEST_DATATAKE_ID, START_TIME_TO, START_TIME_FROM,
					RECORD_FROM, RECORD_TO, ORDER_BY);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Unexpected HTTP status code for bad start times");

			// wrong record number selection
			response = dci.getDatatakes(
					TEST_SC_CODE, TEST_PAYLOAD, TEST_DATATAKE_ID, START_TIME_FROM, START_TIME_TO,
					RECORD_TO, RECORD_FROM, ORDER_BY);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Unexpected HTTP status code for bad record numbers");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "GetDatatakeTests.getReturnsBadRequestOnIllegalArgumentException");
		}

		@Test
		@DisplayName("get returns 404 NOT_FOUND with error headers on EntityNotFoundException")
		void getReturnsNotFoundOnEntityNotFoundException() {
			ResponseEntity<List<RestDatatake>> response = dci.getDatatakes(
					TEST_SC_CODE, TEST_PAYLOAD, TEST_DATATAKE_ID_INVALID, null, null,
					null, null, null);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "GetDatatakeTests.getReturnsNotFoundOnEntityNotFoundException");
		}
	}

	// ------------------------------------------------------------------
	// markProcessed
	// ------------------------------------------------------------------

	@Nested
	@DisplayName("markProcessed(...)")
	class MarkProcessedTests {

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#markProcessed(java.lang.Long)}.
		 */
		@Test
		@DisplayName("mark returns 200 OK with the updated datatake")
		void markReturnsOkWithUpdatedDatatake() {
			long datatakeDbid = createTestDatatake(testMissionData[0]);
			
			ResponseEntity<RestDatatake> response = dci.markProcessed(datatakeDbid);

			assertEquals(HttpStatus.OK, response.getStatusCode(),  "Unexpected HTTP status code");
			assertTrue(response.hasBody(), "Response body missing");
			assertEquals(datatakeDbid, response.getBody().getId(), "Database ID mismatch");
			assertNotNull(response.getBody().getProcessingTime(), "Datatake processing time not set");

			logger.log(GeneralMessage.UNIT_TEST_OK, "MarkProcessedTests.markReturnsOkWithUpdatedDatatake");
		}

		@Test
		@DisplayName("mark returns 404 NOT_FOUND with error headers when the datatake does not exist")
		void markReturnsNotFoundOnEntityNotFoundException() {
			createTestDatatake(testMissionData[0]);
			
			ResponseEntity<RestDatatake> response = dci.markProcessed(DATATAKE_DB_ID_INVALID);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "MarkProcessedTests.markReturnsNotFoundOnEntityNotFoundException");
		}

		@Test
		@DisplayName("mark returns 403 FORBIDDEN due to cross-mission access")
		void markReturnsForbiddenOnSecurityException() {
			
			// Prepare second mission and datatake
			createTestMission(testMissionData2);
			long datatakeDbid = createTestDatatake(testMissionData2[0]);

			ResponseEntity<RestDatatake> response = dci.markProcessed(datatakeDbid);

			assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "MarkProcessedTests.markReturnsForbiddenOnSecurityException");
		}
	}

	// ------------------------------------------------------------------
	// createDatatake
	// ------------------------------------------------------------------

	@Nested
	@DisplayName("createDatatake(...)")
	class CreateDatatakeTests {

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#createDatatake(de.dlr.proseo.dtmgr.rest.model.RestDatatake)}.
		 */
		@Test
		@DisplayName("create returns 201 CREATED with the new datatake")
		final void testCreateDatatake() {
			RestDatatake restDatatake = new RestDatatake();
			restDatatake.setMissionCode(testMissionData[0]);
			restDatatake.setSpacecraftCode(TEST_SC_CODE);
			restDatatake.setPayloadName(TEST_PAYLOAD);
			restDatatake.setDatatakeId(TEST_DATATAKE_ID);
			restDatatake.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
			restDatatake.setDatatakeDuration(TEST_DURATION.toMillis());
			restDatatake.setIsComplete(false);
			restDatatake.getParameters().add(new RestParameter(TEST_PARAM_KEY, ParameterType.STRING.toString(), TEST_PARAM_VALUE));
			
			RestDataSegment restDataSegment = new RestDataSegment();
			restDataSegment.setMissionCode(testMissionData[0]);
			restDataSegment.setSpacecraftCode(TEST_SC_CODE);
			restDataSegment.setPayloadName(TEST_PAYLOAD);
			restDataSegment.setDatatakeId(TEST_DATATAKE_ID);
			restDataSegment.setSegmentNumber(1L);
			restDataSegment.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
			restDataSegment.setSegmentDuration(TEST_DURATION.toMillis());
			
			restDatatake.getDataSegments().add(restDataSegment);
			
			ResponseEntity<RestDatatake> response = dci.createDatatake(restDatatake);
			
			assertEquals(HttpStatus.CREATED, response.getStatusCode(),  "Unexpected HTTP status code");
			assertTrue(response.hasBody(), "Response body missing");
			assertEquals(TEST_DATATAKE_ID, response.getBody().getDatatakeId(), "Datatake ID mismatch");

			logger.log(GeneralMessage.UNIT_TEST_OK, "CreateDatatakeTests.testCreateDatatake");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#createDatatake(de.dlr.proseo.dtmgr.rest.model.RestDatatake)}.
		 */
		@Test
		@DisplayName("create returns 400 BAD_REQUEST due to bad input data")
		final void createReturnsBadRequestOnInvalidInput() {
			RestDatatake restDatatake = new RestDatatake();
			restDatatake.setMissionCode(testMissionData[0]); 
			restDatatake.setSpacecraftCode(TEST_SC_CODE);
			restDatatake.setPayloadName(TEST_PAYLOAD);
			// restDatatake.setDatatakeId(TEST_DATATAKE_ID); // <== MANDATORY DATA MISSING
			restDatatake.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
			restDatatake.setDatatakeDuration(TEST_DURATION.toMillis());
			restDatatake.setIsComplete(false);
			restDatatake.getParameters().add(new RestParameter(TEST_PARAM_KEY, ParameterType.STRING.toString(), TEST_PARAM_VALUE));
			
			RestDataSegment restDataSegment = new RestDataSegment();
			restDataSegment.setMissionCode(testMissionData[0]);
			restDataSegment.setSpacecraftCode(TEST_SC_CODE);
			restDataSegment.setPayloadName(TEST_PAYLOAD);
			restDataSegment.setDatatakeId(TEST_DATATAKE_ID);
			restDataSegment.setSegmentNumber(1L);
			restDataSegment.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
			restDataSegment.setSegmentDuration(TEST_DURATION.toMillis());
			
			restDatatake.getDataSegments().add(restDataSegment);
			
			ResponseEntity<RestDatatake> response = dci.createDatatake(restDatatake);
			
			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "CreateDatatakeTests.createReturnsBadRequestOnInvalidInput");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#createDatatake(de.dlr.proseo.dtmgr.rest.model.RestDatatake)}.
		 */
		@Test
		@DisplayName("create returns 403 FORBIDDEN on cross-mission access")
		final void createReturnsForbiddenOnCrossMissionAccess() {
			
			createTestMission(testMissionData2);
			
			RestDatatake restDatatake = new RestDatatake();
			restDatatake.setMissionCode(testMissionData2[0]); 
			restDatatake.setSpacecraftCode(TEST_SC_CODE);
			restDatatake.setPayloadName(TEST_PAYLOAD);
			// restDatatake.setDatatakeId(TEST_DATATAKE_ID); // <== MANDATORY DATA MISSING
			restDatatake.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
			restDatatake.setDatatakeDuration(TEST_DURATION.toMillis());
			restDatatake.setIsComplete(false);
			restDatatake.getParameters().add(new RestParameter(TEST_PARAM_KEY, ParameterType.STRING.toString(), TEST_PARAM_VALUE));
			
			RestDataSegment restDataSegment = new RestDataSegment();
			restDataSegment.setMissionCode(testMissionData[0]);
			restDataSegment.setSpacecraftCode(TEST_SC_CODE);
			restDataSegment.setPayloadName(TEST_PAYLOAD);
			restDataSegment.setDatatakeId(TEST_DATATAKE_ID);
			restDataSegment.setSegmentNumber(1L);
			restDataSegment.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
			restDataSegment.setSegmentDuration(TEST_DURATION.toMillis());
			
			restDatatake.getDataSegments().add(restDataSegment);
			
			ResponseEntity<RestDatatake> response = dci.createDatatake(restDatatake);
			
			assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "CreateDatatakeTests.createReturnsForbiddenOnCrossMissionAccess");
		}

	}

	// ------------------------------------------------------------------
	// countDatatakes
	// ------------------------------------------------------------------

	@Nested
	@DisplayName("countDatatakes(...)")
	class CountDatatakesTests {

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#countDatatakes(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
		 */
		@Test
		@DisplayName("count returns 200 OK with the number of datatakes")
		final void testCountDatatakes() {
			createTestDatatake(testMissionData[0]);

			ResponseEntity<?> response =
					dci.countDatatakes(TEST_SC_CODE, TEST_PAYLOAD, null, START_TIME_FROM, START_TIME_TO);

			assertEquals(HttpStatus.OK, response.getStatusCode(), "Unexpected HTTP status code");
			assertTrue(response.hasBody(), "Response body missing");
			assertEquals(1, response.getBody().toString(), "Unexpected number of datatakes returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "CountDatatakesTests.testCountDatatakes");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#countDatatakes(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
		 */
		@Test
		@DisplayName("count returns 404 NOT_FOUND due to invalid search criteria")
		final void countReturnsNotFoundOnInvalidInput() {
			createTestDatatake(testMissionData[0]);

			ResponseEntity<?> response =
					dci.countDatatakes(TEST_SC_CODE, TEST_PAYLOAD, TEST_DATATAKE_ID_INVALID, START_TIME_FROM, START_TIME_TO);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "CountDatatakesTests.countReturnsNotFoundOnInvalidInput");
		}
	}

	// ------------------------------------------------------------------
	// getDatatakeById
	// ------------------------------------------------------------------

	@Nested
	@DisplayName("getDatatakesById(...)")
	class GetDatatakeByIddTests {

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#getDatatakeById(java.lang.Long)}.
		 */
		@Test
		@DisplayName("getById returns 200 OK with the requested datatake")
		final void testGetDatatakeById() {
			long datatakeDbid = createTestDatatake(testMissionData[0]);
			
			ResponseEntity<RestDatatake> response = dci.getDatatakeById(datatakeDbid);

			assertEquals(HttpStatus.OK, response.getStatusCode(),  "Unexpected HTTP status code");
			assertTrue(response.hasBody(), "Response body missing");
			assertEquals(datatakeDbid, response.getBody().getId(), "Database ID mismatch");

			logger.log(GeneralMessage.UNIT_TEST_OK, "GetDatatakeByIddTests.testGetDatatakeById");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#getDatatakeById(java.lang.Long)}.
		 */
		@Test
		@DisplayName("getById returns 404 NOT_FOUND due to invalid database ID")
		final void byidReturnsNotFoundOnInvalidId() {
			createTestDatatake(testMissionData[0]);
			
			ResponseEntity<RestDatatake> response = dci.getDatatakeById(DATATAKE_DB_ID_INVALID);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "GetDatatakeByIddTests.byidReturnsNotFoundOnInvalidId");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#getDatatakeById(java.lang.Long)}.
		 */
		@Test
		@DisplayName("getById returns 403 FORBIDDEN due to cross-mission access")
		final void byidReturnsForbiddenOnSecurityException() {
			createTestMission(testMissionData2);
			long datatakeDbid = createTestDatatake(testMissionData2[0]);
			
			ResponseEntity<RestDatatake> response = dci.getDatatakeById(datatakeDbid);

			assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "GetDatatakeByIddTests.byidReturnsForbiddenOnSecurityException");
		}
	}

	// ------------------------------------------------------------------
	// deleteDatatakeById
	// ------------------------------------------------------------------

	@Nested
	@DisplayName("deleteDatatakeById(...)")
	class DeleteDatatakeByIdTests {

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#deleteDatatakeById(java.lang.Long)}.
		 */
		@Test
		@DisplayName("delete returns 204 NO_CONTENT with empty body")
		final void testDeleteDatatakeById() {
			long datatakeDbid = createTestDatatake(testMissionData[0]);
			
			ResponseEntity<?> response = dci.deleteDatatakeById(datatakeDbid);

			assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "DeleteDatatakeByIdTests.testDeleteDatatakeById");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#deleteDatatakeById(java.lang.Long)}.
		 */
		@Test
		@DisplayName("delete returns 404 NOT_FOUND on invalid database ID")
		final void deleteReturnsNotFoundOnInvalidInput() {
			ResponseEntity<?> response = dci.deleteDatatakeById(DATATAKE_DB_ID_INVALID);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "DeleteDatatakeByIdTests.deleteReturnsNotFoundOnInvalidInput");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#deleteDatatakeById(java.lang.Long)}.
		 */
		@Test
		@DisplayName("delete returns 403 FORBIDDEN on cross-mission access")
		final void deleteReturnsForbiddenOnSecurityException() {
			createTestMission(testMissionData2);
			long datatakeDbid = createTestDatatake(testMissionData2[0]);

			ResponseEntity<?> response = dci.deleteDatatakeById(datatakeDbid);

			assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),  "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");

			logger.log(GeneralMessage.UNIT_TEST_OK, "DeleteDatatakeByIdTests.deleteReturnsForbiddenOnSecurityException");
		}
	}

	// ------------------------------------------------------------------
	// modifyDatatake
	// ------------------------------------------------------------------

	@Nested
	@DisplayName("modifyDatatake(...)")
	class ModifyDatatakeTests {

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#modifyDatatake(java.lang.Long, de.dlr.proseo.dtmgr.rest.model.RestDatatake)}.
		 */
		@Test
		@DisplayName("modify returns 200 OK with the updated datatake")
		final void testModifyDatatake() {
			long datatakeDbid = createTestDatatake(testMissionData[0]);
			
			RestDatatake restDatatake = DatatakeUtil.toRestDatatake(
					RepositoryService.getDatatakeRepository().findById(datatakeDbid).get());
			
			// Set datatake attributes
			restDatatake.setIsComplete(true);
			String deliveryStopTime = OrbitTimeFormatter.format(TEST_START_TIME.plus(TEST_DURATION).plus(Duration.ofMinutes(5L)));
			restDatatake.setDeliveryStopTime(deliveryStopTime);
			
			// Add another data segment
			RestDataSegment restDataSegment = new RestDataSegment();
			restDataSegment.setMissionCode(restDatatake.getMissionCode());
			restDataSegment.setSpacecraftCode(restDatatake.getSpacecraftCode());
			restDataSegment.setPayloadName(restDatatake.getPayloadName());
			restDataSegment.setDatatakeId(restDatatake.getDatatakeId());
			restDataSegment.setSegmentNumber(3L);
			restDataSegment.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME.plus(TEST_DURATION)));
			restDataSegment.setSegmentDuration(TEST_DURATION.dividedBy(2).toMillis());
			
			restDatatake.getDataSegments().add(restDataSegment);
			
			ResponseEntity<RestDatatake> response = dci.modifyDatatake(datatakeDbid, restDatatake);
			
			assertEquals(HttpStatus.OK, response.getStatusCode(), "Unexpected HTTP status code");
			assertTrue(response.hasBody(), "Response body missing");
			
			RestDatatake responseDatatake = response.getBody();
			assertEquals(datatakeDbid, responseDatatake.getId(), "Database ID mismatch");
			assertTrue(responseDatatake.getIsComplete(), "Datatake not complete");
			assertEquals(deliveryStopTime, responseDatatake.getDeliveryStopTime(), "Delivery stop time mismatch");
			
			assertEquals(3, responseDatatake.getDataSegments().size(), "Unexpected number of data segments");
			RestDataSegment responseSegment = responseDatatake.getDataSegments().get(2);
			assertEquals(2, responseSegment.getSegmentNumber(), "Segment number mismatch"); // Note: Segments should be ordered!
			assertEquals(TEST_DURATION.dividedBy(2).toMillis(), responseSegment.getSegmentDuration(), "Segment duration mismatch");
			
			logger.log(GeneralMessage.UNIT_TEST_OK, "ModifyDatatakeTests.testModifyDatatake");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#modifyDatatake(java.lang.Long, de.dlr.proseo.dtmgr.rest.model.RestDatatake)}.
		 */
		@Test
		@DisplayName("modify returns 400 BAD_REQUEST on unprocessable input")
		final void modifyReturnsBadRequestOnInvalidInput() {
			long datatakeDbid = createTestDatatake(testMissionData[0]);
			
			RestDatatake restDatatake = DatatakeUtil.toRestDatatake(
					RepositoryService.getDatatakeRepository().findById(datatakeDbid).get());
			
			// Delete mandatory attribute
			restDatatake.setSensingStartTime(null);
			
			ResponseEntity<RestDatatake> response = dci.modifyDatatake(datatakeDbid, restDatatake);
			
			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");
			
			logger.log(GeneralMessage.UNIT_TEST_OK, "ModifyDatatakeTests.modifyReturnsBadRequestOnInvalidInput");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#modifyDatatake(java.lang.Long, de.dlr.proseo.dtmgr.rest.model.RestDatatake)}.
		 */
		@Test
		@DisplayName("modify returns 404 NOT_FOUND on invalid database ID")
		final void modifyReturnsNotFoundOnInvalidInput() {
			long datatakeDbid = createTestDatatake(testMissionData[0]);
			
			RestDatatake restDatatake = DatatakeUtil.toRestDatatake(
					RepositoryService.getDatatakeRepository().findById(datatakeDbid).get());
			
			ResponseEntity<RestDatatake> response = dci.modifyDatatake(DATATAKE_DB_ID_INVALID, restDatatake);
			
			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");
			
			logger.log(GeneralMessage.UNIT_TEST_OK, "ModifyDatatakeTests.modifyReturnsNotFoundOnInvalidInput");
		}

		/**
		 * Test method for {@link de.dlr.proseo.dtmgr.rest.DatatakeControllerImpl#modifyDatatake(java.lang.Long, de.dlr.proseo.dtmgr.rest.model.RestDatatake)}.
		 */
		@Test
		@DisplayName("modify returns 403 FORBIDDEN on cross-mission access")
		final void modifyReturnsForbiddenOnSecurityException() {
			createTestMission(testMissionData2);
			long datatakeDbid = createTestDatatake(testMissionData2[0]);
			
			RestDatatake restDatatake = DatatakeUtil.toRestDatatake(
					RepositoryService.getDatatakeRepository().findById(datatakeDbid).get());
			
			ResponseEntity<RestDatatake> response = dci.modifyDatatake(datatakeDbid, restDatatake);
			
			assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Unexpected HTTP status code");
			assertFalse(response.hasBody(), "Unexpected body returned");
			
			logger.log(GeneralMessage.UNIT_TEST_OK, "ModifyDatatakeTests.modifyReturnsForbiddenOnSecurityException");
		}
	}

}
