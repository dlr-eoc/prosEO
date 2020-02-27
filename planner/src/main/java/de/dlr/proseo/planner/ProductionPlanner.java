/*
 * ProductionPlanner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
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
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.planner.dispatcher.KubeDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.util.JobStepUtil;

/*
 * prosEO Ingestor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
//@Transactional
@EnableJpaRepositories("de.dlr.proseo.model.dao")
public class ProductionPlanner implements CommandLineRunner {
	
	private static final String MSG_PREFIX = "199 proseo-planner: ";
	private static final String MSG_PLANNER_PROCESSING_FACILITY_CONNECTED = MSG_PREFIX + "ProcessingFacility '%s' connected to '%s'";
	private static final String MSG_PLANNER_PROCESSING_FACILITY_WORKER_CNT = MSG_PREFIX + "%d worker nodes found";
	private static final String MSG_PLANNER_PROCESSING_FACILITY_DISCONNECTED = MSG_PREFIX + "ProcessingFacility '%s' disconnected";
	private static final String MSG_PLANNER_PROCESSING_FACILITY_NOT_CONNECTED = MSG_PREFIX + "ProcessingFacility '%s' could not be connected to '%s'";
	
	private static Logger logger = LoggerFactory.getLogger(ProductionPlanner.class);

	public static final String jobNamePrefix = "proseojob";
	public static final String jobContainerPrefix = "proseocont";

	public static String hostName = "localhost";
	public static String hostIP = "127.0.0.1";
	public static String port = "8080";
	
	public static ProductionPlannerConfiguration config;
	

	/** Planner configuration */
	@Autowired
	ProductionPlannerConfiguration plannerConfig;
    @Autowired
    private JobStepUtil jobStepUtil;

	/**
	 * Current running KubeConfigs
	 */
	private Map<String, KubeConfig> kubeConfigs = new HashMap<>();
	
	private KubeDispatcher kubeDispatcher = null;
	
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
	@Transactional
	public void checkForJobStepsToRun() {
		jobStepUtil.checkForJobStepsToRun();
	};
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
	public void updateKubeConfigs() {
		boolean found = false;
		KubeConfig kubeConfig = null;
		for (ProcessingFacility pf : RepositoryService.getFacilityRepository().findAll()) {
			kubeConfig = getKubeConfig(pf.getName());
			if (kubeConfig != null) {
				if (kubeConfig.connect()) {
					found = true;
				} else {
					// error
					kubeConfigs.remove(pf.getName().toLowerCase());

					String message = String.format(MSG_PLANNER_PROCESSING_FACILITY_DISCONNECTED, pf.getName());
					logger.error(message);
				}
			}
			if (kubeConfig == null) {
				kubeConfig = new KubeConfig(pf);
				if (kubeConfig != null && kubeConfig.connect()) {
					kubeConfigs.put(pf.getName().toLowerCase(), kubeConfig);
					found = true;
					String message = String.format(MSG_PLANNER_PROCESSING_FACILITY_CONNECTED, pf.getName(), pf.getProcessingEngineUrl());
					logger.info(message);
					message = String.format(MSG_PLANNER_PROCESSING_FACILITY_WORKER_CNT, kubeConfig.getWorkerCnt());
					logger.info(message);
				} else {
					String message = String.format(MSG_PLANNER_PROCESSING_FACILITY_NOT_CONNECTED, pf.getName(), pf.getProcessingEngineUrl());
					logger.error(message);
				}
			}
		}
		for (KubeConfig kf : getKubeConfigs()) {
			if (RepositoryService.getFacilityRepository().findByName(kf.getId().toLowerCase()) == null) {
				kubeConfigs.remove(kf.getId().toLowerCase());
				String message = String.format(MSG_PLANNER_PROCESSING_FACILITY_DISCONNECTED, kf.getId(), kf.getProcessingEngineUrl());
				logger.info(message);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
	 */
	@Override
	public void run(String... arg0) throws Exception {
		//		
		//		List<String> pfs = new ArrayList<String>();
		//		
		//        for (int i = 0; i < arg0.length; i++) {
		//        	if (arg0[i].equalsIgnoreCase("-processingfacility") && (i + 1) < arg0.length) {
		//        		pfs.add(arg0[i+1]);
		//        	}
		//        } 
      
		InetAddress ip;
		String hostname;
		TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
		config = plannerConfig;
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
		this.updateKubeConfigs();
		this.startDispatcher();
	}

	
	public void startDispatcher() {
		if (kubeDispatcher == null || !kubeDispatcher.isAlive()) {
			kubeDispatcher = new KubeDispatcher(this, jobStepUtil);
			kubeDispatcher.start();
		} else {
			if (kubeDispatcher.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}
	
	public void stopDispatcher() {
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