/**
 * ProductArchiveRepositoryTest.java
 * 
 * (c) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.enums.ArchiveType;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for ProductArchiveRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class ProductArchiveRepositoryTest {

	private static final String TEST_HOSTNAME = "localhost";
	private static final String TEST_CODE = "TEST_CODE";
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductArchiveRepositoryTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeEach
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterEach
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
		assertNotNull(archive, "Find by code failed for ProductArchive");
		
		logger.info("OK: Test for findByCode completed");
		
	}

}
