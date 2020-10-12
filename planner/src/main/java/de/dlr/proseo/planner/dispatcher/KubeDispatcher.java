/**
 * KubeDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Dispatcher to look for runnable job steps
 * 
 * @author Ernst Melchinger
 *
 */
@Transactional
public class KubeDispatcher extends Thread {

	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(KubeDispatcher.class);
	
	/**
	 * The planner instance 
	 */
	private ProductionPlanner productionPlanner;
	
    /**
     * Flag to decide to run once or forever 
     */
    private boolean runOnce;
    
    /**
     * If true, search only for runnable job steps, else evaluate not satisfied queries too.  
     */
    private boolean onlyRun;
    
    /**
     * The kube config of facility
     */
    private KubeConfig kubeConfig;

	/** 
	 * Create new KubeDispatcher for planner 
	 * @param p The planner
	 * @param kc The kube config of facility
	 * @param onlyRun 
	 */
	public KubeDispatcher(ProductionPlanner p, KubeConfig kc, Boolean onlyRun) {
		super((kc != null && p == null) ? "KubeDispatcherRunOnce" : "KubeDispatcher");
		this.setDaemon(true);
		productionPlanner = p;
		kubeConfig = kc;
		this.onlyRun = onlyRun;
		runOnce = (kc != null && p == null);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Transactional
    public void run() {
    	int wait = 100000;
    	try {
    		wait = Integer.parseInt(ProductionPlanner.config.getProductionPlannerDispatcherWaitTime());
    	} catch (NumberFormatException e) {
    		wait = 100000;
    	}
    	if (runOnce) {
    		if (kubeConfig != null) {
    			UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, null, onlyRun);
    		} else {
    			Messages.KUBEDISPATCHER_CONFIG_NOT_SET.log(logger);
    		}
    	} else {
    		if (productionPlanner != null) {
    			if (wait <= 0) {
					Messages.KUBEDISPATCHER_RUN_ONCE.log(logger);
					productionPlanner.checkForJobStepsToRun();
    			} else {
    				while (!this.isInterrupted()) {
    					// look for job steps to run
    					Messages.KUBEDISPATCHER_CYCLE.log(logger);
    					productionPlanner.checkForJobStepsToRun();
    					try {
    						sleep(wait);
    					}
    					catch(InterruptedException e) {
    						Messages.KUBEDISPATCHER_INTERRUPT.log(logger);
    						this.interrupt();
    					}
    				}
    			}
    		} else {
    			Messages.KUBEDISPATCHER_PLANNER_NOT_SET.log(logger);
    		}
    	}
    }   

}
