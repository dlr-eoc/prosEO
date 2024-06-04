/**
 * MetricsHourly.java
 *
 * © 2024 Prophos Informatik GmbH
 */
package de.dlr.proseo.monitor.apimetrics;

import java.time.Duration;

import javax.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import de.dlr.proseo.monitor.MonitorConfiguration;

/**
 * This class calls the metric functions in hourly cycle
 *  
 * @author Ernst Melchinger
 *
 */
public class MetricsHourly extends MetricsCycleBase {

	/**
	 * Constructor of a Metrics10Minutes instance
	 * 
	 * @param config The monitor configuration
	 * @param txManager The transaction manager
	 * @param em The JPA entity manager
	 */
	public MetricsHourly(MonitorConfiguration config, PlatformTransactionManager txManager, EntityManager em) {
		super(config, txManager, em);
		this.millisToWait = 60 * 60 * 1000; // 10 minutes
	}
	
    /**
     * The hourly metrics calls
     */
    protected void doCycle() {    	
    	metrics.producedBytesAndCount();
    	metrics.sensingToPublication(Duration.ofDays(31));
    	metrics.originToPublication(Duration.ofDays(31));
    	metrics.submissionToCompletedOrder(Duration.ofDays(31));
    }
    
}
