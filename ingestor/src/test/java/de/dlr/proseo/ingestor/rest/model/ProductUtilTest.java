/**
 * ProductUtilTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest.model;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.ingestor.rest.ProductControllerTest;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Parameter.ParameterType;

/**
 * @author thomas
 *
 */
public class ProductUtilTest {

	/* Test products */
	private static String[][] testProductData = {
		// id, version, mission code, product class, mode, sensing start, sensing stop, revision (parameter)
		{ "7", "1", "S5P", "L1B", "NRTI", "2019-08-29T22:49:21.074395", "2019-08-30T00:19:33.946628", "01" },
		{ "8", "1", "S5P", "L1B", "NRTI", "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "01" },
		{ "9", "1", "TDM", "DEM", null, "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "02" }
	};

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductControllerTest.class);
	
	/**
	 * Create a product from a data array
	 * 
	 * @param testData an array of Strings representing the product to create
	 * @return a Product with its attributes set to the input data
	 */
	private Product createProduct(String[] testData) {
		Product testProduct = new Product();
		
		testProduct.setId(Long.parseLong(testData[0]));
		while (testProduct.getVersion() < Integer.parseInt(testData[1])) {
			testProduct.incrementVersion();
		}
		
		Mission testMission = new Mission();
		testMission.setCode(testData[2]);
		ProductClass testProductClass = new ProductClass();
		testProductClass.setMission(testMission);
		testProductClass.setProductType(testData[3]);

		logger.info("... creating product with product type {}", (null == testProduct.getProductClass() ? null : testProduct.getProductClass().getProductType()));
		testProduct.setMode(testData[4]);
		testProduct.setSensingStartTime(Instant.from(Orbit.orbitTimeFormatter.parse(testData[5])));
		testProduct.setSensingStopTime(Instant.from(Orbit.orbitTimeFormatter.parse(testData[6])));
		testProduct.getParameters().put(
				"revision", new Parameter().init(ParameterType.INTEGER, Integer.parseInt(testData[7])));
		
		logger.info("Created test product {}", testProduct.getId());
		return testProduct;
	}
	
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
		// Create an empty product
		Product modelProduct = new Product();
		de.dlr.proseo.ingestor.rest.model.Product restProduct = ProductUtil.toRestProduct(modelProduct);
		assertEquals("Unexpected version number for new product: ", 1L, restProduct.getVersion().longValue());
		assertNull("Unexpected mode for new product: ", restProduct.getMode());
		logger.info("Test copy empty product OK");
		
		// Copy a product from model to REST
		modelProduct = createProduct(testProductData[0]);
		restProduct = ProductUtil.toRestProduct(modelProduct);
		assertEquals("Unexpected ID: ", modelProduct.getId(), restProduct.getId().longValue());
		assertEquals("Unexpected version: ", modelProduct.getVersion(), restProduct.getVersion().longValue());
		assertEquals("Unexpected mission code: ", modelProduct.getProductClass().getMission().getCode(),
				restProduct.getMissionCode());
		assertEquals("Unexpected product type: ", modelProduct.getProductClass().getProductType(),
				restProduct.getProductClass());
		assertEquals("Unexpected mode: ", modelProduct.getMode(), restProduct.getMode());
		assertEquals("Unexpected sensing start: ", Orbit.orbitTimeFormatter.format(modelProduct.getSensingStartTime()),
				restProduct.getSensingStartTime());
		assertEquals("Unexpected sensing stop: ", Orbit.orbitTimeFormatter.format(modelProduct.getSensingStopTime()),
				restProduct.getSensingStopTime());
		assertEquals("Unexpected number of parameters", modelProduct.getParameters().size(), restProduct.getParameters().size());
		for (int i = 0; i < modelProduct.getParameters().size(); ++i) {
			de.dlr.proseo.ingestor.rest.model.Parameter restParameter = restProduct.getParameters().get(i);
			String restKey = restParameter.getKey();
			assertTrue("Unexpected parameter key in parameter " + i + ": ",
					modelProduct.getParameters().containsKey(restKey));
			assertEquals("Unexpected parameter type in parameter " + i + ": ", 
					modelProduct.getParameters().get(restKey).getParameterType(), restParameter.getParameterType());
			assertEquals("Unexpected parameter value in parameter " + i + ": ", 
					modelProduct.getParameters().get(restKey).getParameterValue(), restParameter.getParameterValue());
		}
		logger.info("Test copy model to REST OK");
		
		// Copy a product from REST to model
		Product copiedModelProduct = ProductUtil.toModelProduct(restProduct);
		assertEquals("ID not preserved: ", modelProduct.getId(), copiedModelProduct.getId());
		assertEquals("Version not preserved: ", modelProduct.getVersion(), copiedModelProduct.getVersion());
		assertEquals("Mode not preserved: ", modelProduct.getMode(), copiedModelProduct.getMode());
		assertEquals("Start time not preserved: ", modelProduct.getSensingStartTime(), copiedModelProduct.getSensingStartTime());
		assertEquals("Stop time not preserved: ", modelProduct.getSensingStopTime(), copiedModelProduct.getSensingStopTime());
		assertEquals("Number of parameters not preserved: ", modelProduct.getParameters().size(), copiedModelProduct.getParameters().size());
		for (String modelKey: modelProduct.getParameters().keySet()) {
			assertTrue("Parameter " + modelKey + " not preserved: ", copiedModelProduct.getParameters().containsKey(modelKey));
			assertEquals("Parameter type not preserved for key " + modelKey + ": ", 
					modelProduct.getParameters().get(modelKey).getParameterType(),
					copiedModelProduct.getParameters().get(modelKey).getParameterType());
			assertEquals("Parameter value not preserved for key " + modelKey + ": ", 
					modelProduct.getParameters().get(modelKey).getParameterValue(),
					copiedModelProduct.getParameters().get(modelKey).getParameterValue());
		}
		logger.info("Test copy REST to model OK");
	}

}
