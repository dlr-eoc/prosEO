/**
 * KubeJobFinish.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.ProductionPlannerConfiguration;
import de.dlr.proseo.planner.dispatcher.KubeDispatcher;
import de.dlr.proseo.planner.util.UtilService;

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
	 	 
	@Autowired
	private ProductionPlanner planner;
	
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
    				i++;
    				sleep(wait);
    				found = kubeJob.updateFinishInfoAndDelete(jobName);
    				if (found) {
    		    		// Check once for runnable job steps, which can be started as a result of "kubeJob" being finished 
    					Long jobStepId = kubeJob.getJobId();
    					Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(jobStepId);
    					if (js.isPresent()) {	
    						if (js.get().getOutputProduct() != null) {
    							List<ProductClass> productClasses = 
    									getAllComponentClasses(js.get().getOutputProduct().getProductClass());
    							productClasses.add(js.get().getOutputProduct().getProductClass());
    							for (ProductClass pc : productClasses) {
    								UtilService.getJobStepUtil().checkForJobStepsToRun(kubeJob.getKubeConfig(), 
    										pc.getId(), 
    										false,
    										true);		    				
    							}
    						}			
    					}
    		    		KubeDispatcher kd = new KubeDispatcher(null, kubeJob.getKubeConfig(), true);
    		    		kd.start();    					
    				}
    			}
    			catch(InterruptedException e) {
    			}
    		}
    	}
    }  

	/**
	 * Collect all component classes into a list
	 * 
	 * @param pc Product class
	 * @return The collected component classes
	 */
	@Transactional
	private List<ProductClass> getAllComponentClasses(ProductClass pc) {
		if (logger.isTraceEnabled()) logger.trace(">>> getAllComponentClasses({})", (null == pc ? "null" : pc.getProductType()));
		TransactionTemplate transactionTemplate = new TransactionTemplate(ProductionPlanner.productionPlanner.getTxManager());
		final List<ProductClass> productClasses = new ArrayList<ProductClass>();
		final List<ProductClass> dummy = transactionTemplate.execute((status) -> {
			Optional<ProductClass> pcx = RepositoryService.getProductClassRepository().findById(pc.getId());
			if (pcx.isPresent()) {	
				productClasses.addAll(pcx.get().getComponentClasses());
				for (ProductClass subPC : pcx.get().getComponentClasses()) {
					productClasses.addAll(getAllComponentClasses(subPC));
				}		
			}
			return productClasses;
		});
		return productClasses;
	}
	
}
