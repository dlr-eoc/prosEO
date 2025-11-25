package de.dlr.proseo.monitor.microservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.MonExtService;
import de.dlr.proseo.model.MonService;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.monitor.MonitorApplication;
import de.dlr.proseo.monitor.MonitorConfiguration;

public class MonitorServices extends Thread {

	private static ProseoLogger logger = new ProseoLogger(MonitorServices.class);
	private List<MicroService> services;
	private Map<String, DockerService> dockers;
	private Map<String, MonService> monServices;
	private Map<String, MonExtService> monExtServices;
	private MonitorConfiguration config;
	private PlatformTransactionManager txManager;
	
	/**
	 * @return the txManager
	 */
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	public MonitorServices(MonitorConfiguration config, PlatformTransactionManager txManager) {
		this.config = config;
		this.txManager = txManager;
		this.setName("MonitorServices");
		this.services = new ArrayList<MicroService>();
		this.monServices = new HashMap<String, MonService>();
		this.monExtServices = new HashMap<String, MonExtService>();
		for (MonitorConfiguration.Service s : MonitorApplication.config.getServices()) {
			services.add(new MicroService(s));
		}
		this.dockers = new HashMap<String, DockerService>();
		for (MonitorConfiguration.Docker d : MonitorApplication.config.getDockers()) {
			dockers.put(d.getName(), new DockerService(d));
		}
	}
	
	public void checkServices() {
		if (services != null) {
			for (MicroService s : services) {
				s.check(this);
			}
		}
	}

	public DockerService getDockerService(String name) {
		return dockers.get(name);
	}
	
	public DockerService getKubernetes(String name) {
		return null;
	}

	public MonService getMonService(String nameId, String name) {
		MonService ms = monServices.get(nameId);
		if (ms == null) {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			transactionTemplate.setReadOnly(false);
			for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
				try {
					ms = transactionTemplate.execute((status) -> {	
						MonService msX = RepositoryService.getMonServiceRepository().findByNameId(nameId);
						if (msX == null) {
							msX = new MonService();
							msX.setName(name);
							msX.setNameId(nameId);
							msX = RepositoryService.getMonServiceRepository().save(msX);
						}
						return msX;
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
			monServices.put(nameId, ms);
		}
		return ms;
	}
	
	public MonExtService getMonExtService(String nameId, String name) {
		MonExtService ms = monExtServices.get(nameId);
		if (ms == null) {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			transactionTemplate.setReadOnly(false);
			for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
				try {
					ms = transactionTemplate.execute((status) -> {	
						MonExtService msX = RepositoryService.getMonExtServiceRepository().findByNameId(nameId);
						if (msX == null) {
							msX = new MonExtService();
							msX.setName(name);
							msX.setNameId(nameId);
							msX = RepositoryService.getMonExtServiceRepository().save(msX);
						}
						return msX;
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
			monExtServices.put(nameId, ms);
		}
		return ms;
	}
	
    public void run() {
    	Long wait = (long) 100000;
    	try {
    		if (config.getServiceCycle() != null) {
    			wait = config.getServiceCycle();
    		} else {
    			wait = config.getCycle();
    		}
    	} catch (NumberFormatException e) {
    		wait = (long) 100000;
    	}
    	while (!this.isInterrupted()) {
    		// look for job steps to run
    		this.checkServices();
    		try {
    			sleep(wait);
    		}
    		catch(InterruptedException e) {
    			this.interrupt();
    		}
    	}
    }   
}
