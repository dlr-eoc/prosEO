package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.assertj.core.util.Sets;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.ProcessingOrder.OrderSlicingType;
import de.dlr.proseo.model.ProcessingOrder.OrderState;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.OrderManager;
import de.dlr.proseo.ordermgr.OrdermgrSecurityConfig;
import de.dlr.proseo.ordermgr.rest.model.OrderUtil;
import de.dlr.proseo.ordermgr.rest.model.RestOrder;
import net.bytebuddy.asm.Advice.This;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@AutoConfigureTestEntityManager
public class OrderControllerTest {
	/* The base URI of the Orders */
	private static String ORDER_BASE_URI = "/proseo/order-mgr/v0.1";

	@LocalServerPort
	private int port;
	
	@Autowired
	EntityManagerFactory emf;
	
	/** Test configuration */
	@Autowired
	OrdermgrTestConfiguration config;
	
	/** The security environment for this test */
	//@Autowired
	OrdermgrSecurityConfig ordermgrSecurityConfig;
	
	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderControllerTest.class);
	
	/* Test orders */
	private static String[][] testMission = {
			//id,version,Code,Name,Processing_Mode,File_Class,Product_file_template
			{"1", "0", "ABCe", "ABCD Testing", "NRTI","OPER","test_file_temp"},	
			{ "11", "11", "DEFg", "DefrostMission","OFFL","OPER","test_file_temp"},
			{ "12", "12", "XY1Z", "XYZ Testing","RPRO","OPER","test_file_temp"}
			
	};
	
	private static String[][] testSpacecraft = {
			//version,code,name
			{"1","S_TDX1","Tandom-X"},
			{"2","S_TDX2","Tandem-X"},
			{ "3","S_TDX3","Terrasar-X"}
			
	};
	
	private static String[][] testFilterConditions = {
			//filter_conditions_key, parameter_type, parameter_value
			{ "copernicusCollection","STRING","99"},
			{ "revision","INTEGER","1"},
			{ "productColour","STRING","blue"}

	};
	
	private static String [][] testOutputParam = {
			//filter_conditions_key, parameter_type, parameter_value
			{ "copernicusCollection","STRING","99"},
			{ "copernicusCollection1","INTEGER","9"},
			{ "copernicusCollection2","STRING","999"},

			
	};
		
	private static String[][] testConfProc = {
			//identifier
			{"O3_TPR 02.00.00 OFFL 2019-10-11"},
			{"TROPICALIPF 01.00.00 SIR 2019-10-11"},	
	};
	
	private static String [][] testReqProdClass = {
			{"O3"},
			{"CLOUD"}
	};
	
	private static String  testInputProdClass = "L1B";
	private static String  testOutputFileClass= "RPRO";

	private static String [][] testReqOrbits = {
			//Spacecraft Code, OrbitNumber from, OrbitNumber to
			{"S5P", "8132", "8138" },
	        { "S5P", "8136", "8141" }
			
	};
	private static String testJob[][] = {
		//id,job_state,priority,orbit_id,processing_facility_id,filterconditions, outputparameters
			{"1111","INITIAL","1","15","Test Facility"}
	};
	private static String[][] testOrderData = {
			//order_id, order_version, execution_time, identifier, order_state, processing_mode,slice_duartion,slice_type,slice_overlapstart_time, stop_time
			{"111", "0", "2019-11-17T22:49:21.000000","XYZ","RUNNING","NRTI","PT20.345S","TIME_SLICE","0","2019-08-29T22:49:21.000000","2019-08-29T22:49:21.000000"},
			{"112", "0", "2019-11-18T20:04:20.000000","ABCDE","PLANNED","OFFL",null,"ORBIT","0","2019-02-20T22:49:21.000000","2019-05-29T20:29:11.000000"},
			{"113", "0", "2019-10-31T20:49:02.000000","XYZ1234","PLANNED","NRTI",null,"ORBIT","0","2019-01-02T02:40:21.000000","2019-04-29T18:29:10.000000"}
			
			
		};
	
	/**
	 * Create an order from a data array
	 * 
	 * @param testData an array of Strings representing the order to create
	 * @return a order with its attributes set to the input data
	 */
	private ProcessingOrder createOrder(String[] testData) {		
		logger.info("... creating order ");
		
		ProcessingOrder testOrder = new ProcessingOrder();
		if (null != RepositoryService.getOrderRepository().findByIdentifier(testData[3])) {
			logger.info("Found test order {}", testOrder.getId());
			return testOrder = RepositoryService.getOrderRepository().findByIdentifier(testData[3]);	
		}
		else{
			//Adding processing order parameters
			testOrder.setMission(RepositoryService.getMissionRepository().findByCode(testMission[0][2]));
			testOrder.setId(Long.parseLong(testData[0]));
			testOrder.setExecutionTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[2])));
			testOrder.setIdentifier(testData[3]);
			testOrder.setOrderState(OrderState.valueOf(testData[4]));
			testOrder.setProcessingMode(testData[5]);
			testOrder.setSlicingType(OrderSlicingType.valueOf(testData[7]));
			testOrder.setOutputFileClass(testOutputFileClass);

			//If Slice_TYpe is ORBIT then slice duration can be null
			if(testOrder.getSlicingType().toString().equals("TIME_SLICE")) {
				testOrder.setSliceDuration(Duration.parse(testData[6]));
			}
			testOrder.setSliceOverlap(Duration.ZERO);
			testOrder.setStartTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[9])));
			testOrder.setStopTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[10])));
			
			for (int i = 0; i < testFilterConditions.length; ++i) {
				Parameter filterCondition = new Parameter();
				filterCondition.init(ParameterType.valueOf(testFilterConditions[i][1]), testFilterConditions[i][2]);
				testOrder.getFilterConditions().put(testFilterConditions[i][0], filterCondition);
			}
			
			for (int i = 0; i < testOutputParam.length; ++i) {
				Parameter outputParam = new Parameter();
				outputParam.init(ParameterType.valueOf(testOutputParam[i][1]), testOutputParam[i][2]);
				testOrder.getOutputParameters().put(testOutputParam[i][0], outputParam);
			}

			for (int i = 0 ; i < testConfProc.length; ++i) {
				ConfiguredProcessor reqProc = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(testConfProc[i][0]);
				testOrder.getRequestedConfiguredProcessors().add(reqProc);				
			}
			
			for (ProductClass prodClass : RepositoryService.getProductClassRepository().findByProductType(testInputProdClass)){
				testOrder.getInputProductClasses().add(prodClass);

			}
	
			for (int i = 0; i < testReqProdClass.length; ++i) {				
				Set<ProductClass> set = new HashSet<ProductClass>(RepositoryService.getProductClassRepository().findByProductType(testReqProdClass[i][0]));
				testOrder.setRequestedProductClasses(set);			
			}
			for (int i = 0; i < testReqOrbits.length; ++i) {
				List<Orbit> orbits = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumberBetween(testReqOrbits[i][0], 
						Integer.valueOf(testReqOrbits[i][1]), Integer.valueOf(testReqOrbits[i][2]));
				testOrder.setRequestedOrbits(orbits);
			}
			testOrder = RepositoryService.getOrderRepository().save(testOrder);	
		
		}

		logger.info("Created test order {}", testOrder.getId());
		return testOrder;
	}
	
	/**
	 * Create test orders in the database
	 * 
	 * @return a list of orders generated
	 */
	
	private List<ProcessingOrder> createTestOrders() {
		logger.info("Creating test order");
		List<ProcessingOrder> testOrders = new ArrayList<>();		
		logger.info("Creating test orders of length: "+  testOrderData.length);

		for (int i = 0; i < testOrderData.length; ++i) {
			logger.info("Creating test order: "+ i +" "+ testOrderData[i][10]);

			testOrders.add(createOrder(testOrderData[i]));
		}
		return testOrders;
	}
	
	/**
	 * Remove all (remaining) test orders
	 * 
	 * @param testOrders a list of test orders to delete 
	 */
	private void deleteTestOrders(List<ProcessingOrder> testOrders) {
		for (ProcessingOrder testOrder: testOrders) {
			RepositoryService.getOrderRepository().delete(testOrder);
		}
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.createOrder(Order)}.
	 * 
	 * Test: Create a new order
	 */
	@Transactional
	@Test
	public final void testCreateOrder() {

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		transactionTemplate.execute(new TransactionCallback<>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				// Make sure a mission exists
				Mission mission = RepositoryService.getMissionRepository().findByCode(testMission[0][2]);
				if (null == mission) {
					mission = new Mission();
					mission.setCode(testMission[0][2]);
					mission.getProcessingModes().add(testMission[0][4]);
					mission.getFileClasses().add(testMission[0][5]);
					mission = RepositoryService.getMissionRepository().save(mission);
				}
				logger.info("Using mission " + mission.getCode() + " with id " + mission.getId());
				
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(testSpacecraft[0][1]);
				if (null == spacecraft ) {
					spacecraft = new Spacecraft();
					spacecraft.setCode(testSpacecraft[0][1]);
					spacecraft.setMission(mission);
					spacecraft.setName(testSpacecraft[0][2]);
					//orbits to be added
					spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
				}
			return null;
			}
			
		});
		
		List<ProcessingOrder> testOrders = new ArrayList<ProcessingOrder>() ;
		
		// Create an order in the database
		ProcessingOrder orderToCreate = createOrder(testOrderData[0]);
		testOrders.add(orderToCreate);
		RestOrder restOrder = OrderUtil.toRestOrder(orderToCreate);

		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders";
		logger.info("Testing URL {} / POST", testUrl);
		
		ResponseEntity<RestOrder> postEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.postForEntity(testUrl, restOrder, RestOrder.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, postEntity.getStatusCode());
		restOrder = postEntity.getBody();

		assertNotEquals("Id should not be 0 (zero): ", 0L, restOrder.getId().longValue());

		// Test that the mission exists
		testUrl += "/" + restOrder.getId();
		ResponseEntity<RestOrder> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestOrder.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());

		// Clean up database
		transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				deleteTestOrders(testOrders);
				return null;
			}
		});

		logger.info("Test OK: Create order");		
	}	

	
	/**
	 * Test method for { @link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.deleteOrderById(Long)}.
	 * 
	 * Test: Delete an Order by ID
	 * Precondition: An Order in the database
	 */
//	@Test
	public final void testDeleteOrderById() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		
		List<ProcessingOrder> testOrders = transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public List<ProcessingOrder> doInTransaction(TransactionStatus status) {
				
				List<ProcessingOrder> createOrders = new ArrayList<ProcessingOrder>();

				// Make sure a mission and spacecraft exists
				Mission mission = RepositoryService.getMissionRepository().findByCode(testMission[0][2]);
				if (null == mission) {
					mission = new Mission();
					mission.setCode(testMission[0][2]);
					mission.getProcessingModes().add(testMission[0][4]);
					mission.getFileClasses().add(testMission[0][5]);
					mission = RepositoryService.getMissionRepository().save(mission);
				}
				logger.info("Using mission " + mission.getCode() + " with id " + mission.getId());
				
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(testSpacecraft[0][1]);
				if (null == spacecraft ) {
					spacecraft = new Spacecraft();
					spacecraft.setCode(testSpacecraft[0][1]);
					spacecraft.setMission(mission);
					spacecraft.setName(testSpacecraft[0][2]);
					//orbits to be added
					spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
				}
				
				
				for (int i = 0; i < testOrderData.length; ++i) {
					ProcessingOrder order = RepositoryService.getOrderRepository().findByIdentifier(testOrderData[i][3]);
					if (order == null)
						createOrders.add(createOrder(testOrderData[i]));
					else
						createOrders.add(order);
				}
				return createOrders;
			}
		});

		ProcessingOrder orderToDelete = testOrders.get(0);
		
		// Delete the first test order
		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders/" + orderToDelete.getId();
		logger.info("Testing URL {} / DELETE", testUrl);
		
		new TestRestTemplate(config.getUserName(), config.getUserPassword()).delete(testUrl);
		
		// Test that the order is gone
		ResponseEntity<RestOrder> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestOrder.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, entity.getStatusCode());
		
		// Clean up database
		transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				deleteTestOrders(testOrders);
				return null;
			}
		});
		logger.info("Test OK: Delete Order By ID");
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.getOrderById(Long)}.
	 * 
	 * Test: Get an Order by ID
	 * Precondition: At least one order with a known ID is in the database
	 */
//	@Test
	public final void testGetOrderById() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		List<ProcessingOrder> testOrders = transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public List<ProcessingOrder> doInTransaction(TransactionStatus status) {
				
				List<ProcessingOrder> createOrders = new ArrayList<ProcessingOrder>();

				// Make sure a mission and spacecraft exists
				Mission mission = RepositoryService.getMissionRepository().findByCode(testMission[0][2]);
				if (null == mission) {
					mission = new Mission();
					mission.setCode(testMission[0][2]);
					mission.getProcessingModes().add(testMission[0][4]);
					mission.getFileClasses().add(testMission[0][5]);
					mission = RepositoryService.getMissionRepository().save(mission);
				}
				logger.info("Using mission " + mission.getCode() + " with id " + mission.getId());
				
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(testSpacecraft[0][1]);
				if (null == spacecraft ) {
					spacecraft = new Spacecraft();
					spacecraft.setCode(testSpacecraft[0][1]);
					spacecraft.setMission(mission);
					spacecraft.setName(testSpacecraft[0][2]);
					//orbits to be added
					spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
				}
				
				// Make sure test orders exist
				for (int i = 0; i < testOrderData.length; ++i) {
					ProcessingOrder order = RepositoryService.getOrderRepository().findByIdentifier(testOrderData[i][3]);
					if (order == null)
						createOrders.add(createOrder(testOrderData[i]));
					else
						createOrders.add(order);
				}
				return createOrders;
			}
		});
		
		//For test get the first order
		ProcessingOrder orderToFind = testOrders.get(0);
		
		// Test that a order can be read
		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders/" + orderToFind.getId();
		logger.info("Testing URL {} / GET", testUrl);

		ResponseEntity<RestOrder> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestOrder.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong orbit ID: ", orderToFind.getId(), getEntity.getBody().getId().longValue());
		
		// Clean up database
		transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				deleteTestOrders(testOrders);
				return null;
			}
		});
		

		logger.info("Test OK: Get Order By ID");
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.modifyOrder(Long, RestOrder)}.
	 * 
	 * Test: Update an Order by ID
	 * Precondition: At least one orbit with a known ID is in the database 
	 */
	
	@Transactional//Without this i get lazy initialization error 
	@Test
	public final void testModifyOrder() {
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		List<ProcessingOrder> testOrders = transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public List<ProcessingOrder> doInTransaction(TransactionStatus status) {
				
				List<ProcessingOrder> createOrders = new ArrayList<ProcessingOrder>();

				// Make sure a mission and spacecraft exists
				Mission mission = RepositoryService.getMissionRepository().findByCode(testMission[0][2]);
				if (null == mission) {
					mission = new Mission();
					mission.setCode(testMission[0][2]);
					mission.setName(testMission[0][3]);
					mission.getProcessingModes().add(testMission[0][4]);
					mission.setProductFileTemplate(testMission[0][6]);
					mission.getFileClasses().add(testMission[0][5]);
					mission = RepositoryService.getMissionRepository().save(mission);
				}
				logger.info("Using mission " + mission.getCode() + " with id " + mission.getId());
				
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(testSpacecraft[0][1]);
				if (null == spacecraft ) {
					spacecraft = new Spacecraft();
					spacecraft.setCode(testSpacecraft[0][1]);
					spacecraft.setMission(mission);
					spacecraft.setName(testSpacecraft[0][2]);
					//orbits to be added
					spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
				}
				
				for (int i = 0; i < testOrderData.length; ++i) {
					ProcessingOrder order = RepositoryService.getOrderRepository().findByIdentifier(testOrderData[i][3]);
					if (order == null)
						createOrders.add(createOrder(testOrderData[i]));
					else
						createOrders.add(order);
				}
				return createOrders;
			}
		});
			
		// Update  order attribute/s
		ProcessingOrder orderToModify = testOrders.get(0);
		orderToModify.setProcessingMode("OFFL_MOD");		
		//orderToModify.setIdentifier("Mod_XYZG");
		
		RestOrder restOrder = OrderUtil.toRestOrder(orderToModify);		

		String testUrl = "http://localhost:" + this.port  + ORDER_BASE_URI + "/orders/" + orderToModify.getId();
		logger.info("Testing URL {} / PATCH : {}", testUrl, restOrder.toString());

		restOrder = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.patchForObject(testUrl, restOrder, RestOrder.class);
		assertNotNull("Modified order not set", restOrder);

		// Test that the order attribute was changed as expected
		ResponseEntity<RestOrder> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestOrder.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong Start time: ", orderToModify.getStartTime(), getEntity.getBody().getStartTime().toInstant());
		assertEquals("Wrong Stop time: ", orderToModify.getStopTime(), getEntity.getBody().getStopTime().toInstant());
		assertEquals("Wrong Execution time: ", orderToModify.getExecutionTime(), getEntity.getBody().getExecutionTime().toInstant());
		
		assertEquals("Wrong Processing mode: ",  orderToModify.getProcessingMode(), getEntity.getBody().getProcessingMode());
		assertEquals("Wrong Identifier: ",  orderToModify.getIdentifier(), getEntity.getBody().getIdentifier());
//		assertEquals("Wrong Order state: ",  orderToModify.getOrderState().toString(), getEntity.getBody().getOrderState());
		assertEquals("Wrong Mission code: ",  orderToModify.getMission().getCode(), getEntity.getBody().getMissionCode());
		assertEquals("Wrong output file class: ",  orderToModify.getOutputFileClass(), getEntity.getBody().getOutputFileClass());
//
//		//Slice duration and type to be added
//		assertEquals("Wrong Slicing type: ",  orderToModify.getSlicingType().toString(), getEntity.getBody().getSlicingType());
//		assertEquals("Wrong Slicing duration in seconds: ",  orderToModify.getSliceDuration().getSeconds(), getEntity.getBody().getSliceDuration());

		
		
		// Clean up database
		transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				deleteTestOrders(testOrders);
				return null;
			}
		});

		logger.info("Test OK: Modify orbit");
	}


	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.getOrders(String, String, String[], Date, Date)}.
	 * 
	 * Test: List of all orders by mission, product class, start time range
	 * Precondition: For all selection criteria orders within and without a search value exist
	 */
	//@Test
	public final void testGetOrders() {

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		List<ProcessingOrder> testOrders = transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public List<ProcessingOrder> doInTransaction(TransactionStatus status) {
				
				List<ProcessingOrder> createOrders = new ArrayList<ProcessingOrder>();
				// Make sure a mission and spacecraft exists
				Mission mission = RepositoryService.getMissionRepository().findByCode(testMission[0][2]);
				if (null == mission) {
					mission = new Mission();
					mission.setCode(testMission[0][2]);
					mission.getProcessingModes().add(testMission[0][4]);
					mission.getFileClasses().add(testMission[0][5]);
					mission = RepositoryService.getMissionRepository().save(mission);
				}
				logger.info("Using mission " + mission.getCode() + " with id " + mission.getId());
				
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(testSpacecraft[0][1]);
				if (null == spacecraft ) {
					spacecraft = new Spacecraft();
					spacecraft.setCode(testSpacecraft[0][1]);
					spacecraft.setMission(mission);
					spacecraft.setName(testSpacecraft[0][2]);
					//orbits to be added
					spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
				}
				// Make sure test orders exist
				for (int i = 0; i < testOrderData.length; ++i) {
					ProcessingOrder order = RepositoryService.getOrderRepository().findByIdentifier(testOrderData[i][3]);
					if (order == null)
						createOrders.add(createOrder(testOrderData[i]));
					else
						createOrders.add(order);
				}
				return createOrders;
			}
		});
		
		
		// Get products using different selection criteria (also combined)
		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders";
		HttpHeaders headers = new HttpHeaders();

		HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

		
		// Build URI and Query parameters
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(testUrl)
				// Add query parameter
				.queryParam("mission", "ABCe")
				//.queryParam("identifier", "XYZ")
				//.queryParam("productClasses", "")
				.queryParam("executionTimeFrom", testOrderData[0][9].split("\\.")[0])
				.queryParam("executionTimeTo", testOrderData[1][10].split("\\.")[0]);

		logger.info("Testing URL {} / GET, no params, with user {} and password {}", builder.buildAndExpand().toUri(), config.getUserName(), config.getUserPassword());
				

		RestTemplate restTemplate = rtb.basicAuthentication(config.getUserName(), config.getUserPassword()).build();
		try {
			@SuppressWarnings("rawtypes")
			ResponseEntity<List> entity = restTemplate.exchange(builder.buildAndExpand().toUri(), HttpMethod.GET, requestEntity, List.class);
			
			assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
			
			// Test that the correct orders provided above are in the results
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> body = entity.getBody();
			logger.info("Found {} orders", body.size());
			
			boolean[] orderFound = new boolean[testOrders.size()];
			Arrays.fill(orderFound, false);
			for (Map<String, Object> order: body) {
				// Check, if any of the test orders was returned
				long orderId = (Integer) order.get("id");
				logger.info("... found product with ID {}", orderId);
				for (int i = 0; i < testOrders.size(); ++i) {
					ProcessingOrder testOrder = testOrders.get(i);
					if (orderId == testOrder.getId()) {
						orderFound[i] = true;
//					assertEquals("Wrong mode for test order " + i, testOrder.getProcessingMode(), order.get("processing mode"));
						assertEquals("Wrong identifier: "+ i,  testOrder.getIdentifier(), order.get("identifier"));
						
					}
				}
			}
			boolean[] expectedOrbitFound = new boolean[body.size()];
			Arrays.fill(expectedOrbitFound, true);
			int actualLength = 0;
			for(int i=0;i<orderFound.length;i++) {
				if(orderFound[i])
					actualLength++;			
			}
			assertEquals(expectedOrbitFound.length, actualLength);
		}  catch (Exception e) {	
			e.printStackTrace();
		}
		
		// Clean up database
		transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				deleteTestOrders(testOrders);
				return null;
			}
		});

		logger.info("Test OK: Get Orders");
	}

}
