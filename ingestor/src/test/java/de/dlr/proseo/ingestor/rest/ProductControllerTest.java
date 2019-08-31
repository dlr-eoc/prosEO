/**
 * ProductControllerTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.ingestor.Ingestor;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.Product.ParameterType;
import de.dlr.proseo.model.dao.ProductClassRepository;
import de.dlr.proseo.model.dao.ProductRepository;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Test class for the REST API of ProductControllerImpl
 * 
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Ingestor.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
//@Transactional
@AutoConfigureTestEntityManager
public class ProductControllerTest {
	
	/* Test products */
	private static String[][] testProductData = {
		// id, version, mission code, product class, mode, sensing start, sensing stop, revision (parameter)
		{ "0", "1", "S5P", "L1B", "NRTI", "2019-08-29T22:49:21.074395", "2019-08-30T00:19:33.946628", "01" },
		{ "0", "1", "S5P", "L1B", "NRTI", "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "01" },
		{ "0", "1", "TDM", "DEM", null, "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "02" }
	};
	private static List<Product> testProducts = new ArrayList<>();

	/** The security environment for this test */
	@Autowired
	private SecurityProperties security;

	/** The (random) port on which the Ingestor was started */
	@LocalServerPort
	private int port;
	
	/** The DAO for the ProductClass class */
	ProductClassRepository productClasses = RepositoryService.getProductClassRepository();
	/** The DAO for the Product class */
	ProductRepository products = RepositoryService.getProductRepository();

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductControllerTest.class);
	
	/**
	 * Get the generated test password
	 * @return the password to use for login
	 */
	private String getPassword() {
		return this.security.getUser().getPassword();
	}

	/**
	 * Prepare the test environment
	 * 
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Clean up the test environment
	 * 
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Before every test: NOP (cannot use JPA here)
	 * 
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * After every test: NOP (cannot use JPA here)
	 * 
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	/**
	 * Create test products in the database
	 */
	private void createTestProducts() {
		logger.info("Creating test products");
		for (int i = 0; i < testProductData.length; ++i) {
			Product testProduct = new Product();
			testProduct.setProductClass(
					productClasses.findByMissionCodeAndProductType(testProductData[i][2], testProductData[i][3]));
			logger.info("... creating product with product type {}", (null == testProduct.getProductClass() ? null : testProduct.getProductClass().getProductType()));
			testProduct.setMode(testProductData[i][4]);
			testProduct.setSensingStartTime(Instant.from(Orbit.orbitTimeFormatter.parse(testProductData[i][5])));
			testProduct.setSensingStopTime(Instant.from(Orbit.orbitTimeFormatter.parse(testProductData[i][6])));
			testProduct.getParameters().put(
					"revision", new Parameter().init(ParameterType.INTEGER, Integer.parseInt(testProductData[i][7])));
			testProduct = products.save(testProduct);
			logger.info("Created test product {}", testProduct.getId());
			testProducts.add(testProduct);
		}
	}
	
	/**
	 * Remove all (remaining) test products
	 */
	private void deleteTestProducts() {
		for (Product testProduct: testProducts) {
			products.delete(testProduct);
		}
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#deleteProductById(java.lang.Long)}.
	 * 
	 * Test: Delete a product by ID
	 * Precondition: A product in the database
	 */
	@Test
	public final void testDeleteProductById() {
		fail("Not yet implemented"); // TODO
		
		// Delete the first test product
		
		// Test that the product is gone
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#getProducts(java.lang.String, java.lang.String[], java.util.Date, java.util.Date)}.
	 * 
	 * Test: List of all products by mission, product class, start time range
	 * Precondition: For all selection criteria products within and without a search value exist
	 */
	@Test
	public final void testGetProducts() {
		// Make sure test products exist
		createTestProducts();
		
		// Get products using different selection criteria (also combined)
		String testUrl = "http://localhost:" + this.port + "/proseo/ingestor/v0.1/products";
		logger.info("Testing URL {}", testUrl);
		
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = new TestRestTemplate("user", getPassword())
				.getForEntity(testUrl, List.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		
		// Test that the correct products provided above are in the results
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> body = entity.getBody();
		logger.info("Found {} products", body.size());
		
		boolean[] productFound = new boolean[testProducts.size()];
		Arrays.fill(productFound, false);
		for (Map<String, Object> product: body) {
			// Check, if any of the test products was returned
			long productId = (Integer) product.get("id");
			logger.info("... found product with ID {}", productId);
			for (int i = 0; i < testProducts.size(); ++i) {
				Product testProduct = testProducts.get(i);
				if (productId == testProduct.getId()) {
					productFound[i] = true;
					assertEquals("Wrong product class for test product " + i, testProduct.getProductClass().getProductType(), product.get("productClass"));
					assertEquals("Wrong mode for test product " + i, testProduct.getMode(), product.get("mode"));
					assertEquals("Wrong start time for test product " + i,
							testProduct.getSensingStartTime(), Instant.from(Orbit.orbitTimeFormatter.parse((String) product.get("sensingStartTime"))));
					assertEquals("Wrong stop time for test product " + i,
							testProduct.getSensingStopTime(), Instant.from(Orbit.orbitTimeFormatter.parse((String) product.get("sensingStopTime"))));
				}
			}
		}
		boolean[] expectedProductFound = new boolean[testProducts.size()];
		Arrays.fill(expectedProductFound, true);
		assertArrayEquals("Not all products found", expectedProductFound, productFound);
		
		// TODO Tests with different selection criteria
		
		// Clean up database
		deleteTestProducts();
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#createProduct(de.dlr.proseo.ingestor.rest.model.Product)}.
	 * 
	 * Test: Create a new product
	 * Precondition: A (mockup) Production Planner exists, which can be informed of the new product
	 */
	@Test
	public final void testCreateProduct() {
		fail("Not yet implemented"); // TODO
		
		// Create a product in the database
		
		// Test that the product exists
		
		// Test that the Production Planner was informed
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#getProductById(java.lang.Long)}.
	 * 
	 * Test: Get a product by ID
	 * Precondition: At least one product with a known ID is in the database
	 */
	@Test
	public final void testGetProductById() {
		fail("Not yet implemented"); // TODO
		
		// Make sure a product exists
		
		// Test that a product can be read
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#modifyProduct(java.lang.Long, de.dlr.proseo.ingestor.rest.model.Product)}.
	 * 
	 * Test: Update a product by ID
	 * Precondition: At least one product with a known ID is in the database 
	 */
	@Test
	public final void testModifyProduct() {
		fail("Not yet implemented"); // TODO
		
		// Make sure a product exists
		
		// Update a product attribute
		
		// Test that the product attribute was changed as expected
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#updateIngestorProduct(java.lang.String, java.util.List)}.
	 * 
	 * Test: Ingest all given products into the storage manager of the given processing facility
	 * Precondition: A (mockup) storage manager is set up, and a directory with products exists
	 */
	@Test
	public final void testUpdateIngestorProduct() {
		fail("Not yet implemented"); // TODO
		
		// Ingest the products
		
		// Test that the products are available in the database
		
		// Test that the Storage Manager was informed
	}

}
