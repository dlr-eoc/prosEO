/**
 * ProductControllerTest.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import com.github.tomakehurst.wiremock.WireMockServer;
import de.dlr.proseo.ingestor.IngestorApplication;
import de.dlr.proseo.ingestor.IngestorTestConfiguration;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.ProductVisibility;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Test class for the REST API of ProductControllerImpl
 *
 * @author Dr. Thomas Bassler
 * @author Katharina Bassler
 */

@SpringBootTest(classes = IngestorApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@WithMockUser(username = "UTM-testuser", password = "password")
@Transactional

public class ProductControllerTest {

	/* Test products */
	private static final String TEST_CODE = "UTM";
	private static final String TEST_PRODUCT_TYPE = "L1B_______";
	private static final String TEST_FILE_CLASS = "OPER";
	private static final String TEST_MODE = "NRTI";
	private static final String TEST_MODE_2 = "OFFL";

	private static String[][] testProductData = {
			// id, version, sensing start, sensing stop, generation, revision (parameter)
			{ "0", "1", "2019-08-29T22:49:21.074395", "2019-08-30T00:19:33.946628", "2019-10-05T10:12:39.000000",
					"01" },
			{ "0", "1", "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "2019-10-05T10:13:22.000000",
					"01" },
			{ "0", "1", "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "2019-10-05T10:13:22.000000",
					"02" } };

	/** Test configuration */
	@Autowired
	IngestorTestConfiguration config;

	/** The product controller under test */
	@Autowired
	private ProductControllerImpl pci;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** Mocking the storage manager and planner */
	private static WireMockServer wireMockServer;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductControllerTest.class);

	/**
	 * Prepare the test environment
	 *
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		wireMockServer = new WireMockServer();
		wireMockServer.start();

	}

	/**
	 * Clean up the test environment
	 *
	 * @throws java.lang.Exception
	 */
	@AfterAll
	public static void tearDownAfterClass() throws Exception {
		wireMockServer.stop();
	}

	/**
	 * Before every test: Create required data environment (mission, product class etc.)
	 * 
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		Mission mission = new Mission();
		mission.setCode(TEST_CODE);
		mission.getProcessingModes().add(TEST_MODE);
		mission.getProcessingModes().add(TEST_MODE_2);
		mission.getFileClasses().add(TEST_FILE_CLASS);
		mission = RepositoryService.getMissionRepository().save(mission);
		logger.trace("Using mission " + mission.getCode() + " with id " + mission.getId());

		ProductClass prodClass = new ProductClass();
		prodClass.setMission(mission);
		prodClass.setProductType(TEST_PRODUCT_TYPE);
		prodClass.setVisibility(ProductVisibility.PUBLIC);
		prodClass = RepositoryService.getProductClassRepository().save(prodClass);
		mission.getProductClasses().add(prodClass);
		mission = RepositoryService.getMissionRepository().save(mission);
		logger.trace("Using product class " + prodClass.getProductType() + " with id " + prodClass.getId());

		logger.trace("Creating test products");
		Product testProduct;

		for (String[] testData : testProductData) {
			testProduct = new Product();
			
			testProduct.setProductClass(prodClass);
			testProduct.setUuid(UUID.randomUUID());
			testProduct.setFileClass(TEST_FILE_CLASS);
			testProduct.setMode(TEST_MODE);
			testProduct.setSensingStartTime(Instant.from(OrbitTimeFormatter.parse(testData[2])));
			testProduct.setSensingStopTime(Instant.from(OrbitTimeFormatter.parse(testData[3])));
			testProduct.setGenerationTime(Instant.from(OrbitTimeFormatter.parse(testData[4])));
			testProduct.getParameters().put("revision",
					new Parameter().init(ParameterType.INTEGER, Integer.parseInt(testData[5])));
			testProduct = RepositoryService.getProductRepository().save(testProduct);

			testData[0] = testProduct.getId() + "";

			logger.trace("Created test product {}", testProduct.getId());
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
		RepositoryService.getProductRepository().deleteAll();
		RepositoryService.getProductClassRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#deleteProductById(java.lang.Long)}.
	 *
	 * Test: Delete a product by ID Precondition: A product in the database
	 */
	@Test
	public final void testDeleteProductById() {
		logger.trace(">>> testDeleteProductById()");
		
		// Get a test product from the database
		Product testProduct = RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0]))
				.get();

		// Delete the test product with the product controller
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<?> response = pci.deleteProductById(testProduct.getId(), testHeader);

		// Check that the deletion was successful
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "Unexpected HTTP status code: ");
		assertTrue(RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0])).isEmpty(), "Product was not deleted.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#getProducts(java.lang.String, java.lang.String[], java.util.Date, java.util.Date)}.
	 *
	 * Test: List of all products by mission, product class, start time range
	 * Precondition: For all selection criteria products within and without a search
	 * value exist
	 */
	@Test
	public final void testGetProducts() {
		logger.trace(">>> testGetProducts()");

		//
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<List<RestProduct>> response = pci.getProducts(null, null, null, null, null, null, null, null,
				null, null, null, false, null, null, testHeader);

		assertEquals(HttpStatus.OK, response.getStatusCode(), "Unexpected HTTP status code: ");
		assertEquals(testProductData.length, response.getBody().size(), "Unexpected number of results: ");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#createProduct(RestProduct)}.
	 *
	 * Test: Create a new product Precondition: A (mockup) Production Planner
	 * exists, which can be informed of the new product
	 */
	@Test
	public final void testCreateProduct() {
		logger.trace(">>> testCreateProduct()");

		// Get a test product from the database
		RestProduct testProduct = ProductUtil.toRestProduct(
				RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0])).get());

		// Remove the product from the database
		RepositoryService.getProductRepository().deleteById(testProduct.getId());

		// Create the test product with the product controller
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<RestProduct> response = pci.createProduct(testProduct, testHeader);

		// Check that the creation was successful
		assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Unexpected HTTP status code: ");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#getProductById(java.lang.Long)}.
	 *
	 * Test: Get a product by ID Precondition: At least one product with a known ID
	 * is in the database
	 */
	@Test
	public final void testGetProductById() {
		logger.trace(">>> testGetProductById()");

		// Get a test product from the database
		Product testProduct = RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0]))
				.get();

		// Retrieve the test product with the product controller
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<RestProduct> response = pci.getProductById(testProduct.getId(), testHeader);

		// Check that the product was retrieved correctly
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Unexpected HTTP status code: ");
		assertEquals(testProduct.getUuid().toString(), response.getBody().getUuid(), "Wrong product UUID retrieved: ");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.ProductControllerImpl#modifyProduct(java.lang.Long, RestProduct)}.
	 *
	 * Test: Update a product by ID Precondition: At least one product with a known
	 * ID is in the database
	 */
	@Test
	public final void testModifyProduct() {
		logger.trace(">>> testModifyProduct()");

		// Get a test product from the database
		RestProduct testProduct = ProductUtil.toRestProduct(
				RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0])).get());

		// Make a test modification
		RestProduct modifiedProduct = ProductUtil.toRestProduct(
				RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0])).get());
		modifiedProduct.setMode(TEST_MODE_2);

		// Modify the test product with the product controller
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<RestProduct> response = pci.modifyProduct(testProduct.getId(), modifiedProduct, testHeader);

		// Check that the modification was successful
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Unexpected HTTP status code: ");
		assertEquals(modifiedProduct.getMode(), response.getBody().getMode(), "Modification unsuccessful: ");
	}

}
