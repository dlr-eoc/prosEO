/**
 * ProductManagerTest.java
 * 
 * (c) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.dlr.proseo.ingestor.IngestorApplication;
import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Test class for the methods of ProductManager
 * 
 * This class uses programmatic transaction management
 * 
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IngestorApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class ProductManagerTest {

	/* Test products */
	private static final String TEST_CODE = "S5P";
	private static final String TEST_PRODUCT_TYPE = "L1B_______";
	private static final String TEST_FILE_CLASS = "OPER";
	private static final String TEST_MODE = "NRTI";
//	private static final String TEST_MODE_2 = "OFFL";
	private static final String TEST_ALT_CODE = "TDM";
	private static final String TEST_ALT_PRODUCT_TYPE = "TDM.DEM.DEM";
	private static String[][] testProductData = {
		// id, version, mission code, product class, file class, mode, sensing start, sensing stop, generation, revision (parameter)
		{ "0", "1", TEST_CODE, TEST_PRODUCT_TYPE, TEST_FILE_CLASS, TEST_MODE, "2019-08-29T22:49:21.074395", "2019-08-30T00:19:33.946628", "2019-10-05T10:12:39.000000", "01" },
		{ "0", "1", TEST_CODE, TEST_PRODUCT_TYPE, TEST_FILE_CLASS, TEST_MODE, "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "2019-10-05T10:13:22.000000", "01" },
		{ "0", "1", TEST_ALT_CODE, TEST_ALT_PRODUCT_TYPE, TEST_FILE_CLASS, null, "2019-08-30T00:19:33.946628", "2019-08-30T01:49:46.482753", "2019-10-05T10:13:22.000000", "02" }
	};
	private static String[][] testProductFileData = {
		// Product file name, ZIP file name, AUX file name
		{ "product_file_name.nc", "zip_file_name.zip", "aux_file_name.txt" }
	};
	private static final String PROCESSING_FACILITY_NAME = "testfacility";

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
	/** The ProductManager class under test */
	@Autowired
	private ProductManager productManager;
	 
//	/** Test Entity Manager */
//	@Autowired
//	private EntityManager testEm;
	
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
		
		testProduct.setProductClass(
				RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(testData[2], testData[3]));

		logger.info("... creating product with product type {}", (null == testProduct.getProductClass() ? null : testProduct.getProductClass().getProductType()));
		testProduct.setUuid(UUID.randomUUID());
		testProduct.setFileClass(testData[4]);
		testProduct.setMode(testData[5]);
		testProduct.setSensingStartTime(Instant.from(OrbitTimeFormatter.parse(testData[6])));
		testProduct.setSensingStopTime(Instant.from(OrbitTimeFormatter.parse(testData[7])));
		testProduct.setGenerationTime(Instant.from(OrbitTimeFormatter.parse(testData[8])));
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
	private List<Product> createTestProducts(ProcessingFacility testFacility) {
		logger.info("Creating test products");
		List<Product> testProducts = new ArrayList<>();
		for (int i = 0; i < testProductData.length; ++i) {
			Product testProduct = createProduct(testProductData[i]);
			testProducts.add(testProduct);
			if (i < testProductFileData.length) {
				ProductFile testProductFile = new ProductFile();
				testProductFile.setProcessingFacility(testFacility);
				testProductFile.setProductFileName(testProductFileData[i][0]);
				testProductFile.setZipFileName(testProductFileData[i][1]);
				testProductFile.getAuxFileNames().add(testProductFileData[i][2]);
				testProductFile = RepositoryService.getProductFileRepository().save(testProductFile);
				testProduct.getProductFile().add(testProductFile);
			}
		}
		return testProducts;
	}
	
	/**
	 * Generates a test processing facility
	 * 
	 * @return the test processing facility
	 */
	private ProcessingFacility createTestFacility() {
		logger.info("Creating test facility");
		
		ProcessingFacility testFacility = new ProcessingFacility();
		testFacility.setName(PROCESSING_FACILITY_NAME);
		testFacility = RepositoryService.getFacilityRepository().save(testFacility);
		
		return testFacility;
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

//	/**
//	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#deleteProductById(java.lang.Long)}.
//	 */
//	@Test
//	public final void testDeleteProductById() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#getProducts(java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String[])}.
//	 */
//	@Test
//	public final void testGetProducts() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#countProducts(java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.lang.Long)}.
//	 */
//	@Test
//	public final void testCountProducts() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#createProduct(de.dlr.proseo.ingestor.rest.model.RestProduct)}.
//	 */
//	@Test
//	public final void testCreateProduct() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#getProductById(java.lang.Long)}.
//	 */
//	@Test
//	public final void testGetProductById() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#modifyProduct(java.lang.Long, de.dlr.proseo.ingestor.rest.model.RestProduct)}.
//	 */
//	@Test
//	public final void testModifyProduct() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#getProductByUuid(java.lang.String)}.
//	 */
//	@Test
//	public final void testGetProductByUuid() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#downloadProductById(java.lang.Long, java.lang.Long, java.lang.Long)}.
//	 */
//	@Test
//	public final void testDownloadProductById() {
//		fail("Not yet implemented"); // TODO
//	}

	/**
	 * Test method for {@link de.dlr.proseo.ingestor.rest.ProductManager#getDownloadTokenById(java.lang.Long, java.lang.String)}.
	 */
	@Test
	public final void testGetDownloadTokenById() {
		logger.info("Starting test for getDownloadTokenById");
		
		ProcessingFacility testFacility = createTestFacility();
		List<Product> testProducts = createTestProducts(testFacility);
		Product testProduct = testProducts.get(0);
		
		// Basic case: ID only
		String token = productManager.getDownloadTokenById(testProduct.getId(), null);
		
		SignedJWT signedJWT = null;
		try {
			signedJWT = SignedJWT.parse(token);
		} catch (ParseException e) {
			fail("Token not parseable");
		}
		
		JWSVerifier verifier = null;
		try {
			verifier = new MACVerifier(ingestorConfig.getStorageManagerSecret());
		} catch (JOSEException e) {
			fail("Secret length is shorter than the minimum 256-bit requirement");
		}

		try {
			assertTrue(signedJWT.verify(verifier));
		} catch (IllegalStateException e) {
			fail("The JWS object is not in a signed or verified state, actual state: " + signedJWT.getState());
		} catch (JOSEException e) {
			fail("The JWS object couldn't be verified");
		}
		
		// Retrieve / verify the JWT claims according to the app requirements
		JWTClaimsSet claimsSet = null;
		try {
			claimsSet = signedJWT.getJWTClaimsSet();
		} catch (ParseException e) {
			fail("The payload of the JWT doesn't represent a valid JSON object and a JWT claims set");
		}

		assertEquals(testProduct.getProductFile().iterator().next().getProductFileName(), claimsSet.getSubject());
		assertTrue(new Date().before(claimsSet.getExpirationTime()));
		
		// TODO More tests with varying parameters
	}

}
