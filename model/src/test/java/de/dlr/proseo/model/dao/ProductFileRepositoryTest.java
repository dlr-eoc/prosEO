/**
 * ProductFileRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

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
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for FacilityRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ProductFileRepositoryTest {

	private static final String TEST_CODE = "$ABC$";
	private static final String TEST_PRODUCT_TYPE = "$L2__FRESCO_$";
	private static final String TEST_FACILITY_NAME = "$Proseo Facility 1$";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductFileRepositoryTest.class);
	
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

	@Test
	public final void test() {
		Mission mission = new Mission();
		mission.setCode(TEST_CODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		ProductClass prodClass = new ProductClass();
		prodClass.setMission(mission);
		prodClass.setProductType(TEST_PRODUCT_TYPE);
		prodClass = RepositoryService.getProductClassRepository().save(prodClass);
		
		mission.getProductClasses().add(prodClass);
		RepositoryService.getMissionRepository().save(mission);
		
		Product product = new Product();
		product.setProductClass(prodClass);
		product.setUuid(UUID.randomUUID());
		product = RepositoryService.getProductRepository().save(product);

		ProcessingFacility facility = new ProcessingFacility();
		facility.setName(TEST_FACILITY_NAME);
		facility = RepositoryService.getFacilityRepository().save(facility);
		
		ProductFile productFile = new ProductFile();
		productFile.setProduct(product);
		productFile.setProcessingFacility(facility);
		productFile = RepositoryService.getProductFileRepository().save(productFile);
		
		product.getProductFile().add(productFile);
		product = RepositoryService.getProductRepository().save(product);
		
		// Test findByProductId
		List<ProductFile> queryList = RepositoryService.getProductFileRepository().findByProductId(product.getId());
		assertFalse("Find by product id failed for ProductFile", queryList.isEmpty());
		
		logger.info("OK: Test for findByProductId completed");
		
	}

}
