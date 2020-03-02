/**
 * KubeDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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

	private static Logger logger = LoggerFactory.getLogger(KubeDispatcher.class);
	
    private ProductionPlanner productionPlanner;
    private boolean runOnce;
    private KubeConfig kubeConfig;

	public KubeDispatcher(ProductionPlanner p, KubeConfig kc) {
		super((kc != null && p == null) ? "KubeDispatcherRunOnce" : "KubeDispatcher");
		this.setDaemon(true);
		productionPlanner = p;
		kubeConfig = kc;
		runOnce = (kc != null && p == null);
	}
	
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
    			UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig);
    		} else {
    			logger.warn(Messages.KUBEDISPATCHER_CONFIG_NOT_SET.formatWithPrefix());
    		}
    	} else {
    		if (productionPlanner != null) {
    			if (wait <= 0) {
					logger.info(Messages.KUBEDISPATCHER_RUN_ONCE.formatWithPrefix());
					productionPlanner.checkForJobStepsToRun();
    			} else {
    				while (!this.isInterrupted()) {
    					// look for job steps to run
    					logger.info(Messages.KUBEDISPATCHER_CYCLE.formatWithPrefix());
    					productionPlanner.checkForJobStepsToRun();
    					try {
    						sleep(wait);
    					}
    					catch(InterruptedException e) {
    						logger.info(Messages.KUBEDISPATCHER_INTERRUPT.formatWithPrefix());
    						this.interrupt();
    					}
    				}
    			}
    		} else {
    			logger.warn(Messages.KUBEDISPATCHER_PLANNER_NOT_SET.formatWithPrefix());
    		}
    	}
    }   

}
