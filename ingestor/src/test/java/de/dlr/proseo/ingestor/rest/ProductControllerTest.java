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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.ingestor.Ingestor;
import de.dlr.proseo.ingestor.IngestorSecurityConfig;
import de.dlr.proseo.ingestor.IngestorTestConfiguration;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.Parameter.ParameterType;
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
	
	/* The base URI of the Ingestor */
	private static String INGESTOR_BASE_URI = "/proseo/ingestor/v0.1";
	
	/* Test products */
	private static String[][] testProductData = {
		// id, version, mission code, product class, mode, sensing start, sensing stop, revision (parameter)
		{ "0", "1", "S5P", "L1B", "NRTI", "2019-08-29T22:49:21.074395", "2019-08-30T00:19:33.946628", "01" },
		{ "0", "1", "S5P", "L1B", "NRTI", "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "01" },
		{ "0", "1", "TDM", "DEM", null, "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "02" }
	};

	/** Test configuration */
	@Autowired
	IngestorTestConfiguration config;
	
	/** The security environment for this test */
	@Autowired
	IngestorSecurityConfig ingestorSecurityConfig;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** The (random) port on which the Ingestor was started */
	@LocalServerPort
	private int port;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductControllerTest.class);
	
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
	 * Create a product from a data array
	 * 
	 * @param testData an array of Strings representing the product to create
	 * @return a Product with its attributes set to the input data
	 */
	private Product createProduct(String[] testData) {
		Product testProduct = new Product();
		
		testProduct.setProductClass(
				RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(testData[2], testData[3]));

		logger.info("... creating product with product type {}", (null == testProduct.getProductClass() ? null : testProduct.getProductClass().getProductType()));
		testProduct.setMode(testData[4]);
		testProduct.setSensingStartTime(Instant.from(Orbit.orbitTimeFormatter.parse(testData[5])));
		testProduct.setSensingStopTime(Instant.from(Orbit.orbitTimeFormatter.parse(testData[6])));
		testProduct.getParameters().put(
				"revision", new Parameter().init(ParameterType.INTEGER, Integer.parseInt(testData[7])));
		testProduct = RepositoryService.getProductRepository().save(testProduct);
		
		logger.info("Created test product {}", testProduct.getId());
		return testProduct;
	}
	
	/**
	 * Create test products in the database
	 * 
	 * @return a list of test product generated
	 */
	private List<Product> createTestProducts() {
		logger.info("Creating test products");
		List<Product> testProducts = new ArrayList<>();
		for (int i = 0; i < testProductData.length; ++i) {
			testProducts.add(createProduct(testProductData[i]));
		}
		return testProducts;
	}
	
	/**
	 * Remove all (remaining) test products
	 * 
	 * @param testProducts a list of test products to delete 
	 */
	private void deleteTestProducts(List<Product> testProducts) {
		for (Product testProduct: testProducts) {
			RepositoryService.getProductRepository().delete(testProduct);
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
		// Make sure test products exist
		List<Product> testProducts = createTestProducts();
		Product productToDelete = testProducts.get(0);
		testProducts.remove(0);
		
		// Delete the first test product
		String testUrl = "http://localhost:" + this.port + INGESTOR_BASE_URI + "/products/" + productToDelete.getId();
		logger.info("Testing URL {} / DELETE", testUrl);
		
		new TestRestTemplate(config.getUserName(), config.getUserPassword()).delete(testUrl);
		
		// Test that the product is gone
		ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, de.dlr.proseo.ingestor.rest.model.Product.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, entity.getStatusCode());
		
		// Clean up database
		deleteTestProducts(testProducts);

		logger.info("Test OK: Delete Product By ID");
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
		List<Product> testProducts = createTestProducts();
		
		// Get products using different selection criteria (also combined)
		String testUrl = "http://localhost:" + this.port + INGESTOR_BASE_URI + "/products";
		logger.info("Testing URL {} / GET, no params, with user {} and password {}", testUrl, config.getUserName(), config.getUserPassword());
		
//		@SuppressWarnings("rawtypes")
//		ResponseEntity<List> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
//				.getForEntity(testUrl, List.class);
		
		// Just as an example of how to use RestTemplate (does the same as commented code above)
		RestTemplate restTemplate = rtb.basicAuthentication(config.getUserName(), config.getUserPassword()).build();
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = restTemplate.getForEntity(testUrl, List.class);
		
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
		deleteTestProducts(testProducts);

		logger.info("Test OK: Get Products");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#createProduct(de.dlr.proseo.ingestor.rest.model.Product)}.
	 * 
	 * Test: Create a new product
	 * Precondition: A (mockup) Production Planner exists, which can be informed of the new product
	 */
	@Test
	public final void testCreateProduct() {
		// Create a product in the database
		Product productToCreate = createProduct(testProductData[0]);
		de.dlr.proseo.ingestor.rest.model.Product restProduct = ProductUtil.toRestProduct(productToCreate);

		String testUrl = "http://localhost:" + this.port + INGESTOR_BASE_URI + "/products";
		logger.info("Testing URL {} / POST", testUrl);
		
		ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> postEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.postForEntity(testUrl, restProduct, de.dlr.proseo.ingestor.rest.model.Product.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, postEntity.getStatusCode());
		restProduct = postEntity.getBody();
		assertNotEquals("Id should not be 0 (zero): ", 0L, restProduct.getId().longValue());
		assertEquals("Wrong sensing start time: ", testProductData[0][5], restProduct.getSensingStartTime());
		
		// Test that the product exists
		testUrl += "/" + restProduct.getId();
		ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, de.dlr.proseo.ingestor.rest.model.Product.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		
		// Test that the Production Planner was informed
		// TODO Using mock production planner
		
		// Clean up database
		ArrayList<Product> testProducts = new ArrayList<>();
		testProducts.add(productToCreate);
		deleteTestProducts(testProducts);

		logger.info("Test OK: Create Product");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#getProductById(java.lang.Long)}.
	 * 
	 * Test: Get a product by ID
	 * Precondition: At least one product with a known ID is in the database
	 */
	@Test
	public final void testGetProductById() {
		// Make sure test products exist
		List<Product> testProducts = createTestProducts();
		Product productToFind = testProducts.get(0);

		// Test that a product can be read
		String testUrl = "http://localhost:" + this.port + INGESTOR_BASE_URI + "/products/" + productToFind.getId();
		logger.info("Testing URL {} / GET", testUrl);

		ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, de.dlr.proseo.ingestor.rest.model.Product.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong product ID: ", productToFind.getId(), getEntity.getBody().getId().longValue());
		
		// Clean up database
		deleteTestProducts(testProducts);

		logger.info("Test OK: Get Product By ID");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#modifyProduct(java.lang.Long, de.dlr.proseo.ingestor.rest.model.Product)}.
	 * 
	 * Test: Update a product by ID
	 * Precondition: At least one product with a known ID is in the database 
	 */
	@Test
	public final void testModifyProduct() {
		// Make sure test products exist
		List<Product> testProducts = createTestProducts();
		Product productToModify = testProducts.get(0);
		
		// Update a product attribute
		productToModify.setMode("OFFL");

		de.dlr.proseo.ingestor.rest.model.Product restProduct = ProductUtil.toRestProduct(productToModify);
		
		String testUrl = "http://localhost:" + this.port + INGESTOR_BASE_URI + "/products/" + productToModify.getId();
		logger.info("Testing URL {} / PATCH", testUrl);

		restProduct = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.patchForObject(testUrl, restProduct, de.dlr.proseo.ingestor.rest.model.Product.class);
		assertNotNull("Modified product not set", restProduct);
		
		// Test that the product attribute was changed as expected
		ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, de.dlr.proseo.ingestor.rest.model.Product.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong mode: ", productToModify.getMode(), getEntity.getBody().getMode());
		
		// Clean up database
		deleteTestProducts(testProducts);

		logger.info("Test OK: Modify Product");
	}

}
