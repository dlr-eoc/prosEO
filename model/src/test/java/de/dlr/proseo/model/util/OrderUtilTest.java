/**
 * OrderUtilTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { OrderUtil.class, RepositoryApplication.class })
@Transactional
@AutoConfigureTestEntityManager
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

		assertNull("Unexpected identifier: ", restOrder.getIdentifier());
		assertNull("Unexpected mission code for new order: ", restOrder.getMissionCode());
		logger.info("Test copy empty order OK");

		// Copy a model order to rest order
		modelOrder = createOrder(testOrderData[0]);
		restOrder = OrderUtil.toRestOrder(modelOrder);
		assertEquals("Unexpected ID: ", modelOrder.getId().longValue(), restOrder.getId().longValue());
		assertEquals("Unexpected Mission code: ", modelOrder.getMission().getCode(), restOrder.getMissionCode());
		assertEquals("Unexpected Identifier: ", modelOrder.getIdentifier(), restOrder.getIdentifier());
		assertEquals("Unexpected UUID: ", modelOrder.getUuid().toString(), restOrder.getUuid());
		assertEquals("Unexpected order state: ", modelOrder.getOrderState().toString(),
				restOrder.getOrderState().toString());
		logger.info("model execution time: " + modelOrder.getExecutionTime());
		logger.info("rest execution time: " + restOrder.getExecutionTime().toInstant());

		assertEquals("Unexpected execution time: ", modelOrder.getExecutionTime(),
				restOrder.getExecutionTime().toInstant());
		assertEquals("Unexpected start time: ", modelOrder.getStartTime(),
				Instant.from(OrbitTimeFormatter.parse(restOrder.getStartTime())));
		assertEquals("Unexpected stop time: ", modelOrder.getStopTime(),
				Instant.from(OrbitTimeFormatter.parse(restOrder.getStopTime())));

		assertEquals("Unexpected processing Mode: ", modelOrder.getProcessingMode(), restOrder.getProcessingMode());

//		//Validation for requestedOrbits, requestedProductClasses,requestedConfiguredProcesors,
//		//filterconditions,inputProductClasses, outputParameters

		logger.info("Test copy model to REST OK");

		// Copy a order from REST to model
		ProcessingOrder copiedModelOrder = OrderUtil.toModelOrder(restOrder);
		assertEquals("ID not preserved: ", modelOrder.getId(), copiedModelOrder.getId());
		assertEquals("Unexpected Identifier: ", modelOrder.getIdentifier(), copiedModelOrder.getIdentifier());
		assertEquals("Unexpected UUID: ", modelOrder.getUuid(), copiedModelOrder.getUuid());
		assertEquals("Unexpected order state: ", modelOrder.getOrderState(), copiedModelOrder.getOrderState());
		assertEquals("Unexpected execution time: ", modelOrder.getExecutionTime(), copiedModelOrder.getExecutionTime());
		assertEquals("Unexpected start time: ", modelOrder.getStartTime(), copiedModelOrder.getStartTime());
		assertEquals("Unexpected stop time: ", modelOrder.getStopTime(), copiedModelOrder.getStopTime());

		assertEquals("Unexpected processing Mode: ", modelOrder.getProcessingMode(),
				copiedModelOrder.getProcessingMode());
		assertEquals("Unexpected size of input filters: ", modelOrder.getInputFilters().size(),
				copiedModelOrder.getInputFilters().size());
		for (ProductClass productClass : modelOrder.getInputFilters().keySet()) {
			assertEquals("Unexpected filter conditions: ", modelOrder.getInputFilters().get(productClass),
					copiedModelOrder.getInputFilters().get(productClass));
		}
		assertEquals("Unexpected size of parameterized outputs: ", modelOrder.getClassOutputParameters().size(),
				copiedModelOrder.getClassOutputParameters().size());
		for (ProductClass productClass : modelOrder.getClassOutputParameters().keySet()) {
			assertEquals("Unexpected output parameters: ", modelOrder.getClassOutputParameters().get(productClass),
					copiedModelOrder.getClassOutputParameters().get(productClass));
		}
		logger.info("Test copy REST to model OK");
	}
}
