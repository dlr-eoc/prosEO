/**
 * Metrics10Minutes.java
 *
 * © 2024 Prophos Informatik GmbH
 */
package de.dlr.proseo.monitor.apimetrics;

import java.time.Duration;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import de.dlr.proseo.monitor.MonitorConfiguration;

/**
 * This class calls the metric functions in 10 minute cycle
 *  
 * @author Ernst Melchinger
 *
 */
public class Metrics10Minutes extends MetricsCycleBase {

	/**
	 * Constructor of a Metrics10Minutes instance
	 * 
	 * @param config The monitor configuration
	 * @param txManager The transaction manager
	 * @param em The JPA entity manager
	 */
	public Metrics10Minutes(MonitorConfiguration config, PlatformTransactionManager txManager, EntityManager em) {
		super(config, txManager, em);
		this.millisToWait = 10 * 60 * 1000; // 10 minutes
	}

    /**
     * The 10 minutes metrics calls
     */
    protected void doCycle() {
    	metrics.producedBytesAndCountForType();
    	metrics.downloadSize();
    	metrics.sensingToPublication(Duration.ofDays(1));
    	metrics.originToPublication(Duration.ofDays(1));
    	metrics.submissionToCompletedOrder(Duration.ofDays(1));
    }
    
}
