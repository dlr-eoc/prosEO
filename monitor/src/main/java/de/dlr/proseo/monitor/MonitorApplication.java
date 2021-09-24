/**
 * Ingestor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.monitor.microservice.Monitor;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.rest.model.RestOrder;

/*
 * prosEO Planner application
 * 
 * @author Ernst Melchinger
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@EnableJpaRepositories("de.dlr.proseo.model.dao")
public class MonitorApplication implements CommandLineRunner {
	
	private static Logger logger = LoggerFactory.getLogger(MonitorApplication.class);
	
	/**
	 * Some constant definition for public use.
	 */
	public static final String jobNamePrefix = "proseojob";
	public static final String jobContainerPrefix = "proseocont";

	public static String hostName = "localhost";
	public static String hostIP = "127.0.0.1";
	public static String port = "8080";
	
	public static MonitorConfiguration config;
	
	public static RestTemplateBuilder rtb;

	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtba;


	/** 
	 * Monitor configuration 
	 */
	@Autowired
	MonitorConfiguration monitorConfig;
    /**
     * Job step util
     */
	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/**
	 * Initialize and run application 
	 * 
	 * @param args command line arguments
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception { 
		SpringApplication spa = new SpringApplication(MonitorApplication.class);
		spa.run(args);
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
		// TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
		config = monitorConfig;
		rtb = rtba;
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
				
				return null;
			});
		} catch (TransactionException e) {
			e.printStackTrace();
		}
		
		Monitor mon = new Monitor();
		mon.run(monitorConfig);
		
	}

	
}