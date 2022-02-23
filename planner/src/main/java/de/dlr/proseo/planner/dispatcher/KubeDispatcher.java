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
	 * 
	 * @param p The planner
	 * @param kc The kube config of facility
	 * @param onlyRun set to true to evaluate only runnable job steps or to false to check all job steps
	 */
	public KubeDispatcher(ProductionPlanner p, KubeConfig kc, Boolean onlyRun) {
		super((kc != null && p == null) ? "KubeDispatcherRunOnce" : "KubeDispatcher");
		this.setDaemon(true);
		productionPlanner = p;
		kubeConfig = kc;
		this.onlyRun = onlyRun;
		runOnce = (kc != null && p == null);
	}
	
	/**
	 * Checks for job steps, which are ready to run; depending on its creation parameter "runOnce" this is
	 * a one-time process or it is running cyclically until it is terminated externally
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Transactional
    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run()");
		
    	int wait = ProductionPlanner.config.getProductionPlannerDispatcherWaitTime();

    	if (runOnce) {
			Messages.KUBEDISPATCHER_RUN_ONCE.log(logger);
    		if (kubeConfig != null) {
    			UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, null, onlyRun);
    		} else {
    			Messages.KUBEDISPATCHER_CONFIG_NOT_SET.log(logger);
    		}
    	} else {
    		if (productionPlanner != null) {
    			if (wait <= 0) {
					Messages.KUBEDISPATCHER_RUN_ONCE.log(logger);
					try {
						UtilService.getJobStepUtil().checkForJobStepsToRun();
					} catch (Exception e) {
						logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
					}
    			} else {
    				while (!this.isInterrupted()) {
    					// look for job steps to run
    					Messages.KUBEDISPATCHER_CYCLE.log(logger);
    					try {
    						UtilService.getJobStepUtil().checkForJobStepsToRun();
						} catch (Exception e) {
							logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
						}
    					try {
        					Messages.KUBEDISPATCHER_SLEEP.log(logger, wait);
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
