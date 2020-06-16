/**
 * KubeJobFinish.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.kubernetes;

import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.KubeDispatcher;

/**
 * Wait for finished Kubernetes job
 * 
 * @author Ernst Melchinger
 *
 */

public class KubeJobFinish extends Thread {

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

	/* 
	 * Start the tread to look onto Kubernetes job until it been finished and the finish info was retrieved.
	 * This check sleeps a defined time between the cycles and stops also after a maximum number of cycles (parameters are defined in the configuration).
     * @see java.lang.Thread#run()
     */
    public void run() {
    	if (kubeJob != null && jobName != null && !jobName.isEmpty()) {
    		boolean found = false;
    		int i = 0;
    		int wait = 1000;
    		int maxCycles = 50;
    		try {
    			wait = Integer.parseInt(ProductionPlanner.config.getProductionPlannerCycleWaitTime());
    		} catch (NumberFormatException e) {
    			wait = 1000;
    		}
    		try {
    			maxCycles = Integer.parseInt(ProductionPlanner.config.getProductionPlannerMaxCycles());
    		} catch (NumberFormatException e) {
    			maxCycles = 50;
    		}
    		while (!found && i < maxCycles) {
    			try {
    				sleep(wait);
    				found = kubeJob.getFinishInfo(jobName);
    			}
    			catch(InterruptedException e) {
    			}
    		}
    		KubeDispatcher kd = new KubeDispatcher(null, kubeJob.getKubeConfig(), true);
    		kd .start();
    	}
    }    	
}
