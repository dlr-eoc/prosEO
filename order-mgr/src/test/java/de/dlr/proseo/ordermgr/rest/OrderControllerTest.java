package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.ProcessingOrder.OrderSlicingType;
import de.dlr.proseo.model.ProcessingOrder.OrderState;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.OrderManager;
import de.dlr.proseo.ordermgr.OrdermgrSecurityConfig;
import de.dlr.proseo.ordermgr.rest.model.OrderUtil;
import de.dlr.proseo.ordermgr.rest.model.RestOrder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
//@Transactional
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
			//id,version,Code,Name,Processing_Mode,File_Class
			{"1", "0", "ABCe", "ABCD Testing", "NRTI","OPER"},	
			{ "11", "11", "DEFg", "DefrostMission","OFFL","OPER"},
			{ "12", "12", "XY1Z", "XYZ Testing","RPRO","OPER"}
			
	};
	
	private static String[][] testSpacecraft = {
			//version,code,name
			{"1","S_TDX1","Tandom-X"},
			{"2","S_TDX2","Tandem-X"},
			{ "3","S_TDX3","Terrasar-X"}
			
	};
	
//	private static String[][] testOrderData = {
//		// mission_id, mission_version, mission_code, mission_name,spacecraft_version,spacecraft_code,spacecraft_name, order_id, order_version, execution_time, identifier, order_state, processing_mode,slice_duartion,slice_type,start_time, stop_time,output_file_class
//		{ testMission[0][0], testMission[0][1], testMission[0][2], testMission[0][3], testSpacecraft[0][0],testSpacecraft[0][1], testSpacecraft[0][2],"111", "0", "2019-11-17T22:49:21.000000","XYZ","RUNNING","NRTI","30","ORBIT","2019-08-29T22:49:21.000000","2019-08-29T22:49:21.000000","TEST"},
//		{testMission[1][0], testMission[1][1],testMission[1][2], testMission[1][3], testSpacecraft[1][0],testSpacecraft[1][1], testSpacecraft[1][2], "112", "0", "2019-11-18T20:04:20.000000","ABCDE","PLANNED","OFFL",null,"ORBIT","2019-02-20T22:49:21.000000","2019-05-29T20:29:11.000000","TEST"},
//		{ testMission[2][0], testMission[2][1], testMission[2][2], testMission[2][3], testSpacecraft[2][0],testSpacecraft[2][1], testSpacecraft[2][2],	"113", "0", "2019-10-31T20:49:02.000000","XYZ1234","PLANNED","NRTI",null,"ORBIT","2019-01-02T02:40:21.000000","2019-04-29T18:29:10.000000","TEST"}
//		
//		
//	};

	
	
	private static String[][] testFilterConditions = {
			//filter_conditions_key, parameter_type, parameter_value
			{ "copernicusCollection","STRING","99"},
			{ "revision","INTEGER","1"},
			{ "productColour","STRING","blue"}

	};
	
	private static String [][] testOutputParam = {
			//processing_order_id, parameter_type, parameter_value, output_parameters_key
			{ "copernicusCollection1","revision1","fileClass1"},
			{ "copernicusCollection2","revision2","fileClass2"},
			{ "copernicusCollection3","revision3","fileClass3"}
			
	};
	
	private static String[][] testConfProc = {
			{"KNMI L2 01.03.02 2019-07-03", "DLR L2 01.01.05 2019-07-03"},
			{"KNMI L2 01.03.02 2019-07-04", "DLR L2 01.01.05 2019-07-04"},	
			{"KNMI L2 01.03.02 2019-07-05", "DLR L2 01.01.05 2019-07-05"}	
	};
	
	private static String [][] testReqProdClass = {
			{"O3", "CLOUD", "FRESCO", "AAI"}	
	};
	
	private static String [][] testInputProdClass = {
			{"L1B"}	
	};
	
	private static String [][] testReqOrbits = {
			//Spacecraft Code, OrbitNumber from, OrbitNumber to
			{"S5P", "4567", "5330" },
	        { "S5P", "5421", "5678" }
			
	};
	private static String[][] testOrderData = {
			//order_id, order_version, execution_time, identifier, order_state, processing_mode,slice_duartion,slice_type,start_time, stop_time,output_file_class,outputparam_key,OutPutParama_value,PutputParam_Type
			{"111", "0", "2019-11-17T22:49:21.000000","XYZ","RUNNING","NRTI","30","ORBIT","2019-08-29T22:49:21.000000","2019-08-29T22:49:21.000000","TEST",testOutputParam[0][2],testOutputParam[0][1],testOutputParam[0][0]},
			{"112", "0", "2019-11-18T20:04:20.000000","ABCDE","PLANNED","OFFL",null,"ORBIT","2019-02-20T22:49:21.000000","2019-05-29T20:29:11.000000","TEST",testOutputParam[1][2],testOutputParam[0][1],testOutputParam[1][0]},
			{"113", "0", "2019-10-31T20:49:02.000000","XYZ1234","PLANNED","NRTI",null,"ORBIT","2019-01-02T02:40:21.000000","2019-04-29T18:29:10.000000","TEST",testOutputParam[2][2],testOutputParam[0][1],testOutputParam[2][0]}
			
			
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
		if (null != RepositoryService.getOrderRepository().findByIdentifier(testData[10])) {
			logger.info("Found test order {}", testOrder.getId());
			return testOrder = RepositoryService.getOrderRepository().findByIdentifier(testData[10]);	
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
//			//If Slice_TYpe is ORBIT then slice duration can be null
//			//To be filled only if Slice_TYpe is TIME_SLICE
//			if(testOrder.getSlicingType().toString().equals("ORBIT"))
//			testOrder.setSliceDuration(null);
			testOrder.setStartTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[8])));
			testOrder.setStopTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(testData[9])));
			testOrder.setOutputFileClass(testData[10]);
			//Filtercondiitons,confProcessors,orbits.o/p parameter reqProductCLasses,inputProducClasses,sliceoverlap
			
			for (int i = 0; i < testFilterConditions.length; ++i) {
				Parameter filterCondition = new Parameter();
				filterCondition.init(ParameterType.valueOf(testFilterConditions[i][1]), testFilterConditions[i][2]);
				testOrder.getFilterConditions().put(testFilterConditions[i][0], filterCondition);
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
		Session session = emf.unwrap(SessionFactory.class).openSession();
		for (ProcessingOrder testOrder: testOrders) {
			testOrder = (ProcessingOrder) session.merge(testOrder);
			de.dlr.proseo.model.Mission mission = testOrder.getMission();
			Set<de.dlr.proseo.model.Spacecraft> spacecrafts = testOrder.getMission().getSpacecrafts();
			
			RepositoryService.getOrderRepository().delete(testOrder);
//			for(de.dlr.proseo.model.Spacecraft spacecraft : spacecrafts) {
//				RepositoryService.getSpacecraftRepository().delete(spacecraft);
//			}
//
//			RepositoryService.getMissionRepository().deleteById(mission.getId());

		}
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.createOrder(Order)}.
	 * 
	 * Test: Create a new order
	 */
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
		
		// Create an order in the database
		ProcessingOrder orderToCreate = createOrder(testOrderData[2]);
		
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
		ArrayList<ProcessingOrder> testOrder = new ArrayList<>();
		testOrder.add(orderToCreate);
		//deleteTestOrders(testOrder);

		logger.info("Test OK: Create order");		
	}	

	
	/**
	 * Test method for { @link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.deleteOrderById(Long)}.
	 * 
	 * Test: Delete an Order by ID
	 * Precondition: An Order in the database
	 */
	@Test
	public final void testDeleteOrderById() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		
		ProcessingOrder orderToDelete = transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public ProcessingOrder doInTransaction(TransactionStatus status) {
				
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
				
				ProcessingOrder order = RepositoryService.getOrderRepository().findByIdentifier(testOrderData[0][3]);
				if (order == null)
				return createOrder(testOrderData[0]);
				
				else return order;
			}
		});

//		List<ProcessingOrder> testOrders = createTestOrders();
//		ProcessingOrder orderToDelete = testOrders.get(0);
//		testOrders.remove(0);
		
		// Delete the first test order
		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders/" + orderToDelete.getId();
		logger.info("Testing URL {} / DELETE", testUrl);
		
		new TestRestTemplate(config.getUserName(), config.getUserPassword()).delete(testUrl);
		
		// Test that the order is gone
		ResponseEntity<RestOrder> entity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestOrder.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, entity.getStatusCode());
		
		// Clean up database
		//deleteTestOrders(testOrders);

		logger.info("Test OK: Delete Order By ID");
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.getOrderById(Long)}.
	 * 
	 * Test: Get an Order by ID
	 * Precondition: At least one order with a known ID is in the database
	 */
	@Test
	public final void testGetOrderById() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		ProcessingOrder orderToFind = transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public ProcessingOrder doInTransaction(TransactionStatus status) {
				
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
				
				ProcessingOrder order = RepositoryService.getOrderRepository().findByIdentifier(testOrderData[0][3]);
				if (order == null)
				return createOrder(testOrderData[0]);
				
				else return order;
			}
		});
		

//		// Make sure test orbits exist
//		List<ProcessingOrder> testOrders = createTestOrders();
//		ProcessingOrder orderToFind = testOrders.get(0);

		// Test that a order can be read
		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders/" + orderToFind.getId();
		logger.info("Testing URL {} / GET", testUrl);

		ResponseEntity<RestOrder> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestOrder.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong orbit ID: ", orderToFind.getId(), getEntity.getBody().getId().longValue());
		
//		// Clean up database
//		deleteTestOrders(testOrders);

		logger.info("Test OK: Get Order By ID");
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl.modifyOrder(Long, RestOrder)}.
	 * 
	 * Test: Update an Order by ID
	 * Precondition: At least one orbit with a known ID is in the database 
	 */
	
/*	@Test
	public final void testModifyOrder() {
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		ProcessingOrder orderToModify = transactionTemplate.execute(new TransactionCallback<>() {
			@Override
			public ProcessingOrder doInTransaction(TransactionStatus status) {
				
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
				
				ProcessingOrder order = RepositoryService.getOrderRepository().findByIdentifier(testOrderData[0][3]);
				if (order == null)
				return createOrder(testOrderData[0]);
				
				else return order;
			}
		});
		
//		// Make sure test orbits exist
//		List<ProcessingOrder> testOrders = createTestOrders();
//		ProcessingOrder orderToModify = testOrders.get(0);
		
		// Update a orbit attribute
		orderToModify.setIdentifier("Mod_XYZG");
		
		RestOrder restOrder = OrderUtil.toRestOrder(orderToModify);		
		logger.info("RestOrder modified identifier: "+restOrder.getIdentifier());

		String testUrl = "http://localhost:" + this.port + ORDER_BASE_URI + "/orders/" + orderToModify.getId();
		logger.info("Testing URL {} / PATCH", testUrl);

		restOrder = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.patchForObject(testUrl, restOrder, RestOrder.class);
		assertNotNull("Modified order not set", restOrder);

		// Test that the orbit attribute was changed as expected
		ResponseEntity<RestOrder> getEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.getForEntity(testUrl, RestOrder.class);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, getEntity.getStatusCode());
		assertEquals("Wrong Start time: ", Orbit.orbitTimeFormatter.format(orderToModify.getStartTime()), getEntity.getBody().getStartTime());
		assertEquals("Wrong Stop time: ", Orbit.orbitTimeFormatter.format(orderToModify.getStopTime()), getEntity.getBody().getStopTime());
		assertEquals("Wrong Execution time: ", Orbit.orbitTimeFormatter.format(orderToModify.getExecutionTime()), getEntity.getBody().getExecutionTime());
		
		assertEquals("Wrong Processing mode: ",  orderToModify.getProcessingMode(), getEntity.getBody().getProcessingMode());
		assertEquals("Wrong Identifier: ",  orderToModify.getIdentifier(), getEntity.getBody().getIdentifier());
//		assertEquals("Wrong Order state: ",  orderToModify.getOrderState().toString(), getEntity.getBody().getOrderState());
//		assertEquals("Wrong Mission id: ",  orderToModify.getMission().getCode(), getEntity.getBody().getMissionCode());
//		//assertEquals("Wrong output file class: ",  orderToModify.getOutputFileClass(), getEntity.getBody().getOutputFileClass());
//
//		//Slice duration and type to be added
//		assertEquals("Wrong Slicing type: ",  orderToModify.getSlicingType().toString(), getEntity.getBody().getSlicingType());
//		assertEquals("Wrong Slicing duration in seconds: ",  orderToModify.getSliceDuration().getSeconds(), getEntity.getBody().getSliceDuration());

		
		
		// Clean up database
//		deleteTestOrders(testOrders);

		logger.info("Test OK: Modify orbit");
	}

*/
}
