package de.dlr.proseo.monitor.microservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.MonExtService;
import de.dlr.proseo.model.MonService;
import de.dlr.proseo.model.MonServiceState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.monitor.MonitorApplication;
import de.dlr.proseo.monitor.MonitorConfiguration;

@Transactional
public class Monitor extends Thread {

	private static Logger logger = LoggerFactory.getLogger(Monitor.class);
	private List<MicroService> services;
	private Map<String, DockerService> dockers;
	private Map<String, MonService> monServices;
	private Map<String, MonExtService> monExtServices;
	private Map<String, MonServiceState> monServiceStates;
	
	public Monitor() {
		this.services = new ArrayList<MicroService>();
		this.monServices = new HashMap<String, MonService>();
		this.monExtServices = new HashMap<String, MonExtService>();
		this.monServiceStates = new HashMap<String, MonServiceState>();
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

	@Transactional
	public MonService getMonService(String nameId, String name) {
		MonService ms = monServices.get(nameId);
		if (ms == null) {
			ms = RepositoryService.getMonServiceRepository().findByNameId(nameId);
			if (ms == null) {
				ms = new MonService();
				ms.setName(name);
				ms.setNameId(nameId);
				ms = RepositoryService.getMonServiceRepository().save(ms);
			}
			monServices.put(nameId, ms);
		}
		return ms;
	}
	
	@Transactional
	public MonExtService getMonExtService(String nameId, String name) {
		MonExtService ms = monExtServices.get(nameId);
		if (ms == null) {
			ms = RepositoryService.getMonExtServiceRepository().findByNameId(nameId);
			if (ms == null) {
				ms = new MonExtService();
				ms.setName(name);
				ms.setNameId(nameId);
				ms = RepositoryService.getMonExtServiceRepository().save(ms);
			}
			monExtServices.put(nameId, ms);
		}
		return ms;
	}
	
    public void run(MonitorConfiguration config) {
    	Long wait = (long) 100000;
    	try {
    		wait = config.getCycle();
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
