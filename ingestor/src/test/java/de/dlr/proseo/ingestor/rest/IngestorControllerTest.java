/**
 * IngestorControllerTest.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import de.dlr.proseo.ingestor.IngestorApplication;
import de.dlr.proseo.ingestor.IngestorTestConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.ProductFileUtil;
import de.dlr.proseo.ingestor.rest.model.RestParameter;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.ingestor.rest.model.RestProductFile;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Test class for the REST API of IngestorControllerImpl
 *
 * @author Dr. Thomas Bassler
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IngestorApplication.class)
@WithMockUser(username = "UTM-testuser", password = "password")
@Transactional
public class IngestorControllerTest {

	/* Various static test data */
	private static final String TEST_CODE = "UTM";
	private static final String TEST_PRODUCT_TYPE = "L2__FRESCO_";
	private static final String TEST_NAME = "Test Facility";
	private static final String TEST_STORAGE_SYSTEM = "src/test/resources/IDA_test";
	private static final String TEST_PRODUCT_PATH_1 = "L2/2018/07/21/03982/OFFL/S5P_OFFL_L2__CLOUD__20180721T000328_20180721T000828_03982_01_010100_20180721T010233.nc";
	private static final String TEST_PRODUCT_PATH_2 = "L2/2018/07/21/03982/OFFL/S5P_OFFL_L2__FRESCO_20180721T000328_20180721T000828_03982_01_010100_20180721T010233.nc";
	private static final String TEST_SC_CODE = "XYZ";
	private static final int TEST_ORBIT_NUMBER = 4712;
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.396521"));
	private static final String TEST_STOP_TIME_TEXT = "2018-07-21T00:08:28.000123";
	private static final String TEST_START_TIME_TEXT = "2018-07-21T00:03:28.000456";
	private static final String TEST_GEN_TIME_TEXT = "2018-08-15T10:12:39.000789";
	private static final String TEST_MODE_OFFL = "OFFL";
	private static final String TEST_FILE_CLASS = "OPER";
	private static final long TEST_FILE_SIZE = 7654321L;
	private static final String TEST_FILE_NAME = "S5P_OPER_L0________20190509T212509_20190509T214507_08138_01.ZIP";
	private static final String TEST_CHECKSUM = "fb720888a0d9bae6b16c1f9607c4de27";
	private static final String STORAGE_MGR_RESPONSE = "{" + "    \"productId\": \"newProdId001XYZ\","
			+ "    \"sourceStorageType\": \"POSIX\"," + "    \"sourceFilePaths\": [" + "        \"src/\","
			+ "        \"target/\"" + "    ]," + "    \"targetStorageId\": \"proseo-data-001\","
			+ "    \"targetStorageType\": \"S3\","
			+ "    \"registeredFilePath\": \"s3://proseo-data-001/newProdId001XYZ/1573057763/\","
			+ "    \"registered\": true," + "    \"registeredFilesCount\": 108,"
			+ "    \"registeredFilesList\": [        "
			+ "			\"src/test/resources/IDA_test/L2/2018/07/21/03982/OFFL/S5P_OFFL_L2__FRESCO_20180721T000328_20180721T000828_03982_01_010100_20180721T010233.nc\""
			+ "]," + "    \"deleted\": false" + "}";

	/* Test products */
	private static String[][] testProductData = {
			// id, version, mission code, product class, file class, mode, sensing start,
			// sensing stop, generation, revision (parameter)
			{ "0", "1", TEST_CODE, "L1B", TEST_FILE_CLASS, "NRTI", "2019-08-29T22:49:21.074395",
					"2019-08-30T00:19:33.946628", "2019-10-05T10:12:39.000000", "01" },
			{ "0", "1", TEST_CODE, "L1B", TEST_FILE_CLASS, "NRTI", "2019-08-30T00:19:33.946628",
					"2019-08-30T01:49:46.482753", "2019-10-05T10:13:22.000000", "01" },
			{ "0", "1", TEST_CODE, "DEM", TEST_FILE_CLASS, null, "2019-08-30T00:19:33.946628",
					"2019-08-30T01:49:46.482753", "2019-10-05T10:13:22.000000", "02" } };

	/** The ingestor controller under test */
	@Autowired
	private IngestControllerImpl ici;

	/** Test configuration */
	@Autowired
	IngestorTestConfiguration config;

	/** Mocking the storage manager and planner */
	private static int WIREMOCK_PORT = 8080;
	@ClassRule
	public static WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);
	//private static WireMockServer wireMockServer;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(IngestorControllerTest.class);
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		wireMockRule.start();

		wireMockRule.stubFor(WireMock.post(WireMock.urlEqualTo("/storage-mgr/products"))
				.willReturn(WireMock.aResponse().withStatus(201).withHeader("Content-Type", "application/json")
						.withBody(STORAGE_MGR_RESPONSE)));

		wireMockRule.stubFor(WireMock.delete(WireMock.urlEqualTo(
				"/storage-mgr/products?pathInfo=L2/2018/07/21/03982/OFFL/S5P_OFFL_L2__FRESCO_20180721T000328_20180721T000828_03982_01_010100_20180721T010233.nc"
						+ "/S5P_OPER_L0________20190509T212509_20190509T214507_08138_01.ZIP"))
				.willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
						.withBody("{\"foo\":\"bar\"}")));

//		wireMockServer.getStubMappings().forEach(s -> logger.trace("Stub mapping: " + s));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// Make sure processing facility and product class exist
		Mission mission = new Mission();
		mission.setCode(TEST_CODE);
		mission.getFileClasses().add(TEST_FILE_CLASS);
		mission.getProcessingModes().add(TEST_MODE_OFFL);
		mission = RepositoryService.getMissionRepository().save(mission);
		logger.trace("Using mission " + mission.getCode() + " with id " + mission.getId());

		ProductClass prodClass = new ProductClass();
		prodClass.setMission(mission);
		prodClass.setProductType(TEST_PRODUCT_TYPE);
		prodClass = RepositoryService.getProductClassRepository().save(prodClass);
		mission.getProductClasses().add(prodClass);
		mission = RepositoryService.getMissionRepository().save(mission);
		logger.trace("Using product class " + prodClass.getProductType() + " with id " + prodClass.getId());

		Spacecraft spacecraft = new Spacecraft();
		spacecraft.setCode(TEST_SC_CODE);
		spacecraft.setMission(mission);
		spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
		logger.trace("Spacecraft " + spacecraft.getCode() + " created with id " + spacecraft.getId());
		mission.getSpacecrafts().add(spacecraft);
		mission = RepositoryService.getMissionRepository().save(mission);
		logger.trace("Using spacecraft " + spacecraft.getCode() + " with id " + spacecraft.getId());

		Orbit orbit = new Orbit();
		orbit.setSpacecraft(spacecraft);
		orbit.setOrbitNumber(TEST_ORBIT_NUMBER);
		orbit.setStartTime(TEST_START_TIME);
		orbit = RepositoryService.getOrbitRepository().save(orbit);
		spacecraft.getOrbits().add(orbit);
		spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);

		ProcessingFacility facility = new ProcessingFacility();
		facility.setName(TEST_NAME);
		facility.setProcessingEngineUrl("not used");
		facility.setStorageManagerUrl(config.getStorageManagerUrl());
		facility.setStorageManagerUser("testuser");
		facility.setStorageManagerPassword("testpwd");
		facility.setDefaultStorageType(StorageType.POSIX);
		facility = RepositoryService.getFacilityRepository().save(facility);

		Product testProduct;
		logger.trace("... creating test products in the database");
		for (String[] element : testProductData) {
			testProduct = new Product();

			testProduct.setProductClass(prodClass);
			testProduct.setUuid(UUID.randomUUID());
			testProduct.setFileClass(element[4]);
			testProduct.setMode(element[5]);
			testProduct.setSensingStartTime(Instant.from(OrbitTimeFormatter.parse(element[6])));
			testProduct.setSensingStopTime(Instant.from(OrbitTimeFormatter.parse(element[7])));
			testProduct.setGenerationTime(Instant.from(OrbitTimeFormatter.parse(element[8])));
			testProduct.getParameters().put("revision",
					new Parameter().init(ParameterType.INTEGER, Integer.parseInt(element[9])));

			element[0] = RepositoryService.getProductRepository().save(testProduct).getId() + "";
		}

		testProduct = RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0])).get();

		ProductFile productFile = new ProductFile();
		productFile.setProduct(testProduct);
		productFile.setProductFileName(TEST_FILE_NAME);
		productFile.setFilePath(TEST_PRODUCT_PATH_2);
		productFile.setProcessingFacility(facility);
		productFile.setStorageType(StorageType.POSIX);
		productFile.setFileSize(TEST_FILE_SIZE);
		productFile.setChecksum(TEST_CHECKSUM);
		productFile.setChecksumTime(Instant.now());
		productFile = RepositoryService.getProductFileRepository().save(productFile);

		testProduct.getProductFile().add(productFile);
		RepositoryService.getProductRepository().save(testProduct);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		RepositoryService.getProductFileRepository().deleteAll();
		RepositoryService.getProductRepository().deleteAll();
		RepositoryService.getProductClassRepository().deleteAll();
		RepositoryService.getOrbitRepository().deleteAll();
		RepositoryService.getSpacecraftRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		wireMockRule.stop();
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#ingestProducts(java.lang.String, java.util.List)}.
	 *
	 * Test: Ingest a single product
	 *
	 * Precondition: Processing facility exists, product class exists, mock storage
	 * manager exists, mock production planner exists
	 */
	@Test
	public final void testIngestProducts() {
		logger.trace(">>> testIngestProducts()");

//		Create a directory with product data files
//		Static: src/test/resources/IDA_test

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
		ingestorProduct.setSourceStorageType(StorageType.S3.toString());
		ingestorProduct.setMountPoint(TEST_STORAGE_SYSTEM);
		ingestorProduct.setFilePath(productFile.getParent());
		ingestorProduct.setProductFileName(productFile.getName());
		ingestorProduct.setFileSize(TEST_FILE_SIZE);
		ingestorProduct.setChecksum(TEST_CHECKSUM);
		ingestorProduct.setChecksumTime(TEST_GEN_TIME_TEXT);
		ingestorProduct.getParameters().add(new RestParameter("copernicusCollection", "STRING", "01"));
		ingestorProduct.getParameters().add(new RestParameter("revision", "STRING", "99"));
		List<IngestorProduct> ingestorProducts = new ArrayList<>();
		ingestorProducts.add(ingestorProduct);

		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<List<RestProduct>> postEntity = ici.ingestProducts(TEST_NAME, false, ingestorProducts,
				testHeader);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, postEntity.getStatusCode());
		assertEquals("Unexpected number of response products: ", 1, postEntity.getBody().size());

		// Check result attributes
		RestProduct responseProduct = postEntity.getBody().get(0);
		assertNotEquals("Unexpected database ID: ", 0L, responseProduct.getId().longValue());
		assertEquals("Unexpected product class: ", ingestorProduct.getProductClass(),
				responseProduct.getProductClass());
		assertEquals("Unexpected processing mode: ", ingestorProduct.getMode(), responseProduct.getMode());
		assertEquals("Unexpected sensing start time: ", ingestorProduct.getSensingStartTime(),
				responseProduct.getSensingStartTime());
		assertEquals("Unexpected sensing stop time: ", ingestorProduct.getSensingStopTime(),
				responseProduct.getSensingStopTime());
		assertEquals("Unexpected generation time: ", ingestorProduct.getGenerationTime(),
				responseProduct.getGenerationTime());

		de.dlr.proseo.ingestor.rest.model.Orbit responseOrbit = responseProduct.getOrbit();
		assertNotNull("Orbit missing", responseOrbit);
		assertEquals("Unexpected orbit number: ", ingestorProduct.getOrbit().getOrbitNumber().intValue(),
				responseOrbit.getOrbitNumber().intValue());

		@Valid
		List<RestProductFile> responseProductFiles = responseProduct.getProductFile();
		assertNotNull("Product files missing", responseProductFiles);
		assertEquals("Unexpected number of product files: ", 1, responseProductFiles.size());
		RestProductFile responseProductFile = responseProductFiles.get(0);
		assertEquals("Unexpected product file name: ", ingestorProduct.getProductFileName(),
				responseProductFile.getProductFileName());
		assertEquals("Unexpected number of aux files: ", 0, responseProductFile.getAuxFileNames().size());

		// Check triggering of production planner in log
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#getProductFile(java.lang.Long, java.lang.String)}.
	 *
	 * Test: Get the product file for a product at a given processing facility
	 *
	 * Precondition: Processing facility exists, product and product file exist
	 */
	@Test
	public final void testGetProductFile() {
		logger.trace(">>> testGetProductFile()");

		// Retrieve a test product from the database
		Product testProduct = RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0]))
				.get();

		// Create a test header for authentication
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<RestProductFile> response = ici.getProductFile(testProduct.getId(), TEST_NAME, testHeader);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, response.getStatusCode());
		assertEquals("Wrong product file: ", TEST_PRODUCT_PATH_2, response.getBody().getFilePath());

	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#ingestProductFile(java.lang.Long, ProcessingFacility, de.dlr.proseo.ingestor.rest.model.ProductFile)}.
	 *
	 * Test: Ingest a product file for an existing product
	 *
	 * Precondition: Processing facility exists, product exists, mock storage
	 * manager exists, mock production planner exists
	 */
	@Test
	public final void testIngestProductFile() {

		// Retrieve a test product and product file from the database
		Product testProduct = RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0]))
				.get();
		ProductFile testProductFile = RepositoryService.getProductFileRepository().findByProductId(testProduct.getId())
				.get(0);

		// Delete product file from the product and the database
		testProduct.getProductFile().clear();
		RepositoryService.getProductFileRepository().deleteAll();

		//
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<RestProductFile> response = ici.ingestProductFile(testProduct.getId(), TEST_NAME,
				ProductFileUtil.toRestProductFile(testProductFile), testHeader);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, response.getStatusCode());

		// Check logged calls for storage manager and production planner
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#deleteProductFile(java.lang.Long, java.lang.String)}.
	 *
	 * Test: Delete a product file from the metadata database and from the storage
	 * manager
	 *
	 * Precondition: Processing facility exists, product with a product file exists,
	 * mock storage manager exists
	 */
	@Test
	public final void testDeleteProductFile() {
		logger.trace(">>> testDeleteProductFile()");

		// Retrieve a test product and product file from the database
		Product testProduct = RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0]))
				.get();

		//
		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<?> response = ici.deleteProductFile(testProduct.getId(), TEST_NAME, true, testHeader);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.NO_CONTENT, response.getStatusCode());

		// Check logged calls for storage manager
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ingestor.rest.IngestControllerImpl#modifyProductFile(java.lang.Long, java.lang.String, de.dlr.proseo.ingestor.rest.model.ProductFile)}.
	 *
	 * Test: Update a product file at a given processing facility
	 *
	 * Precondition: At least one product with a known ID is in the database
	 */
	@Test
	public final void testModifyProductFile() {
		logger.trace(">>> testModifyProductFile()");

		// Retrieve a test product and product file from the database
		Product testProduct = RepositoryService.getProductRepository().findById(Long.valueOf(testProductData[0][0]))
				.get();
		RestProductFile testProductFile = ProductFileUtil.toRestProductFile(
				RepositoryService.getProductFileRepository().findByProductId(testProduct.getId()).get(0));
		testProductFile.setFilePath(TEST_PRODUCT_PATH_1);

		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk");

		ResponseEntity<RestProductFile> response = ici.modifyProductFile(testProduct.getId(), TEST_NAME,
				testProductFile, testHeader);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.OK, response.getStatusCode());

		// Check logged calls for storage manager
	}

}
