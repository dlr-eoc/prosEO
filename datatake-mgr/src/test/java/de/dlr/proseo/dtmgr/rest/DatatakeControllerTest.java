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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.dtmgr.DatatakeManagerApplication;
import de.dlr.proseo.dtmgr.rest.model.RestDatatake;
import de.dlr.proseo.logging.logger.ProseoLogger;
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

	private static final Long DATATAKE_DB_ID = 42L;
	private static final Long DATATAKE_DB_ID_INVALID = 421L;
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


	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DatatakeControllerTest.class);

	/** The datatake controller under test */
	@Autowired
	DatatakeController controller;

	/** Database interface */
	@Autowired
	private EntityManager em;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeAll() throws Exception {
		logger.trace(">>> Starting to create test data in the database");

		fillDatabase();

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
	 * Filling the database with some initial data for testing purposes
	 *
	 * @param mission the mission to be referenced by the data filled in the
	 *                database
	 */
	private static void fillDatabase() {

		logger.trace("... creating mission {}", testMissionData[0]);

		Mission mission = new Mission();
		mission.setCode(testMissionData[0]);
		mission.setName(testMissionData[1]);
		mission.getProcessingModes().add(testMissionData[2]);
		mission.getFileClasses().add(testMissionData[3]);
		mission.setProductFileTemplate(testMissionData[4]);
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

	private void createDatatake() {

		Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByMissionAndCode(testMissionData[0], TEST_SC_CODE);

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
		@DisplayName("returns 200 OK with the datatake list from the manager")
		void returnsOkWithListFromManager() {
			createDatatake();

			ResponseEntity<List<RestDatatake>> response =
					controller.getDatatakes(testMissionData[0], TEST_SC_CODE, null, START_TIME_FROM, START_TIME_TO,
							RECORD_FROM, RECORD_TO, ORDER_BY);

			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertEquals(1, response.getBody().size());
		}

		@Test
		@DisplayName("passes null for all optional criteria when no filter is given")
		void passesNullCriteriaToManager() {
			createDatatake();

			ResponseEntity<List<RestDatatake>> response =
					controller.getDatatakes(null, null, null, null, null, null, null, null);

			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertEquals(1, response.getBody().size());
		}

		@Test
		@DisplayName("returns 400 BAD_REQUEST with error headers on IllegalArgumentException")
		void returnsBadRequestOnIllegalArgumentException() {
			createDatatake();

			// wrong time interval selection
			ResponseEntity<List<RestDatatake>> response = controller.getDatatakes(
					TEST_SC_CODE, TEST_PAYLOAD, TEST_DATATAKE_ID, START_TIME_TO, START_TIME_FROM,
					RECORD_FROM, RECORD_TO, ORDER_BY);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			assertInstanceOf(HttpHeaders.class, response.getBody());

			// wrong record number selection
			response = controller.getDatatakes(
					TEST_SC_CODE, TEST_PAYLOAD, TEST_DATATAKE_ID, START_TIME_FROM, START_TIME_TO,
					RECORD_TO, RECORD_FROM, ORDER_BY);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			assertInstanceOf(HttpHeaders.class, response.getBody());
		}

		@Test
		@DisplayName("returns 404 NOT_FOUND with error headers on EntityNotFoundException")
		void returnsNotFoundOnEntityNotFoundException() {
			ResponseEntity<List<RestDatatake>> response = controller.getDatatakes(
					TEST_SC_CODE, TEST_PAYLOAD, TEST_DATATAKE_ID_INVALID, null, null,
					null, null, null);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
			assertInstanceOf(HttpHeaders.class, response.getBody());
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
		@DisplayName("returns 200 OK with the updated datatake")
		void returnsOkWithUpdatedDatatake() {
			RestDatatake expected = new RestDatatake();

			ResponseEntity<RestDatatake> response = controller.markProcessed(DATATAKE_DB_ID);

			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertSame(expected, response.getBody());
		}

		@Test
		@DisplayName("returns 404 NOT_FOUND with error headers when the datatake does not exist")
		void returnsNotFoundOnEntityNotFoundException() {
			ResponseEntity<RestDatatake> response = controller.markProcessed(DATATAKE_DB_ID_INVALID);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
			assertInstanceOf(HttpHeaders.class, response.getBody());
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
		final void testCreateDatatake() {
			fail("Not yet implemented"); // TODO
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
		final void testCountDatatakes() {
			fail("Not yet implemented"); // TODO
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
		final void testGetDatatakeById() {
			fail("Not yet implemented"); // TODO
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
		final void testDeleteDatatakeById() {
			fail("Not yet implemented"); // TODO
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
		final void testModifyDatatake() {
			fail("Not yet implemented"); // TODO
		}
	}

}
