/**
 * ProductionPlanner.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.dispatcher.KubeDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJobFinish;
import de.dlr.proseo.planner.util.OrderPlanThread;
import de.dlr.proseo.planner.util.OrderReleaseThread;
import de.dlr.proseo.planner.util.UtilService;

/**
 * prosEO planner application
 *
 * @author Ernst Melchinger
 *
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
//@Transactional(isolation = Isolation.REPEATABLE_READ)
@EnableJpaRepositories("de.dlr.proseo.model.dao")
public class ProductionPlanner implements CommandLineRunner {

	private static ProseoLogger logger = new ProseoLogger(ProductionPlanner.class);

	// Some constant definitions for public use
	public static final String jobNamePrefix = "proseojob";
	public static final String jobContainerPrefix = "proseocont";

	public static String hostName = "localhost";
	public static String hostIP = "127.0.0.1";
	public static String port = "8080";

	public static String PLAN_THREAD_PREFIX = "PlanOrder_";
	public static String RELEASE_THREAD_PREFIX = "ReleaseOrder_";

	public static String STATE_MESSAGE_COMPLETED = "requested output product is available";
	public static String STATE_MESSAGE_QUEUED = "request is queued for processing";
	public static String STATE_MESSAGE_RUNNING = "request is under processing";
	public static String STATE_MESSAGE_CANCELLED = "request cancelled by user";
	public static String STATE_MESSAGE_FAILED = "production has failed";
	public static String STATE_MESSAGE_NO_INPUT_AVAILABLE = "input product currently unavailable";
	public static String STATE_MESSAGE_NO_INPUT = "input product not found on LTA";

	/** The static production planner configuration */
	public static ProductionPlannerConfiguration config;

	/** The production planner instance */
	public static ProductionPlanner productionPlanner;

	/** The instance production planner configuration */
	@Autowired
	ProductionPlannerConfiguration plannerConfig;

	/** Currently running Kubernetes configurations */
	private Map<String, KubeConfig> kubeConfigs = new HashMap<>();

	/** Kubernetes dispatcher looking for runnable job steps */
	private KubeDispatcher kubeDispatcher = null;

	/** Password cache for orders (used to set username/password in wrapper call) */
	private Map<Long, Map<String, String>> orderPwCache = new HashMap<>();

	/** Currently running order planning threads */
	private Map<String, OrderPlanThread> planThreads = new HashMap<>();

	/** Currently running order finishing threads */
	private Map<String, KubeJobFinish> finishThreads = new HashMap<>();

	/** Currently running order release threads */
	private Map<String, OrderReleaseThread> releaseThreads = new HashMap<>();

	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** Collection of all orders in state SUSPENDING at planner start */
	private List<Long> suspendingOrders = new ArrayList<>();

	/** Collection of all orders in state RELEASING at planner start */
	private List<Long> releasingOrders = new ArrayList<>();

	/** Collection of all orders in state PLANNING at planner start */
	private List<Long> planningOrders = new ArrayList<>();

	/**
	 * Gets the finishThreads map.
	 *
	 * @return The map of finishing threads.
	 */
	public Map<String, KubeJobFinish> getFinishThreads() {
		return finishThreads;
	}

	/**
	 * Gets the suspendingOrders list.
	 *
	 * @return The list of suspending orders.
	 */
	public List<Long> getSuspendingOrders() {
		return suspendingOrders;
	}

	/**
	 * Gets the releasingOrders list.
	 *
	 * @return The list of releasing orders.
	 */
	public List<Long> getReleasingOrders() {
		return releasingOrders;
	}

	/**
	 * Gets the planningOrders list.
	 *
	 * @return The list of planning orders.
	 */
	public List<Long> getPlanningOrders() {
		return planningOrders;
	}

	/**
	 * Gets the planThreads map.
	 *
	 * @return The map of planning threads.
	 */
	public Map<String, OrderPlanThread> getPlanThreads() {
		return planThreads;
	}

	/**
	 * Gets the releaseThreads map.
	 *
	 * @return The map of release threads.
	 */
	public Map<String, OrderReleaseThread> getReleaseThreads() {
		return releaseThreads;
	}

	/**
	 * Gets the transaction manger.
	 *
	 * @return The PlatformTransactionManager instance.
	 */
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	/**
	 * Gets the entity manager.
	 *
	 * @return The EntityManager instance.
	 */
	public EntityManager getEm() {
		return em;
	}

	/**
	 * Gets the authentication details of an order (username and password).
	 *
	 * @param orderId The ID of the order.
	 * @return The authentication details.
	 */
	public Map<String, String> getAuth(Long orderId) {
		return orderPwCache.get(orderId);
	}

	/**
	 * Updates authentication details for an order).
	 *
	 * @param orderId The ID of the order.
	 * @param user    The username.
	 * @param pw      The password.
	 */
	public void updateAuth(Long orderId, String user, String pw) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateAuth({}, user, password)", orderId);

		if (orderPwCache.containsKey(orderId)) {
			Map<String, String> aMap = orderPwCache.get(orderId);
			if (aMap.containsKey(user)) {
				if (!aMap.get(user).equals(pw)) {
					aMap.replace(user, pw);
				}
			} else {
				aMap.remove(user);
				aMap.put(user, pw);
			}
		} else {
			Map<String, String> aMap = new HashMap<>();
			aMap.put(user, pw);
			orderPwCache.put(orderId, aMap);
		}
	}

	/**
	 * Gets the KubeConfig instance by name.
	 *
	 * @param name The name of the KubeConfig instance (may be null).
	 * @return The KubeConfig instance.
	 */
	public KubeConfig getKubeConfig(String name) {
		if (name == null) {
			if (0 < kubeConfigs.size()) {
				return (KubeConfig) kubeConfigs.values().toArray()[0];
			} else {
				return null;
			}
		}
		return kubeConfigs.get(name.toLowerCase());
	}

	/**
	 * Gets all connected KubeConfig instances.
	 *
	 * @return Collection of KubeConfig instances.
	 */
	public Collection<KubeConfig> getKubeConfigs() {
		return kubeConfigs.values();
	}

	/**
	 * Main method to start the application.
	 *
	 * @param args Command line arguments.
	 * @throws Exception Exception if an error occurs.
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication spa = new SpringApplication(ProductionPlanner.class);
		spa.run(args);
	}

	/**
	 * Walk through ProcessingFacility list of DB and try to connect each.
	 * Disconnect and remove KubeConfigs not defined in this list.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public void updateKubeConfigs() {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateKubeConfigs()");

		KubeConfig kubeConfig = null;

		for (ProcessingFacility processingFacility : RepositoryService.getFacilityRepository().findAll()) {
			kubeConfig = getKubeConfig(processingFacility.getName());
			if (kubeConfig != null) {
				if (!kubeConfig.connect()) {
					// error
					kubeConfigs.remove(processingFacility.getName().toLowerCase());

					logger.log(PlannerMessage.PLANNER_FACILITY_DISCONNECTED, processingFacility.getName());
				}
			}
			if (kubeConfig == null) {
				kubeConfig = new KubeConfig(processingFacility, this);
				if (kubeConfig.getFacilityState(processingFacility) == FacilityState.RUNNING
						|| kubeConfig.getFacilityState(processingFacility) == FacilityState.STARTING
						|| kubeConfig.getFacilityState(processingFacility) == FacilityState.STOPPING) {
					if (kubeConfig != null && kubeConfig.connect()) {
						kubeConfigs.put(processingFacility.getName().toLowerCase(), kubeConfig);
						logger.log(PlannerMessage.PLANNER_FACILITY_CONNECTED, processingFacility.getName(),
								processingFacility.getProcessingEngineUrl());
						logger.log(PlannerMessage.PLANNER_FACILITY_WORKER_CNT, String.valueOf(kubeConfig.getWorkerCnt()));
					} else {
						logger.log(PlannerMessage.PLANNER_FACILITY_NOT_CONNECTED, processingFacility.getName(),
								processingFacility.getProcessingEngineUrl());
					}
				}
			}
			for (KubeConfig kf : getKubeConfigs()) {
				if (RepositoryService.getFacilityRepository().findByName(kf.getId().toLowerCase()) == null) {
					kubeConfigs.remove(kf.getId().toLowerCase());
					logger.log(PlannerMessage.PLANNER_FACILITY_DISCONNECTED, kf.getId(), kf.getProcessingEngineUrl());
				}
			}
		}
	}

	/**
	 * Attempts to connect to a processing facility. Adds a new Kubernetes configuration for the previously not connected facility,
	 * removes Kubernetes configuration for a facility to which a connection is not possible.
	 *
	 * @param facilityName the name of the processing facility to connect
	 * @return the KubeConfig object for this processing facility, or null, if the facility is not connected
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public KubeConfig updateKubeConfig(String facilityName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateKubeConfig({})", facilityName);

		if (facilityName == null) {
			return null;
		}

		ProcessingFacility processingFacility = RepositoryService.getFacilityRepository().findByName(facilityName);
		if (processingFacility == null) {
			try {
				Long anId = Long.valueOf(facilityName);
				if (anId > 0) {
					Optional<ProcessingFacility> opt =RepositoryService.getFacilityRepository().findById(anId);
					if (opt != null) {
						processingFacility = opt.get();
					} else {
						return null;
					}
				} else {
					return null;
				}
			} catch (NumberFormatException e) {
				return null;
			}
		}

		KubeConfig kubeConfig = getKubeConfig(processingFacility.getName());

		if (kubeConfig == null) {
			// Planner does not know facility yet, so create a new KubeConfig object and make sure it can be connected
			kubeConfig = new KubeConfig(processingFacility, this);
			if (kubeConfig.getFacilityState(processingFacility) == FacilityState.RUNNING
					|| kubeConfig.getFacilityState(processingFacility) == FacilityState.STARTING
					|| kubeConfig.getFacilityState(processingFacility) == FacilityState.STOPPING) {
				if (kubeConfig.connect()) {
					kubeConfigs.put(processingFacility.getName().toLowerCase(), kubeConfig);
					logger.log(PlannerMessage.PLANNER_FACILITY_CONNECTED, processingFacility.getName(),
							processingFacility.getProcessingEngineUrl());
					logger.log(PlannerMessage.PLANNER_FACILITY_WORKER_CNT, String.valueOf(kubeConfig.getWorkerCnt()));
				} else {
					logger.log(PlannerMessage.PLANNER_FACILITY_NOT_CONNECTED, processingFacility.getName(),
							processingFacility.getProcessingEngineUrl());
				}
			}
		} else {
			// Update information in KubeConfig object and make sure it can be connected
			kubeConfig.setFacility(processingFacility);
			if (kubeConfig.getFacilityState(processingFacility) == FacilityState.RUNNING
					|| kubeConfig.getFacilityState(processingFacility) == FacilityState.STARTING
					|| kubeConfig.getFacilityState(processingFacility) == FacilityState.STOPPING) {
				if (!kubeConfig.connect()) {
					// error
					kubeConfigs.remove(processingFacility.getName().toLowerCase());
					logger.log(PlannerMessage.PLANNER_FACILITY_DISCONNECTED, processingFacility.getName());
				}
			}
		}
		return kubeConfig;
	}

	/**
	 * Collect the orders in state SUSPENDING, RELEASING and PLANNING, store their IDs and restart the first.
	 */
	private void checkForRestartSuspend() {
		List<ProcessingOrder> orders = RepositoryService.getOrderRepository().findByOrderState(OrderState.SUSPENDING);
		for (ProcessingOrder order : orders) {
			// Resume the order
			suspendingOrders.add(order.getId());
			if (logger.isTraceEnabled())
				logger.trace(">>> suspending order found ({})", order.getId());
		}
		UtilService.getOrderUtil().checkNextForRestart();
	}

	/**
	 * Checks for orders to restart that are in 'releasing' or 'planning' state.
	 */
	private void checkForRestart() {
		List<ProcessingOrder> orders = RepositoryService.getOrderRepository().findByOrderState(OrderState.RELEASING);
		for (ProcessingOrder order : orders) {
			// Resume the order
			releasingOrders.add(order.getId());
			if (logger.isTraceEnabled())
				logger.trace(">>> releasing order found ({})", order.getId());
		}
		orders = RepositoryService.getOrderRepository().findByOrderState(OrderState.PLANNING);
		for (ProcessingOrder order : orders) {
			// Resume the order
			planningOrders.add(order.getId());
			if (logger.isTraceEnabled())
				logger.trace(">>> planning order found ({})", order.getId());
		}
		UtilService.getOrderUtil().checkNextForRestart();
	}

	/**
	 * Checks for the next order to restart.
	 */
	public void checkNextForRestart() {
		UtilService.getOrderUtil().checkNextForRestart();
	}

	/**
	 * Runs the application, initializing necessary configurations and components.
	 *
	 * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
	 * @param arg0 Command line arguments.
	 * @throws Exception If an error occurs during execution.
	 */
	@Override
	public void run(String... arg0) throws Exception {
		if (logger.isTraceEnabled())
			logger.trace(">>> run({})", arg0.toString());

		InetAddress ip;
		String hostname;
		config = plannerConfig;
		productionPlanner = this;
		try {
			ip = InetAddress.getLocalHost();
			hostname = ip.getHostName();
			hostIP = ip.getHostAddress();
			hostName = hostname;
			System.out.println("Your current IP address : " + ip);
			System.out.println("Your current Hostname : " + hostname);
			System.out.println("jobStepSort : " + config.getJobStepSort());

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		try {
			@SuppressWarnings("unused")
			String dummy = transactionTemplate.execute((status) -> {
				this.updateKubeConfigs();
				return null;
			});
		} catch (TransactionException e) {
			e.printStackTrace();
		}

		// Continue order suspension first
		checkForRestartSuspend();

		this.startDispatcher();

		checkForRestart();
	}

	/**
	 * Starts the Kubernetes dispatcher thread.
	 */
	public void startDispatcher() {
		if (logger.isTraceEnabled())
			logger.trace(">>> startDispatcher()");

		if (kubeDispatcher == null || !kubeDispatcher.isAlive()) {
			kubeDispatcher = new KubeDispatcher(this, null, false);
			kubeDispatcher.start();
		} else {
			if (kubeDispatcher.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stops the Kubernetes dispatcher thread.
	 */
	public void stopDispatcher() {
		if (logger.isTraceEnabled())
			logger.trace(">>> stopDispatcher()");

		if (kubeDispatcher != null && kubeDispatcher.isAlive()) {
			kubeDispatcher.interrupt();
			int i = 0;
			while (kubeDispatcher.isAlive() && i < 100) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		kubeDispatcher = null;
	}
}
