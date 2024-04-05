package de.dlr.proseo.monitor.order;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.MonOrderProgress;
import de.dlr.proseo.model.MonOrderState;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.monitor.MonitorConfiguration;

/**
 * The thread monitoring the orders
 * 
 * @author Melchinger
 *
 */
public class MonitorOrders extends Thread {
	private static ProseoLogger logger = new ProseoLogger(MonitorOrders.class);	

	/** Transaction manager for transaction control */

	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	/**
	 * Hold the order state cache 
	 */
	private Map<String, MonOrderState> monOrderStates;
	
	
	/**
	 * The monitor configuration (application.yml) 
	 */
	private MonitorConfiguration config;

	/**
	 * Instantiate the monitor order thread
	 * 
	 * @param config The monitor configuration
	 * @param txManager The transaction manager
	 */
	public MonitorOrders(MonitorConfiguration config, PlatformTransactionManager txManager) {
		this.config = config;
		this.txManager = txManager;
		this.setName("MonitorOrders");
		this.monOrderStates = new HashMap<String, MonOrderState>();
		
		for (MonOrderState mos : RepositoryService.getMonOrderStateRepository().findAll()) {
			monOrderStates.put(mos.getName(), mos);
		}
	}
		
	/**
	 * Collect the monitoring information of orders
	 */
	public void checkOrders() {
    	if (logger.isTraceEnabled()) logger.trace(">>> checkOrders()");

		Instant now = Instant.now();
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		List<ProcessingOrder> processingOrders = transactionTemplate.execute((status) -> {			
			return RepositoryService.getOrderRepository().findAll();
		});
		if (processingOrders != null) {
			for (ProcessingOrder po : processingOrders) {
				MonOrderProgress mop = new MonOrderProgress();
				// mop.setProcessingOrder(po);				
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
					transactionTemplate.execute((status) -> {	
						int jsCount = RepositoryService.getJobStepRepository().countJobStepByOrderId(po.getId());
						int jsCountCompleted = RepositoryService.getJobStepRepository().countJobStepCompletedByOrderId(po.getId());
						int jsCountFailed = RepositoryService.getJobStepRepository().countJobStepFailedByOrderId(po.getId());
						int jsCountRunning = RepositoryService.getJobStepRepository().countJobStepRunningByOrderId(po.getId());
						mop.setAllJobSteps(jsCount);
						mop.setFailedJobSteps(jsCountFailed);
						mop.setCompletedJobSteps(jsCountCompleted);
						mop.setRunningJobSteps(jsCountRunning);
						mop.setFinishedJobSteps(jsCountFailed + jsCountCompleted);
						return null;
					});
					break;
				}
				mop.setDatetime(now);
				transactionTemplate.setReadOnly(false);
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {	
							Optional<ProcessingOrder> poxOpt = RepositoryService.getOrderRepository().findById(po.getId());
							ProcessingOrder pox = null;
							if (poxOpt.isPresent()) {
								pox = poxOpt.get();
							} else {
								return null;
							}
							pox.getMonOrderProgress().add(mop);
							RepositoryService.getOrderRepository().save(pox);
							return null;
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}
			}
		}
	}
	
    /**
     * Start the monitor thread
     */
    public void run() {
    	if (logger.isTraceEnabled()) logger.trace(">>> run()");
    	
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

    		try {
    			// Transaction to check the delete preconditions			
    			this.checkOrders();
    		} catch (NoResultException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (IllegalArgumentException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (TransactionException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (Exception e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		}
    		try {
    			sleep(wait);
    		}
    		catch(InterruptedException e) {
    			this.interrupt();
    		}
    	}
    }   
}
