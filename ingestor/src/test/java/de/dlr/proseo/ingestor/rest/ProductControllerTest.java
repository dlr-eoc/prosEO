/**
 * ProductControllerTest.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

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
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IngestorApplication.class)
@WithMockUser(username = "UTM-testuser", password = "password")
@Transactional
@AutoConfigureTestEntityManager
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
	private static int WIREMOCK_PORT = 8080;
	private static WireMockServer wireMockServer;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductControllerTest.class);

	/**
	 * Prepare the test environment
	 *
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		wireMockServer = new WireMockServer(WIREMOCK_PORT);
		wireMockServer.start();

		wireMockServer
				.stubFor(WireMock.get(WireMock.urlEqualTo("/planner/semaphore/acquire")).willReturn(WireMock.aResponse()
						.withStatus(200).withHeader("Content-Type", "application/json").withBody("{}")));

		wireMockServer
				.stubFor(WireMock.get(WireMock.urlEqualTo("/planner/semaphore/release")).willReturn(WireMock.aResponse()
						.withStatus(200).withHeader("Content-Type", "application/json").withBody("{}")));
		
//		wireMockServer.getStubMappings().forEach(s -> logger.trace("Stub mapping: " + s));

	}

	/**
	 * Clean up the test environment
	 *
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		wireMockServer.stop();
	}

	/**
	 * Before every test: NOP (cannot use JPA here)
	 *
	 * @throws java.lang.Exception
	 */
	@Before
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
	@After
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
		
		jdbcTemplate.execute("RUNSCRIPT FROM '" + "classpath:create_view_product_processing_facilities.sql" + "'");

		// Get a test product from the database
		Product testProduct = RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0]))
				.get();

		// Delete the test product with the product controller
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<?> response = pci.deleteProductById(testProduct.getId(), testHeader);

		// Check that the deletion was successful
		assertEquals("Unexpected HTTP status code: ", HttpStatus.NO_CONTENT, response.getStatusCode());
		assertTrue("Product was not deleted.",
				RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0])).isEmpty());
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
				null, null, null, null, null, null);

		assertEquals("Unexpected HTTP status code: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Unexpected number of results: ", testProductData.length, response.getBody().size());
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
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, response.getStatusCode());
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
		assertEquals("Unexpected HTTP status code: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong product UUID retrieved: ", testProduct.getUuid().toString(), response.getBody().getUuid());
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
		assertEquals("Unexpected HTTP status code: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Modification unsuccessful: ", modifiedProduct.getMode(), response.getBody().getMode());
	}

}
