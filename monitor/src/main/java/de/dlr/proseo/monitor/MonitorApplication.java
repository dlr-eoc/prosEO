/**
 * Ingestor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.monitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.monitor.microservice.MonServiceAggregation;
import de.dlr.proseo.monitor.microservice.MonitorServices;
import de.dlr.proseo.monitor.order.MonitorOrders;
import de.dlr.proseo.monitor.product.MonitorProducts;
import de.dlr.proseo.monitor.apimetrics.Metrics;
import de.dlr.proseo.monitor.apimetrics.Metrics10Minutes;
import de.dlr.proseo.monitor.apimetrics.MetricsHourly;

/*
 * prosEO Planner application
 * 
 * @author Ernst Melchinger
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
@EnableJpaRepositories("de.dlr.proseo.model.dao")
public class MonitorApplication implements CommandLineRunner {

	private static ProseoLogger logger = new ProseoLogger(MonitorApplication.class);

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
	 * MonitorServices configuration
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

	private MonitorServices monServices = null;
	private MonServiceAggregation monServiceAggregation = null;
	private MonitorOrders monOrders = null;
	private MonitorProducts monProducts = null;
	private Metrics10Minutes metrics10 = null;
	private MetricsHourly metricsHourly = null;

	/**
	 * Initialize and run application
	 * 
	 * @param args
	 *            command line arguments
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication spa = new SpringApplication(MonitorApplication.class);
		spa.run(args);
	}

	/**
	 * Start the service monitoring thread
	 */
	public void startMonitorServices() {
		if (logger.isTraceEnabled())
			logger.trace(">>> startMonitorServices()");

		if (monServices == null || !monServices.isAlive()) {
			monServices = new MonitorServices(monitorConfig, txManager);
			monServices.start();
		} else {
			if (monServices.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the service monitoring thread
	 */
	public void stopMonitorServices() {
		if (logger.isTraceEnabled())
			logger.trace(">>> stopMonitorServices()");

		if (monServices != null && monServices.isAlive()) {
			monServices.interrupt();
			int i = 0;
			while (monServices.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("An exception occurred. Cause: ", e);
					}
				}
			}
		}
		monServices = null;
	}

	/**
	 * Start the service monitoring thread
	 */
	public void startMonitorServicesAggregation() {
		if (logger.isTraceEnabled())
			logger.trace(">>> startMonitorServicesAggregation()");

		if (monServiceAggregation == null || !monServiceAggregation.isAlive()) {
			monServiceAggregation = new MonServiceAggregation(monitorConfig, txManager, em);
			monServiceAggregation.start();
		} else {
			if (monServiceAggregation.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the service monitoring thread
	 */
	public void stopMonitorServicesAggregation() {
		if (logger.isTraceEnabled())
			logger.trace(">>> stopMonitorServicesAggregation()");

		if (monServiceAggregation != null && monServiceAggregation.isAlive()) {
			monServiceAggregation.interrupt();
			int i = 0;
			while (monServiceAggregation.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
				}
			}
		}
		monServiceAggregation = null;
	}

	/**
	 * Start the order monitoring thread
	 */
	public void startMonitorOrders() {
		if (logger.isTraceEnabled())
			logger.trace(">>> startMonitorOrders()");

		if (monOrders == null || !monOrders.isAlive()) {
			monOrders = new MonitorOrders(monitorConfig, txManager);
			monOrders.start();
		} else {
			if (monOrders.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the order monitoring thread
	 */
	public void stopMonitorOrders() {
		if (logger.isTraceEnabled())
			logger.trace(">>> stopMonitorOrders()");

		if (monOrders != null && monOrders.isAlive()) {
			monOrders.interrupt();
			int i = 0;
			while (monOrders.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
				}
			}
		}
		monOrders = null;
	}

	/**
	 * Start the product monitoring thread
	 */
	public void startMonitorProducts() {
		if (logger.isTraceEnabled())
			logger.trace(">>> startMonitorOrders()");

		if (monProducts == null || !monProducts.isAlive()) {
			monProducts = new MonitorProducts(monitorConfig, txManager);
			monProducts.start();
		} else {
			if (monProducts.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}

	/**
	 * Stop the product monitoring thread
	 */
	public void stopMonitorProducts() {
		if (logger.isTraceEnabled())
			logger.trace(">>> stopMonitorOrders()");

		if (monProducts != null && monProducts.isAlive()) {
			monProducts.interrupt();
			int i = 0;
			while (monProducts.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
				}
			}
		}
		monOrders = null;
	}

	/**
	 * Start the 10 minutes metrics thread
	 */
	public void startMetrics10() {
		if (logger.isTraceEnabled())
			logger.trace(">>> startMetrics10()");

		if (metrics10 == null || !metrics10.isAlive()) {
			metrics10 = new Metrics10Minutes(monitorConfig, txManager, em);
			metrics10.start();
		} else {
			if (metrics10.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}
	/**
	 * Stop the 10 minutes metrics thread
	 */
	public void stopMetrics10() {
		if (logger.isTraceEnabled())
			logger.trace(">>> stopMetrics10()");

		if (metrics10 != null && metrics10.isAlive()) {
			metrics10.interrupt();
			int i = 0;
			while (metrics10.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
				}
			}
		}
		metrics10 = null;
	}

	/**
	 * Start the hourly metrics thread
	 */
	public void startMetricsHourly() {
		if (logger.isTraceEnabled())
			logger.trace(">>> startMetricsHourly()");

		if (metricsHourly == null || !metricsHourly.isAlive()) {
			metricsHourly = new MetricsHourly(monitorConfig, txManager, em);
			metricsHourly.start();
		} else {
			if (metricsHourly.isInterrupted()) {
				// kubeDispatcher
			}
		}
	}
	/**
	 * Stop the hourly metrics thread
	 */
	public void stopMetricsHourly() {
		if (logger.isTraceEnabled())
			logger.trace(">>> stopMetricsHourly()");

		if (metricsHourly != null && metricsHourly.isAlive()) {
			metricsHourly.interrupt();
			int i = 0;
			while (metricsHourly.isAlive() && i < 100) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
				}
			}
		}
		metrics10 = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
	 */
	@Override
	public void run(String... arg0) throws Exception {
		config = monitorConfig;
		rtb = rtba;

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		try {
			transactionTemplate.execute((status) -> {

				return null;
			});
		} catch (TransactionException e) {
			if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
		}
		// mostly used  for testing and debugging
		if (config.getDoFirstStart()) {
			Metrics m = new Metrics(monitorConfig, txManager, em);
			m.producedBytesAndCountForType();
			m.producedBytesAndCount();
			m.sensingToPublication(Duration.ofDays(config.getFirstStartDuration()));
			m.originToPublication(Duration.ofDays(config.getFirstStartDuration()));
			m.downloadSize();
			m.submissionToCompletedOrder(Duration.ofDays(config.getFirstStartDuration()));
		}
		startMetrics10();
		startMetricsHourly();
		startMonitorServices();
		startMonitorServicesAggregation();
		startMonitorOrders();
		startMonitorProducts();
	}
	
}