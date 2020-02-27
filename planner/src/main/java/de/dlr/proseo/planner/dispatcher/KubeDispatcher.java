package de.dlr.proseo.planner.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.util.JobStepUtil;

@Component
@Transactional
public class KubeDispatcher extends Thread {

	private static Logger logger = LoggerFactory.getLogger(KubeDispatcher.class);
	
    private ProductionPlanner productionPlanner;
    
    private JobStepUtil jobStepUtil;


	public KubeDispatcher(ProductionPlanner p, JobStepUtil jsu) {
		super("KubeDispatcher");
		productionPlanner = p;
		jobStepUtil = jsu;
	}
	
	@Transactional
    public void run() {
    	int wait = 100000;
    	try {
    		wait = Integer.parseInt(ProductionPlanner.config.getProductionPlannerDispatcherWaitTime());
    	} catch (NumberFormatException e) {
    		wait = 100000;
    	}
    	while (!this.isInterrupted()) {
			// look for job steps to run
			logger.info("KubeDispatcher cycle");
			productionPlanner.checkForJobStepsToRun();
    		try {
    			sleep(wait);
    		}
    		catch(InterruptedException e) {
    			logger.info("KubeDispatcher interrupt");
    			this.interrupt();
    		}

    	}
    }   

}
