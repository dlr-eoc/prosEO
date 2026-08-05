/**
 * OrderUtilTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.service.RepositoryApplication;

/**
 * @author Ranjitha Vignesh
 *
 */
@SpringBootTest(classes = { OrderUtil.class, RepositoryApplication.class })
@Transactional
public class OrderUtilTest {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderUtilTest.class);

	/* Test orbits */
	private static String[][] testOrderData = {
			// mission_id, mission_version, mission_code,
			// mission_name,spacecraft_version,spacecraft_code,spacecraft_name, order_id,
			// order_version, execution_time, identifier, order_state, processing_mode,
			// start_time, stop_time
			{ "1", "0", "ABCe", "ABCD Testing", "1", "S_TDX1", "Tandom-X", "11", "0", "2019-10-17T22:49:21.000000",
					"XYZ", "RUNNING", "NRTI", "2019-08-29T22:49:21.000000", "2019-08-29T22:49:21.000000" } };

	/**
	 * Create an orbit from a data array
	 *
	 * @param testData an array of Strings representing the orbit to create
	 * @return a Orbit with its attributes set to the input data
	 */
	private ProcessingOrder createOrder(String[] testData) {
		logger.info("... creating order ");

		// create TestMission
		de.dlr.proseo.model.Mission testMission = new de.dlr.proseo.model.Mission();
		de.dlr.proseo.model.Spacecraft testSpacecraft = new de.dlr.proseo.model.Spacecraft();

		testMission.setId(Long.parseLong(testData[0]));
		testMission.setCode(testData[2]);
		testMission.setName(testData[3]);

		// adding Spacecraft parameters
		testSpacecraft.setMission(testMission);
		testSpacecraft.incrementVersion();
		testSpacecraft.setCode(testData[5]);
		testSpacecraft.setName(testData[6]);

		// Adding processing order parameters
		ProcessingOrder testOrder = new ProcessingOrder();
		testOrder.setId(Long.parseLong(testData[7]));
		testOrder.setExecutionTime(Instant.from(OrbitTimeFormatter.parse(testData[9])));
		testOrder.setIdentifier(testData[10]);
		testOrder.setUuid(UUID.randomUUID());
		testOrder.setOrderState(OrderState.valueOf(testData[11]));
		testOrder.setProcessingMode(testData[10]);

		testOrder.setStartTime(Instant.from(OrbitTimeFormatter.parse(testData[13])));
		testOrder.setStopTime(Instant.from(OrbitTimeFormatter.parse(testData[14])));
		testOrder.setMission(testMission);

		logger.info("Created test order {}", testOrder.getId());
		return testOrder;
	}

	@Test
	public final void test() {
		// Create an empty product
		ProcessingOrder modelOrder = new ProcessingOrder();
		RestOrder restOrder = new RestOrder();
		// restOrder = OrderUtil.toRestOrder(modelOrder);

		assertNull(restOrder.getIdentifier(), "Unexpected identifier: ");
		assertNull(restOrder.getMissionCode(), "Unexpected mission code for new order: ");
		logger.info("Test copy empty order OK");

		// Copy a model order to rest order
		modelOrder = createOrder(testOrderData[0]);
		restOrder = OrderUtil.toRestOrder(modelOrder);
		assertEquals(modelOrder.getId().longValue(), restOrder.getId().longValue(), "Unexpected ID: ");
		assertEquals(modelOrder.getMission().getCode(), restOrder.getMissionCode(), "Unexpected Mission code: ");
		assertEquals(modelOrder.getIdentifier(), restOrder.getIdentifier(), "Unexpected Identifier: ");
		assertEquals(modelOrder.getUuid().toString(), restOrder.getUuid(), "Unexpected UUID: ");
		assertEquals(modelOrder.getOrderState().toString(),
				restOrder.getOrderState().toString(),
				"Unexpected order state: ");
		logger.info("model execution time: " + modelOrder.getExecutionTime());
		logger.info("rest execution time: " + restOrder.getExecutionTime().toInstant());

		assertEquals(modelOrder.getExecutionTime(),
				restOrder.getExecutionTime().toInstant(),
				"Unexpected execution time: ");
		assertEquals(modelOrder.getStartTime(),
				Instant.from(OrbitTimeFormatter.parse(restOrder.getStartTime())),
				"Unexpected start time: ");
		assertEquals(modelOrder.getStopTime(),
				Instant.from(OrbitTimeFormatter.parse(restOrder.getStopTime())),
				"Unexpected stop time: ");

		assertEquals(modelOrder.getProcessingMode(), restOrder.getProcessingMode(), "Unexpected processing Mode: ");

//		//Validation for requestedOrbits, requestedProductClasses,requestedConfiguredProcesors,
//		//filterconditions,inputProductClasses, outputParameters

		logger.info("Test copy model to REST OK");

		// Copy a order from REST to model
		ProcessingOrder copiedModelOrder = OrderUtil.toModelOrder(restOrder);
		assertEquals(modelOrder.getId(), copiedModelOrder.getId(), "ID not preserved: ");
		assertEquals(modelOrder.getIdentifier(), copiedModelOrder.getIdentifier(), "Unexpected Identifier: ");
		assertEquals(modelOrder.getUuid(), copiedModelOrder.getUuid(), "Unexpected UUID: ");
		assertEquals(modelOrder.getOrderState(), copiedModelOrder.getOrderState(), "Unexpected order state: ");
		assertEquals(modelOrder.getExecutionTime(), copiedModelOrder.getExecutionTime(), "Unexpected execution time: ");
		assertEquals(modelOrder.getStartTime(), copiedModelOrder.getStartTime(), "Unexpected start time: ");
		assertEquals(modelOrder.getStopTime(), copiedModelOrder.getStopTime(), "Unexpected stop time: ");

		assertEquals(modelOrder.getProcessingMode(),
				copiedModelOrder.getProcessingMode(),
				"Unexpected processing Mode: ");
		assertEquals(modelOrder.getInputFilters().size(),
				copiedModelOrder.getInputFilters().size(),
				"Unexpected size of input filters: ");
		for (ProductClass productClass : modelOrder.getInputFilters().keySet()) {
			assertEquals(modelOrder.getInputFilters().get(productClass),
					copiedModelOrder.getInputFilters().get(productClass),
					"Unexpected filter conditions: ");
		}
		assertEquals(modelOrder.getClassOutputParameters().size(),
				copiedModelOrder.getClassOutputParameters().size(),
				"Unexpected size of parameterized outputs: ");
		for (ProductClass productClass : modelOrder.getClassOutputParameters().keySet()) {
			assertEquals(modelOrder.getClassOutputParameters().get(productClass),
					copiedModelOrder.getClassOutputParameters().get(productClass),
					"Unexpected output parameters: ");
		}
		logger.info("Test copy REST to model OK");
	}
}
