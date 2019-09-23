/**
 * IngestorControllerTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import static org.junit.Assert.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.ingestor.Ingestor;
import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.IngestorSecurityConfig;
import de.dlr.proseo.ingestor.IngestorTestConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.ProcessingFacility;
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
//@Transactional
@AutoConfigureTestEntityManager
public class IngestorControllerTest {
	

	/* The base URI of the Ingestor */
	private static String INGESTOR_BASE_URI = "/proseo/ingestor/v0.1";
	
	/* Various static test data */
	private static final String TEST_CODE = "ABC";
	private static final String TEST_PRODUCT_TYPE = "FRESCO";
	private static final String TEST_MISSION_TYPE = "L2__FRESCO_";
	private static final String TEST_NAME = "Test Facility";
	private static final String TEST_STORAGE_SYSTEM = "src/test/resources/IDA_test";
	private static final String TEST_PRODUCT_PATH_1 = "L2/2018/07/21/03982/OFFL/S5P_OFFL_L2__CLOUD__20180721T000328_20180721T000828_03982_01_010100_20180721T010233.nc";
	private static final String TEST_PRODUCT_PATH_2 = "L2/2018/07/21/03982/OFFL/S5P_OFFL_L2__FRESCO_20180721T000328_20180721T000828_03982_01_010100_20180721T010233.nc";
	private static final String TEST_SC_CODE = "XYZ";
	private static final int TEST_ORBIT_NUMBER = 4712;
	private static final Instant TEST_START_TIME = Instant.from(Orbit.orbitTimeFormatter.parse("2018-06-13T09:23:45.396521"));
	private static final String TEST_STOP_TIME_TEXT = "2018-07-21T00:08:28.000000";
	private static final String TEST_START_TIME_TEXT = "2018-07-21T00:03:28.000000";
	private static final String TEST_MODE_OFFL = "OFFL";
	
	
	/* Test products */
	private static String[][] testProductData = {
		// id, version, mission code, product class, mode, sensing start, sensing stop, revision (parameter)
		{ "0", "1", "S5P", "L1B", "NRTI", "2019-08-29T22:49:21.074395", "2019-08-30T00:19:33.946628", "01" },
		{ "0", "1", "S5P", "L1B", "NRTI", "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "01" },
		{ "0", "1", "TDM", "DEM", null, "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "02" }
	};

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
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
		Mission mission = RepositoryService.getMissionRepository().findByCode(TEST_CODE);
		if (null == mission) {
			mission = new Mission();
			mission.setCode(TEST_CODE);
			mission = RepositoryService.getMissionRepository().save(mission);
		}
		logger.info("Using mission " + mission.getCode() + " with id " + mission.getId());
		
		ProductClass prodClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(TEST_CODE, TEST_PRODUCT_TYPE);
		if (null == prodClass) {
			prodClass = new ProductClass();
			prodClass.setMission(mission);
			prodClass.setProductType(TEST_PRODUCT_TYPE);
			prodClass.setMissionType(TEST_MISSION_TYPE);
			prodClass = RepositoryService.getProductClassRepository().save(prodClass);
			mission.getProductClasses().add(prodClass);
			mission = RepositoryService.getMissionRepository().save(mission);
		}
		logger.info("Using product class " + prodClass.getProductType() + " with id " + prodClass.getId());
		
		Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(TEST_SC_CODE);
		if (null == spacecraft) {
			spacecraft = new Spacecraft();
			spacecraft.setCode(TEST_SC_CODE);
			spacecraft.setMission(mission);
			spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
			logger.info("Spacecraft " + spacecraft.getCode() + " created with id " + spacecraft.getId());
			mission.getSpacecrafts().add(spacecraft);
			mission = RepositoryService.getMissionRepository().save(mission);
		}
		logger.info("Using spacecraft " + spacecraft.getCode() + " with id " + spacecraft.getId());
		
		Orbit orbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(TEST_SC_CODE, TEST_ORBIT_NUMBER);
		if (null == orbit) {
			orbit = new Orbit();
			orbit.setSpacecraft(spacecraft);
			orbit.setOrbitNumber(TEST_ORBIT_NUMBER);
			orbit.setStartTime(TEST_START_TIME);
			orbit = RepositoryService.getOrbitRepository().save(orbit);
			spacecraft.getOrbits().add(orbit);
			spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
		}
		
		ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(TEST_NAME);
		if (null == facility) {
			facility = new ProcessingFacility();
			facility.setName(TEST_NAME);
		}
		// Make sure the following attributes are as expected	
		facility.setProcessingEngineUrl(ingestorConfig.getProductionPlannerUrl());
		facility.setStorageManagerUrl(config.getStorageManagerUrl());
		facility = RepositoryService.getFacilityRepository().save(facility);
		
		// Create a directory with product data files
		// Static: src/test/resources/IDA_test
		
		// Create an IngestorProduct describing the product directory
		IngestorProduct ingestorProduct = new IngestorProduct();
		ingestorProduct.setId(0L);
		ingestorProduct.setVersion(1L);
		ingestorProduct.setMissionCode(TEST_CODE);
		ingestorProduct.setMode(TEST_MODE_OFFL);
		de.dlr.proseo.ingestor.rest.model.Orbit restOrbit = new de.dlr.proseo.ingestor.rest.model.Orbit();
		restOrbit.setSpacecraftCode(TEST_SC_CODE);
		restOrbit.setOrbitNumber(Long.valueOf(TEST_ORBIT_NUMBER));
		ingestorProduct.setOrbit(restOrbit);
		ingestorProduct.setProductClass(TEST_PRODUCT_TYPE);
		ingestorProduct.setSensingStartTime(TEST_START_TIME_TEXT);
		ingestorProduct.setSensingStopTime(TEST_STOP_TIME_TEXT);
		File productFile = new File(TEST_PRODUCT_PATH_2);
		ingestorProduct.setMountPoint(TEST_STORAGE_SYSTEM);
		ingestorProduct.setFilePath(productFile.getParent());
		ingestorProduct.setProductFileName(productFile.getName());
		ingestorProduct.getParameters().add(new de.dlr.proseo.ingestor.rest.model.Parameter(
				"copernicusCollection", "STRING", "01"));
		ingestorProduct.getParameters().add(new de.dlr.proseo.ingestor.rest.model.Parameter(
				"revision", "STRING", "99"));
		List<IngestorProduct> ingestorProducts = new ArrayList<>();
		ingestorProducts.add(ingestorProduct);
		
		// Check mock storage manager is up (logging calls) (using Castlemock: https://hub.docker.com/r/castlemock/castlemock/)
		String testUrl = config.getStorageManagerUrl() + "/store?productId=4711";
		Map<String, String> mockRequest = new HashMap<>();
		mockRequest.put("productId", "4711");
		
		logger.info("Testing availability of mock storage manager at {}", testUrl);
		
		ResponseEntity<Object> mockEntity = new TestRestTemplate().postForEntity(testUrl, mockRequest, Object.class);
		assertEquals("Mock storage manager not available:", HttpStatus.CREATED, mockEntity.getStatusCode());
		
		// Check mock production planner is up (logging calls)
		testUrl = ingestorConfig.getProductionPlannerUrl() + "/product/4711";
		logger.info("Testing availability of mock production planner at {}", testUrl);
		
		Object mockObject = new TestRestTemplate().patchForObject(testUrl, mockRequest, Object.class);
		assertNotNull("Mock production planner not available", mockObject);
		logger.info("Got result object " + mockObject);
		if (mockObject instanceof ResponseEntity) {
			@SuppressWarnings("unchecked")
			Map<String, String> mockResult = ((ResponseEntity<Map<String, String>>) mockObject).getBody();
			assertEquals("Mock production planner returns unexpected status:", "OK", mockResult.get("status"));
		}
		
		// Perform REST API call
		try {
			testUrl = "http://localhost:" + port + INGESTOR_BASE_URI + "/ingest/" + URLEncoder.encode(TEST_NAME, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		logger.info("Testing URL {} / POST", testUrl);

		@SuppressWarnings("rawtypes")
		ResponseEntity<List> postEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.postForEntity(testUrl, ingestorProducts, List.class);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, postEntity.getStatusCode());
		
		@SuppressWarnings("unchecked")
		List<RestProduct> responseProducts = postEntity.getBody();
		assertEquals("Unexpected number of response products: ", 1, responseProducts.size());
		
		// Check result attributes
		RestProduct responseProduct = responseProducts.get(0);
		assertNotEquals("Unexpected database ID: ", 0L, responseProduct.getId().longValue());
		assertEquals("Unexpected product class: ", ingestorProduct.getProductClass(), responseProduct.getProductClass());
		assertEquals("Unexpected processing mode: ", ingestorProduct.getMode(), responseProduct.getMode());
		assertEquals("Unexpected sensing start time: ", ingestorProduct.getSensingStartTime(), responseProduct.getSensingStartTime());
		assertEquals("Unexpected sensing stop time: ", ingestorProduct.getSensingStopTime(), responseProduct.getSensingStopTime());
		assertEquals("Unexpected orbit number: ", ingestorProduct.getOrbit().getOrbitNumber(), responseProduct.getOrbit().getOrbitNumber());
		assertEquals("Unexpected number of product files: ", 1, responseProduct.getProductFile().size());
		de.dlr.proseo.ingestor.rest.model.ProductFile restProductFile = responseProduct.getProductFile().get(0);
		assertEquals("Unexpected product file name: ", ingestorProduct.getProductFileName(), restProductFile.getProductFileName());
		assertEquals("Unexpected number of aux files: ", 0, restProductFile.getAuxFileNames().size());
		assertTrue("Unexpected file path", restProductFile.getFilePath().matches(File.separator + TEST_CODE + File.separator + responseProduct.getId()));
		
		// Check triggering of production planner
		// TODO

		logger.info("Test OK: Insert a list of products");
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

		logger.info("Test OK: Insert files for an existing product");
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
