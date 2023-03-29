/**
 * ProductArchiveRepositoryTest.java
 * 
 * (c) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.enums.ArchiveType;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for ProductArchiveRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ProductArchiveRepositoryTest {

	private static final String TEST_HOSTNAME = "localhost";
	private static final String TEST_CODE = "TEST_CODE";
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductArchiveRepositoryTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test the additional repository methods
	 */
	@Test
	public final void test() {
		ProductArchive archive = new ProductArchive();
		archive.setCode(TEST_CODE);
		archive.setName(TEST_CODE);
		archive.setArchiveType(ArchiveType.PRIP);
		archive.setBaseUri("https://" + TEST_HOSTNAME);
		RepositoryService.getProductArchiveRepository().save(archive);
		
		// Test findByName
		archive = RepositoryService.getProductArchiveRepository().findByCode(TEST_CODE);
		assertNotNull("Find by code failed for ProductArchive", archive);
		
		logger.info("OK: Test for findByCode completed");
		
	}

}
