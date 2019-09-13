/**
 * ProductClassControllerTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.dlr.proseo.prodclmgr.ProductClassSecurityConfig;
import de.dlr.proseo.prodclmgr.ProductClassTestConfiguration;

/**
 * Test class for the REST API of ProductClassControllerImpl
 * 
 * @author Dr. Thomas Bassler
 */
public class ProductClassControllerTest {
	
	/* The base URI of the Ingestor */
	private static String PRODUCT_CLASS_BASE_URI = "/proseo/productclass-mgr/v0.1";
	
	/** Test configuration */
	@Autowired
	ProductClassTestConfiguration config;
	
	/** The security environment for this test */
	@Autowired
	ProductClassSecurityConfig ingestorSecurityConfig;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassControllerTest.class);

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

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#createProductClass(de.dlr.proseo.prodclmgr.rest.model.ProductClass)}.
	 */
	@Test
	public final void testCreateProductClass() {
		// TODO
		logger.warn("Test not implemented for createProductClass");

		logger.info("Test OK: Insert a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getProductClass(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetProductClass() {
		// TODO
		logger.warn("Test not implemented for getProductClass");

		logger.info("Test OK: Read all product classes");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getProductClassById(java.lang.Long)}.
	 */
	@Test
	public final void testGetProductClassById() {
		// TODO
		logger.warn("Test not implemented for getProductClassById");

		logger.info("Test OK: Read a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#updateProductClass(java.lang.Long, de.dlr.proseo.prodclmgr.rest.model.ProductClass)}.
	 */
	@Test
	public final void testUpdateProductClass() {
		// TODO
		logger.warn("Test not implemented for updateProductClass");

		logger.info("Test OK: Update a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#deleteProductclassById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteProductclassById() {
		// TODO
		logger.warn("Test not implemented for deleteProductclassById");

		logger.info("Test OK: Delete a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getObjects(java.lang.String, java.lang.Long)}.
	 */
	@Test
	public final void testGetObjects() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#updateObject(java.lang.String, java.lang.Long, java.util.List)}.
	 */
	@Test
	public final void testUpdateObject() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#deleteTargetclass(java.lang.String, java.lang.String, java.lang.Long)}.
	 */
	@Test
	public final void testDeleteTargetclass() {
		fail("Not yet implemented"); // TODO
	}

}
