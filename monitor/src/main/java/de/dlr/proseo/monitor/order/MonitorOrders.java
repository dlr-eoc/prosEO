package de.dlr.proseo.monitor.order;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.MonOrderProgress;
import de.dlr.proseo.model.MonOrderState;
import de.dlr.proseo.model.MonService;
import de.dlr.proseo.model.MonServiceState;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.monitor.MonitorConfiguration;
import de.dlr.proseo.monitor.microservice.MicroService;

@Transactional
public class MonitorOrders extends Thread {
	private static Logger logger = LoggerFactory.getLogger(MonitorOrders.class);	

	private Map<String, MonOrderState> monOrderStates;
	private MonitorConfiguration config;

	public MonitorOrders(MonitorConfiguration config) {
		this.config = config;
		this.monOrderStates = new HashMap<String, MonOrderState>();
		
		for (MonOrderState mos : RepositoryService.getMonOrderStateRepository().findAll()) {
			monOrderStates.put(mos.getName(), mos);
		}
	}
	
	@Transactional
	public void checkOrders() {
		Instant now = Instant.now();
		List<ProcessingOrder> processingOrders = RepositoryService.getOrderRepository().findAll();
		if (processingOrders != null) {
			for (ProcessingOrder po : processingOrders) {
				MonOrderProgress mop = new MonOrderProgress();
				mop.setProcessingOrder(po);				
				mop.setMonOrderState(monOrderStates.get(po.getOrderState().toString()));
				switch (po.getOrderState()) {
				case INITIAL: 
				case APPROVED: 
					mop.setAllJobSteps(0);
					mop.setFailedJobSteps(0);
					mop.setCompletedJobSteps(0);
					mop.setRunningJobSteps(0);
					mop.setFinishedJobSteps(0);
					break;
				default:
					int jsCount = RepositoryService.getJobStepRepository().countJobStepByOrderId(po.getId());
					int jsCountCompleted = RepositoryService.getJobStepRepository().countJobStepCompletedByOrderId(po.getId());
					int jsCountFailed = RepositoryService.getJobStepRepository().countJobStepFailedByOrderId(po.getId());
					int jsCountRunning = RepositoryService.getJobStepRepository().countJobStepRunningByOrderId(po.getId());
					mop.setAllJobSteps(jsCount);
					mop.setFailedJobSteps(jsCountFailed);
					mop.setCompletedJobSteps(jsCountCompleted);
					mop.setRunningJobSteps(jsCountRunning);
					mop.setFinishedJobSteps(jsCountFailed + jsCountCompleted);
					break;
				}
				mop.setDatetime(now);
				RepositoryService.getMonOrderProgressRepository().save(mop);
			}
		}
	}
	
    public void run() {
    	Long wait = (long) 100000;
    	try {
    		if (config.getOrderCycle() != null) {
    			wait = config.getOrderCycle();
    		} else {
    			wait = config.getCycle();
    		}
    	} catch (NumberFormatException e) {
    		wait = (long) 100000;
    	}
    	while (!this.isInterrupted()) {
    		// look for job steps to run
    		this.checkOrders();
    		try {
    			sleep(wait);
    		}
    		catch(InterruptedException e) {
    			this.interrupt();
    		}
    	}
    }   
}
