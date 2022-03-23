/*
 * ProductionPlanner.java
 * 
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.planner.dispatcher.KubeDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.util.OrderPlanThread;
import de.dlr.proseo.planner.util.OrderReleaseThread;

/*
 * prosEO Planner application
 * 
 * @author Ernst Melchinger
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
//@Transactional
@EnableJpaRepositories("de.dlr.proseo.model.dao")
public class ProductionPlanner implements CommandLineRunner {
	
	private static Logger logger = LoggerFactory.getLogger(ProductionPlanner.class);
	
	/**
	 * Some constant definition for public use.
	 */
	public static final String jobNamePrefix = "proseojob";
	public static final String jobContainerPrefix = "proseocont";

	public static String hostName = "localhost";
	public static String hostIP = "127.0.0.1";
	public static String port = "8080";

	public static String PLAN_THREAD_PREFIX = "PlanOrder_";
	public static String RELEASE_THREAD_PREFIX = "ReleaseOrder_";
	
	public static ProductionPlannerConfiguration config;
	
	public static ProductionPlanner productionPlanner;
	

	/** 
	 * Planner configuration 
	 */
	@Autowired
	ProductionPlannerConfiguration plannerConfig;

	/**
	 * Current running KubeConfigs
	 */
	private Map<String, KubeConfig> kubeConfigs = new HashMap<>();
	
	/**
	 * Kube dispatcher
	 */
	private KubeDispatcher kubeDispatcher = null;
	
	
	/**
	 * password cache for orders (used to set user/pw in wrapper call)
	 */
	private Map<Long, Map<String, String>> orderPwCache = new HashMap<>();
	
	/**
	 * Current running order planning threads
	 */
	private Map<String, OrderPlanThread> planThreads = new HashMap<>();

	/**
	 * Current running order release threads
	 */
	private Map<String, OrderReleaseThread> releaseThreads = new HashMap<>();
	
	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/**
	 * Semaphore to avoid concurrent kubernetes job generation. 
	 */
	private Semaphore releaseSemaphore = new Semaphore(1);

	/**
	 * Semaphore to avoid concurrent db access of threads. 
	 */
	private Semaphore threadSemaphore = new Semaphore(1);

	/**
	 * @return the planThreads
	 */
	public Map<String, OrderPlanThread> getPlanThreads() {
		return planThreads;
	}

	/**
	 * @return the releaseThreads
	 */
	public Map<String, OrderReleaseThread> getReleaseThreads() {
		return releaseThreads;
	}

	/**
	 * @return the txManager
	 */
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	/**
	 * @return the em
	 */
	public EntityManager getEm() {
		return em;
	}

	/**
	 * @return the releaseSemaphore
	 */
	public Semaphore getReleaseSemaphore() {
		return releaseSemaphore;
	}
	

	/**
	 * @return the threadSemaphore
	 */
	public Semaphore getThreadSemaphore() {
		return threadSemaphore;
	}

	/**
	 * Get the user/pw for processing order
	 * 
	 * @param orderId Id of order
	 * @return user/pw
	 */
	public Map<String, String> getAuth(Long orderId) {
		return orderPwCache.get(orderId);
	}

	public void acquireReleaseSemaphore(String here) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> acquireReleaseSemaphore({})", here == null ? "null" : here);
		try {
			getReleaseSemaphore().acquire();
		} catch (InterruptedException e) {
			if (getReleaseSemaphore().availablePermits() <= 0) {
				getReleaseSemaphore().release();
			}
			if (logger.isTraceEnabled()) logger.trace("<<< acquireReleaseSemaphore({}) interrupted", here == null ? "null" : here);
			throw e;
		}
		if (logger.isTraceEnabled()) logger.trace("<<< acquireReleaseSemaphore({})", here == null ? "null" : here);
	}
	
	public void releaseReleaseSemaphore(String here) {
		if (logger.isTraceEnabled()) logger.trace(">>> releaseReleaseSemaphore({})", here == null ? "null" : here);
		if (getReleaseSemaphore().availablePermits() <= 0) {
			if (logger.isTraceEnabled()) logger.trace("    released({})", here);
			getReleaseSemaphore().release();
		} else {
			if (logger.isTraceEnabled()) logger.trace("    nothing to release({})", here == null ? "null" : here);
		}
	}

	public void acquireThreadSemaphore(String here) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> acquireThreadSemaphore({})", here == null ? "null" : here);
		try {
			getThreadSemaphore().acquire();
		} catch (InterruptedException e) {
			if (getThreadSemaphore().availablePermits() <= 0) {
				getThreadSemaphore().release();
			}
			if (logger.isTraceEnabled()) logger.trace("<<< acquireThreadSemaphore({}) interrupted", here == null ? "null" : here);
			throw e;
		}
		if (logger.isTraceEnabled()) logger.trace("<<< acquireThreadSemaphore({})", here == null ? "null" : here);
	}
	
	public void releaseThreadSemaphore(String here) {
		if (logger.isTraceEnabled()) logger.trace(">>> releaseThreadSemaphore({})", here == null ? "null" : here);
		if (getThreadSemaphore().availablePermits() <= 0) {
			if (logger.isTraceEnabled()) logger.trace("    released({})", here);
			getThreadSemaphore().release();
		} else {
			if (logger.isTraceEnabled()) logger.trace("    nothing to release({})", here == null ? "null" : here);
		}
	}

	/**
	 * Set or update user/pw of a processing order
	 * 
	 * @param orderId
	 * @param user 
	 * @param pw
	 */
	public void updateAuth(Long orderId, String user, String pw) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateAuth({}, user, password)", orderId);
		
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
	 * Look for connected KubeConfig of name. 
	 * 
	 * @param name of KubeConfig to find (may be null)
	 * @return KubeConfig found or null
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
	 * @return the collection of KubeConfigs which are connected.
	 */
	public Collection<KubeConfig> getKubeConfigs() {
		return kubeConfigs.values();
	}
	
	/**
	 * Initialize and run application 
	 * 
	 * @param args command line arguments
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception { 
		SpringApplication spa = new SpringApplication(ProductionPlanner.class);
		spa.run(args);
	}

	/**
	 * Walk through ProcessingFacility list of DB and try to connect each.
	 * Disconnect and remove KubeConfigs not defined in this list.
	 */
	@Transactional
	public void updateKubeConfigs() {
		if (logger.isTraceEnabled()) logger.trace(">>> updateKubeConfigs()");
		
		KubeConfig kubeConfig = null;
			
		for (ProcessingFacility pf : RepositoryService.getFacilityRepository().findAll()) {
			kubeConfig = getKubeConfig(pf.getName());
			if (kubeConfig != null) {
				if (!kubeConfig.connect()) {
					// error
					kubeConfigs.remove(pf.getName().toLowerCase());

					Messages.PLANNER_FACILITY_DISCONNECTED.log(logger, pf.getName());
				}
			}
			if (kubeConfig == null) {
				kubeConfig = new KubeConfig(pf, this);
				if (kubeConfig != null && kubeConfig.connect()) {
					kubeConfigs.put(pf.getName().toLowerCase(), kubeConfig);
					Messages.PLANNER_FACILITY_CONNECTED.log(logger, pf.getName(), pf.getProcessingEngineUrl());
					Messages.PLANNER_FACILITY_WORKER_CNT.log(logger, String.valueOf(kubeConfig.getWorkerCnt()));
				} else {
					Messages.PLANNER_FACILITY_NOT_CONNECTED.log(logger, pf.getName(), pf.getProcessingEngineUrl());
				}
			}
		}
		for (KubeConfig kf : getKubeConfigs()) {
			if (RepositoryService.getFacilityRepository().findByName(kf.getId().toLowerCase()) == null) {
				kubeConfigs.remove(kf.getId().toLowerCase());
				Messages.PLANNER_FACILITY_DISCONNECTED.log(logger, kf.getId(), kf.getProcessingEngineUrl());
			}
		}
	}
	
	/**
	 * Try to connect processing facility.
	 * Add new kube config for not connected facility, remove kube config for not connectable facility.
	 * 
	 * @param facilityName the name of the processing facility to connect
	 * @return the KubeConfig object for this processing facility, or null, if the facility is not connected
	 */
	@Transactional
	public KubeConfig updateKubeConfig(String facilityName) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateKubeConfig({})", facilityName);
		
		if (null == facilityName) {
			return null;
		}
		
		ProcessingFacility pf = RepositoryService.getFacilityRepository().findByName(facilityName);
		if (null == pf) {
			return null;
		}
		
		KubeConfig kubeConfig = getKubeConfig(pf.getName());
		
		if (null == kubeConfig) {
			// Planner does not know facility yet, so create a new KubeConfig object and make sure it can be connected
			kubeConfig = new KubeConfig(pf, this);
			if (kubeConfig.connect()) {
				kubeConfigs.put(pf.getName().toLowerCase(), kubeConfig);
				Messages.PLANNER_FACILITY_CONNECTED.log(logger, pf.getName(), pf.getProcessingEngineUrl());
				Messages.PLANNER_FACILITY_WORKER_CNT.log(logger, String.valueOf(kubeConfig.getWorkerCnt()));
			} else {
				Messages.PLANNER_FACILITY_NOT_CONNECTED.log(logger, pf.getName(), pf.getProcessingEngineUrl());
				kubeConfig = null;
			}
		} else {
			// Update information in KubeConfig object and make sure it can be connected
			kubeConfig.setFacility(pf);
			if (!kubeConfig.connect()) {
				// error
				kubeConfigs.remove(pf.getName().toLowerCase());
				Messages.PLANNER_FACILITY_DISCONNECTED.log(logger, pf.getName());
				kubeConfig = null;
			}
		}
		return kubeConfig;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
	 */
	@Override
	public void run(String... arg0) throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", arg0.toString());
      
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

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		try {
			@SuppressWarnings("unused")
			String dummy = transactionTemplate.execute((status) -> {
				this.updateKubeConfigs();
				return null;
			});
		} catch (TransactionException e) {
			e.printStackTrace();
		}
		
		this.startDispatcher();
	}

	
	/**
	 * Start the kube dispatcher thread
	 */
	public void startDispatcher() {
		if (logger.isTraceEnabled()) logger.trace(">>> startDispatcher()");
		
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
	 * Stop the kube dispatcher thread
	 */
	public void stopDispatcher() {
		if (logger.isTraceEnabled()) logger.trace(">>> stopDispatcher()");
		
		if (kubeDispatcher != null && kubeDispatcher.isAlive()) {
			kubeDispatcher.interrupt();
			int i = 0;
			while (kubeDispatcher.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		kubeDispatcher = null;
	}
}