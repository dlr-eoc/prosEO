/**
 * IngestorControllerTest.java
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
 * Test class for the REST API of IngestorControllerImpl
 * 
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Ingestor.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class IngestorControllerTest {
	
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
	private static Logger logger = LoggerFactory.getLogger(IngestorControllerTest.class);
	
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
	 * Test method for {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#ingestProducts(java.lang.String, java.util.List)}.
	 * 
	 * Test: Ingest a single product
	 * Precondition: Processing facility exists, product class exists, mock storage manager exists, mock production planner exists
	 */
	@Test
	public final void testIngestProducts() {
		
		// Make sure processing facility and product class exist
		
		// Create a directory with product data files
		
		// Create an IngestorProduct describing the product directory
		
		// Create mock storage manager (logging calls)
		
		// Create mock production planner (logging calls)
		
		// Perform REST API call
		
		// Check logged calls for storage manager and production planner
		
		// TODO
		logger.warn("Test not implemented for ingestProducts");

		logger.info("Test OK: Insert a single product");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#getProductFile(java.lang.Long, java.lang.String)}.
	 * 
	 * Test: Get the product file for a product at a given processing facility
	 * Precondition: Processing facility exists, product and product file exist
	 */
	@Test
	public final void testGetProducts() {
		
		// Make sure processing facility and product class exist
				
		// Make sure test products with product files exist
		List<Product> testProducts = createTestProducts();
		
		// Perform REST API call and check retrieved product file
				
		// TODO
		logger.warn("Test not implemented for getProductFile");
		
		// Clean up database
		deleteTestProducts(testProducts);

		logger.info("Test OK: Get Product File");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#ingestProductFile(java.lang.Long, java.lang.String, de.dlr.proseo.ingestor.rest.model.ProductFile)}.
	 * 
	 * Test: Ingest a product file for an existing product
	 * Precondition: Processing facility exists, product exists, mock storage manager exists, mock production planner exists
	 */
	@Test
	public final void testIngestProductFile() {
		
		// Make sure processing facility and product class exist
		
		// Make sure product exists
		
		// Create a directory with product data files
		
		// Create a ProductFile describing the product
		
		// Create mock storage manager (logging calls)
		
		// Create mock production planner (logging calls)
		
		// Perform REST API call
		
		// Check logged calls for storage manager and production planner
		
		
		// TODO
		logger.warn("Test not implemented for ingestProductFile");

		logger.info("Test OK: Insert a single product");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#deleteProductFile(java.lang.Long, java.lang.String)}.
	 * 
	 * Test: Delete a product file from the metadata database and from the storage manager
	 * Precondition: Processing facility exists, product with a product file exists, mock storage manager exists
	 */
	@Test
	public final void testDeleteProductFile() {
		
		// Make sure processing facility and product class exist
				
		// Make sure test products with product files exist
		List<Product> testProducts = createTestProducts();
		
		// Create mock storage manager (logging calls)
		
		// Perform REST API call
		
		// Check logged calls for storage manager
		
		// TODO
		logger.warn("Test not implemented for deleteProductFile");

		logger.info("Test OK: Delete Product File");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#modifyProductFile(java.lang.Long, java.lang.String, de.dlr.proseo.ingestor.rest.model.ProductFile)}.
	 * 
	 * Test: Update a product file at a given processing facility
	 * Precondition: At least one product with a known ID is in the database 
	 */
	@Test
	public final void testModifyProductFile() {
		
		// Make sure processing facility and product class exist
		
		// Make sure test products with product files exist
		List<Product> testProducts = createTestProducts();
		
		// Create mock storage manager (logging calls)
		
		// Perform REST API call
		
		// Check logged calls for storage manager
		
		// TODO
		logger.warn("Test not implemented for modifyProductFile");

		logger.info("Test OK: Modify Product File");
	}

}
