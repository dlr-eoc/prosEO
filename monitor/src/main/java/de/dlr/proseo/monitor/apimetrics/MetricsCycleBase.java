/**
 * MetricsCycleBase.java
 *
 * © 2024 Prophos Informatik GmbH
 */
package de.dlr.proseo.monitor.apimetrics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.monitor.MonitorConfiguration;

/**
 * This abstract class contains the base functionality to call the metrics function cyclic.
 *  
 * @author Ernst Melchinger
 *
 */
public abstract class MetricsCycleBase extends Thread {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(MetricsCycleBase.class);
	
	/** The Metrics instance used for calculations */
	protected Metrics metrics;
	
	/** The static monitor configuration */
	private MonitorConfiguration config;
	
	/** Transaction manager for transaction control */
	private PlatformTransactionManager txManager;
	
	/** JPA entity manager */
	private EntityManager em;

	/** Milliseconds to wait between the calls */
	protected long millisToWait = 0;
	

	/**
	 * Constructor of a MetricsCycleBase instance
	 * 
	 * @param config The monitor configuration
	 * @param txManager The transaction manager
	 * @param em The JPA entity manager
	 */
	public MetricsCycleBase(MonitorConfiguration config, PlatformTransactionManager txManager, EntityManager em) {
		this.config = config;
		this.txManager = txManager;
		this.em = em;
		metrics = new Metrics(config, txManager, em);
	}

	/**
	 * To be declared in subclasses
	 */
	protected abstract void doCycle();
	
    /**
     * Start the metrics thread
     */
    public void run() {
    	if (logger.isTraceEnabled()) logger.trace(">>> run()");
    	
    	while (!this.isInterrupted()) {
    		// look for job steps to run

    		try {
    			// Transaction to check the delete preconditions			
    			this.doCycle();
    		} catch (NoResultException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (IllegalArgumentException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (TransactionException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (Exception e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		}
    		try {
    			sleep(millisToWait);
    		}
    		catch(InterruptedException e) {
    			this.interrupt();
    		}
    	}
    }   
    
}
