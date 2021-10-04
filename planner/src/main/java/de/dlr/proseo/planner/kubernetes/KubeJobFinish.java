/**
 * KubeJobFinish.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.KubeDispatcher;

/**
 * Wait for finished Kubernetes job
 * 
 * @author Ernst Melchinger
 *
 */

@Transactional
public class KubeJobFinish extends Thread {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(KubeJobFinish.class);
	
	/**
	 * The Kubernetes job name which is regarded
	 */
	private String jobName;
	
	/**
	 * The planner kube job
	 */
	private KubeJob kubeJob;
	
	/**
	 * Create a new thread instance to regard the Kubernetes job until end of life
	 *  
	 * @param aJob The planner kube job
	 * @param aJobName The Kubernetes job name
	 */
	public KubeJobFinish(KubeJob aJob, String aJobName) {
		super(aJobName);
		kubeJob = aJob;
		jobName = aJobName;
	}

	/**
	 * Start the tread to look onto Kubernetes job until it been finished and the finish info was retrieved.
	 * This check sleeps a defined time between the cycles and stops also after a maximum number of cycles
	 * (parameters are defined in the configuration).
	 * 
     * @see java.lang.Thread#run()
     */
	@Transactional
    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run()");
		
    	if (kubeJob != null && jobName != null && !jobName.isEmpty()) {
    		boolean found = false;
    		int i = 0;
    		int wait = ProductionPlanner.config.getProductionPlannerCycleWaitTime();
    		int maxCycles = ProductionPlanner.config.getProductionPlannerMaxCycles();

    		while (!found && i < maxCycles) {
    			try {
    				sleep(wait);
    				found = kubeJob.updateFinishInfoAndDelete(jobName);
    			}
    			catch(InterruptedException e) {
    			}
    		}
    		// Check once for runnable job steps, which can be started as a result of "kubeJob" being finished 
    		KubeDispatcher kd = new KubeDispatcher(null, kubeJob.getKubeConfig(), true);
    		kd.start();
    	}
    }    	
}
