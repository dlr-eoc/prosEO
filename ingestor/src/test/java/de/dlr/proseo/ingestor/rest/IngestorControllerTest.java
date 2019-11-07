/**
 * IngestorControllerTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.ingestor.IngestorApplication;
import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.IngestorSecurityConfig;
import de.dlr.proseo.ingestor.IngestorTestConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.RestParameter;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Test class for the REST API of IngestorControllerImpl
 * 
 * This class uses programmatic transaction management
 * 
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IngestorApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class IngestorControllerTest {

	/* The base URI of the Ingestor */
	private static String INGESTOR_BASE_URI = "/proseo/ingestor/v0.1";
	
	/* Various static test data */
	private static final String TEST_CODE = "ABC";
	private static final String TEST_PRODUCT_TYPE = "FRESCO";
	private static final String TEST_MISSION_TYPE = "L2__FRESCO_";
	private static final String TEST_NAME = "Test Facility";
	private static final String TEST_STORAGE_SYSTEM = "src/test/resources/IDA_test";
//	private static final String TEST_PRODUCT_PATH_1 = "L2/2018/07/21/03982/OFFL/S5P_OFFL_L2__CLOUD__20180721T000328_20180721T000828_03982_01_010100_20180721T010233.nc";
	private static final String TEST_PRODUCT_PATH_2 = "L2/2018/07/21/03982/OFFL/S5P_OFFL_L2__FRESCO_20180721T000328_20180721T000828_03982_01_010100_20180721T010233.nc";
	private static final String TEST_SC_CODE = "XYZ";
	private static final int TEST_ORBIT_NUMBER = 4712;
	private static final Instant TEST_START_TIME = Instant.from(Orbit.orbitTimeFormatter.parse("2018-06-13T09:23:45.396521"));
	private static final String TEST_STOP_TIME_TEXT = "2018-07-21T00:08:28.000123";
	private static final String TEST_START_TIME_TEXT = "2018-07-21T00:03:28.000456";
	private static final String TEST_GEN_TIME_TEXT = "2018-08-15T10:12:39.000789";
	private static final String TEST_MODE_OFFL = "OFFL";
	private static final String TEST_FILE_CLASS = "OPER";
	
	
	/* Test products */
	private static String[][] testProductData = {
		// id, version, mission code, product class, file class, mode, sensing start, sensing stop, generation, revision (parameter)
		{ "0", "1", "S5P", "L1B", TEST_FILE_CLASS, "NRTI", "2019-08-29T22:49:21.074395", "2019-08-30T00:19:33.946628", "2019-10-05T10:12:39.000000", "01" },
		{ "0", "1", "S5P", "L1B", TEST_FILE_CLASS, "NRTI", "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "2019-10-05T10:13:22.000000", "01" },
		{ "0", "1", "TDM", "DEM", TEST_FILE_CLASS, null, "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "2019-10-05T10:13:22.000000", "02" }
	};

	/** Ingestor configuration */
	@Autowired
	private IngestorConfiguration ingestorConfig;
	
	/** Test configuration */
	@Autowired
	private IngestorTestConfiguration config;
	
	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;
	
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
		testProduct.setFileClass(testData[4]);
		testProduct.setMode(testData[5]);
		testProduct.setSensingStartTime(Instant.from(Orbit.orbitTimeFormatter.parse(testData[6])));
		testProduct.setSensingStopTime(Instant.from(Orbit.orbitTimeFormatter.parse(testData[7])));
		testProduct.setGenerationTime(Instant.from(Orbit.orbitTimeFormatter.parse(testData[8])));
		testProduct.getParameters().put(
				"revision", new Parameter().init(ParameterType.INTEGER, Integer.parseInt(testData[9])));
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
	@SuppressWarnings("unchecked")
	@Test
	public final void testIngestProducts() {
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		transactionTemplate.execute(new TransactionCallback<>() {

			@Override
			public Object doInTransaction(TransactionStatus txStatus) {
				// Make sure processing facility and product class exist
				Mission mission = RepositoryService.getMissionRepository().findByCode(TEST_CODE);
				if (null == mission) {
					mission = new Mission();
					mission.setCode(TEST_CODE);
					mission.getFileClasses().add(TEST_FILE_CLASS);
					mission.getProcessingModes().add(TEST_MODE_OFFL);
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
					//mission.getProductClasses().add(prodClass);
					//mission = RepositoryService.getMissionRepository().save(mission);
				}
				logger.info("Using product class " + prodClass.getProductType() + " with id " + prodClass.getId());
				
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(TEST_SC_CODE);
				if (null == spacecraft) {
					spacecraft = new Spacecraft();
					spacecraft.setCode(TEST_SC_CODE);
					spacecraft.setMission(mission);
					spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
					logger.info("Spacecraft " + spacecraft.getCode() + " created with id " + spacecraft.getId());
					//mission.getSpacecrafts().add(spacecraft);
					//mission = RepositoryService.getMissionRepository().save(mission);
				}
				logger.info("Using spacecraft " + spacecraft.getCode() + " with id " + spacecraft.getId());
				
				Orbit orbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(TEST_SC_CODE, TEST_ORBIT_NUMBER);
				if (null == orbit) {
					orbit = new Orbit();
					orbit.setSpacecraft(spacecraft);
					orbit.setOrbitNumber(TEST_ORBIT_NUMBER);
					orbit.setStartTime(TEST_START_TIME);
					orbit = RepositoryService.getOrbitRepository().save(orbit);
					//spacecraft.getOrbits().add(orbit);
					//spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
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

				return null;
			}
		});
		
		// Create a directory with product data files
		// Static: src/test/resources/IDA_test
		
		// Create an IngestorProduct describing the product directory
		IngestorProduct ingestorProduct = new IngestorProduct();
		ingestorProduct.setId(0L);
		ingestorProduct.setVersion(1L);
		ingestorProduct.setMissionCode(TEST_CODE);
		ingestorProduct.setFileClass(TEST_FILE_CLASS);
		ingestorProduct.setMode(TEST_MODE_OFFL);
		de.dlr.proseo.ingestor.rest.model.Orbit restOrbit = new de.dlr.proseo.ingestor.rest.model.Orbit();
		restOrbit.setSpacecraftCode(TEST_SC_CODE);
		restOrbit.setOrbitNumber(Long.valueOf(TEST_ORBIT_NUMBER));
		ingestorProduct.setOrbit(restOrbit);
		ingestorProduct.setProductClass(TEST_PRODUCT_TYPE);
		ingestorProduct.setSensingStartTime(TEST_START_TIME_TEXT);
		ingestorProduct.setSensingStopTime(TEST_STOP_TIME_TEXT);
		ingestorProduct.setGenerationTime(TEST_GEN_TIME_TEXT);
		File productFile = new File(TEST_PRODUCT_PATH_2);
		ingestorProduct.setMountPoint(TEST_STORAGE_SYSTEM);
		ingestorProduct.setFilePath(productFile.getParent());
		ingestorProduct.setProductFileName(productFile.getName());
		ingestorProduct.getParameters().add(new RestParameter(
				"copernicusCollection", "STRING", "01"));
		ingestorProduct.getParameters().add(new RestParameter(
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
		
		Object mockObject = new TestRestTemplate().getForObject(testUrl, Object.class);
		assertNotNull("Mock production planner not available", mockObject);
		logger.info("Got result object " + mockObject);
		if (mockObject instanceof ResponseEntity) {
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
		assertEquals("Unexpected number of response products: ", 1, postEntity.getBody().size());
		
		// Check result attributes
		Map<String, Object> responseProduct = (Map<String, Object>) postEntity.getBody().get(0);
		assertNotEquals("Unexpected database ID: ", 0L, responseProduct.get("id"));
		assertEquals("Unexpected product class: ", ingestorProduct.getProductClass(), responseProduct.get("productClass"));
		assertEquals("Unexpected processing mode: ", ingestorProduct.getMode(), responseProduct.get("mode"));
		assertEquals("Unexpected sensing start time: ", ingestorProduct.getSensingStartTime(), responseProduct.get("sensingStartTime"));
		assertEquals("Unexpected sensing stop time: ", ingestorProduct.getSensingStopTime(), responseProduct.get("sensingStopTime"));
		assertEquals("Unexpected generation time: ", ingestorProduct.getGenerationTime(), responseProduct.get("generationTime"));
		Map<String, Object> responseOrbit = (Map<String, Object>) responseProduct.get("orbit");
		assertNotNull("Orbit missing", responseOrbit);
		assertEquals("Unexpected orbit number: ", ingestorProduct.getOrbit().getOrbitNumber().intValue(), responseOrbit.get("orbitNumber"));
		List<Map<String, Object>> responseProductFiles = (List<Map<String, Object>>) responseProduct.get("productFile");
		assertNotNull("Product files missing", responseProductFiles);
		assertEquals("Unexpected number of product files: ", 1, responseProductFiles.size());
		Map<String, Object> responseProductFile = responseProductFiles.get(0);
		assertEquals("Unexpected product file name: ", ingestorProduct.getProductFileName(), responseProductFile.get("productFileName"));
		assertEquals("Unexpected number of aux files: ", 0, ((List<String>) responseProductFile.get("auxFileNames")).size());
		
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
		
		List<Product> testProducts = new ArrayList<>();
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		transactionTemplate.execute(new TransactionCallback<>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				// Make sure processing facility and product class exist
				
				// Make sure test products with product files exist
				testProducts.addAll(createTestProducts());

				return null;
			}
		
		});
		
		// Perform REST API call and check retrieved product file
				
		// TODO
		logger.warn("Test not implemented for getProductFile");
		
		// Clean up database
		deleteTestProducts(testProducts);

		logger.info("Test OK: Get Product File");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#ingestProductFile(java.lang.Long, ProcessingFacility, de.dlr.proseo.ingestor.rest.model.ProductFile)}.
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
		
		List<Product> testProducts = new ArrayList<>();
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		transactionTemplate.execute(new TransactionCallback<>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				// Make sure processing facility and product class exist
				
				// Make sure test products with product files exist
				testProducts.addAll(createTestProducts());

				return null;
			}
		
		});
		
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
		
		List<Product> testProducts = new ArrayList<>();
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		transactionTemplate.execute(new TransactionCallback<>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				// Make sure processing facility and product class exist
				
				// Make sure test products with product files exist
				testProducts.addAll(createTestProducts());

				return null;
			}
		
		});
		
		// Create mock storage manager (logging calls)
		
		// Perform REST API call
		
		// Check logged calls for storage manager
		
		// TODO
		logger.warn("Test not implemented for modifyProductFile");

		logger.info("Test OK: Modify Product File");
	}

}
