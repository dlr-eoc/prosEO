/**
 * GeneratorControllerImplTest.java
 * 
 * (c) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.rest;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.DataDrivenOrderTrigger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordergen.OrderGenerator;
import de.dlr.proseo.model.rest.model.RestOrder;

/**
 * Unit test for GeneratorControllerImpl (automatic generation of processing orders)
 * 
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderGenerator.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@WithMockUser(username = "UTM-testuser", password = "password")
@Transactional
public class GeneratorControllerImplTest {

	private static final String TEST_CODE_1 = "UTM";
	private static final String TEST_PRODUCT_CLASS_1 = "$PC1$";
	private static final String TEST_WORKFLOW_1 = "$WF1$";
	private static final String TEST_TRIGGER_1 = "$TR1$";

	private static final String HEADER_AUTH_BASIC = "Basic VVRNLXRlc3R1c2VyOnBhc3N3b3Jk";
	
	@Autowired
	private GeneratorControllerImpl generatorController;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GeneratorControllerImplTest.class);
	
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
		Mission mission1 = new Mission();
		mission1.setCode(TEST_CODE_1);
		mission1 = RepositoryService.getMissionRepository().save(mission1);
		
		ProductClass productClass1 = new ProductClass();
		productClass1.setMission(mission1);
		productClass1.setProductType(TEST_PRODUCT_CLASS_1);
		productClass1 = RepositoryService.getProductClassRepository().save(productClass1);
		
		Workflow workflow1 = new Workflow();
		workflow1.setMission(mission1);
		workflow1.setName(TEST_WORKFLOW_1);
		workflow1.setUuid(UUID.randomUUID());
		workflow1.setWorkflowVersion("1");
		workflow1.setInputProductClass(productClass1);
		workflow1.setEnabled(true);
		workflow1 = RepositoryService.getWorkflowRepository().save(workflow1);
		
		DataDrivenOrderTrigger trigger1 = new DataDrivenOrderTrigger();
		trigger1.setMission(mission1);
		trigger1.setName(TEST_TRIGGER_1);
		trigger1.setWorkflow(workflow1);
		trigger1 = RepositoryService.getDataDrivenOrderTriggerRepository().save(trigger1);

		Product product1 = new Product();
		product1.setProductClass(productClass1);
		product1.setUuid(UUID.randomUUID());
		product1.setSensingStartTime(Instant.parse("2025-12-31T00:00:00.00Z"));
		product1.setSensingStopTime(Instant.parse("2025-12-31T01:00:00.00Z"));
		product1.setGenerationTime(Instant.parse("2025-12-31T02:00:00.00Z"));
		product1 = RepositoryService.getProductRepository().save(product1);

		HttpHeaders testHeader = new HttpHeaders();
		testHeader.add(HttpHeaders.AUTHORIZATION, HEADER_AUTH_BASIC);

		ResponseEntity<List<RestOrder>> responseEntity = generatorController.generateForProduct(product1.getId(), testHeader);
		
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, responseEntity.getStatusCode());
		assertEquals("Unexpected number of response products: ", 1, responseEntity.getBody().size());
	}	
}
