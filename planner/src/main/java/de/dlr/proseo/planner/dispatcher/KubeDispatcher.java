/**
 * KubeDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Dispatcher to look for runnable job steps
 * 
 * @author Ernst Melchinger
 *
 */
// @Transactional
public class KubeDispatcher extends Thread {

	/**
	 * Logger for this class
	 */
	private static ProseoLogger logger = new ProseoLogger(KubeDispatcher.class);
	
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
    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run()");
		
    	int wait = ProductionPlanner.config.getProductionPlannerDispatcherWaitTime();

    	if (runOnce) {
			logger.log(PlannerMessage.KUBEDISPATCHER_RUN_ONCE);
    		if (kubeConfig != null) {
				try {
					kubeConfig.getProductionPlanner().acquireThreadSemaphore("run");
					UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, 0, onlyRun, true);
					kubeConfig.getProductionPlanner().releaseThreadSemaphore("run");		
				} catch (Exception e) {
					kubeConfig.getProductionPlanner().releaseThreadSemaphore("run");		
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
				}
    		} else {
    			logger.log(PlannerMessage.KUBEDISPATCHER_CONFIG_NOT_SET);
    		}
    	} else {
    		if (productionPlanner != null) {
    			if (wait <= 0) {
					logger.log(PlannerMessage.KUBEDISPATCHER_RUN_ONCE);
					try {
						productionPlanner.acquireThreadSemaphore("run");
						UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, 0, onlyRun, true);
						productionPlanner.releaseThreadSemaphore("run");		
					} catch (Exception e) {
						productionPlanner.releaseThreadSemaphore("run");		
						logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
					}
    			} else {
    				while (!this.isInterrupted()) {
    					// look for job steps to run
    					logger.log(PlannerMessage.KUBEDISPATCHER_CYCLE);
    					if (productionPlanner.getReleaseThreads().size() == 0) {
    						try {
    							productionPlanner.acquireThreadSemaphore("run");
    							UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, 0, onlyRun, true);
    							productionPlanner.releaseThreadSemaphore("run");		
    						} catch (Exception e) {
    							productionPlanner.releaseThreadSemaphore("run");		
    							logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
    						}
    					}
    					try {
        					logger.log(PlannerMessage.KUBEDISPATCHER_SLEEP, wait);
    						sleep(wait);
    					}
    					catch(InterruptedException e) {
    						logger.log(PlannerMessage.KUBEDISPATCHER_INTERRUPT);
    						this.interrupt();
    					}
    				}
    			}
    		} else {
    			logger.log(PlannerMessage.KUBEDISPATCHER_PLANNER_NOT_SET);
    		}
    	}
    }   

}
