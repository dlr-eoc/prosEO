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

	private String jobName;
	private KubeJob kubeJob;
	
	public KubeJobFinish(KubeJob aJob, String aJobName) {
		super(aJobName);
		kubeJob = aJob;
		jobName = aJobName;
	}

	// @Transactional
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
