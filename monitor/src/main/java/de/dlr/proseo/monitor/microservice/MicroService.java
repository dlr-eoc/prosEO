package de.dlr.proseo.monitor.microservice;

import java.util.Optional;
import java.time.Instant;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.interfaces.rest.model.RestHealth;
import de.dlr.proseo.model.MonExtServiceStateOperation;
import de.dlr.proseo.model.MonService;
import de.dlr.proseo.model.MonServiceState;
import de.dlr.proseo.model.MonServiceStateOperation;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.monitor.MonitorApplication;
import de.dlr.proseo.monitor.MonitorConfiguration;
import de.dlr.proseo.model.util.MonServiceStates;

/**
 * Represent a microservice to check status
 * The microservice has to be a spring boot application 
 * 
 * @author Melchinger
 *
 */

@Transactional
public class MicroService {

	private static Logger logger = LoggerFactory.getLogger(MicroService.class);
	
	/**
	 * The service name
	 */
	private String name;
	/**
	 * The machine url service is running on
	 */
	private String url;
	/**
	 * The docker name
	 */
	private String docker;
	/**
	 * The kubernetes name
	 */
	private String kubernetes;
	/**
	 * Service is a prosEO service 
	 */
	private Boolean isProseo;
	/**
	 * Service is a spring boot application wiht actuator
	 */
	private Boolean hasActuator;
	/**
	 * State of service (MonServiceStates)
	 */
	private Long state = (long) 2;
	
	/**
	 * @return the hasActuator
	 */
	public Boolean getHasActuator() {
		return hasActuator;
	}

	/**
	 * @return the isProseo
	 */
	public Boolean getIsProseo() {
		return isProseo;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the docker
	 */
	public String getDocker() {
		return docker;
	}

	/**
	 * @return the kubernetes
	 */
	public String getKubernetes() {
		return kubernetes;
	}

	/**
	 * @return the state
	 */
	public Long getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(Long state) {
		this.state = state;
	}

	public MicroService(MonitorConfiguration.Service service) {
		this.name = service.getName();
		this.url = service.getUrl();
		this.docker = service.getDocker();
		this.isProseo = service.getIsProseo();
		this.hasActuator = service.getHasActuator();
		this.state = MonServiceStates.STOPPED_ID;
	}
	
	public void check(Monitor monitor) {

		String serviceUrl = this.getUrl();

		MonService ms = monitor.getMonService(this.getName()); 

		if (this.getHasActuator()) {
			RestTemplate restTemplate = MonitorApplication.rtb
					.setReadTimeout(Duration.ofMillis(MonitorApplication.config.getReadTimeout()))
					.setConnectTimeout(Duration.ofMillis(MonitorApplication.config.getConnectTimeout()))
					.build();
			try {
				ResponseEntity<RestHealth> response = restTemplate.getForEntity(serviceUrl, RestHealth.class);
				if (HttpStatus.OK.equals(response.getStatusCode())) {
					// check whether service is running in docker
					if (response.getBody().getStatus().equalsIgnoreCase("UP")) {
						this.state = MonServiceStates.RUNNING_ID;
						createEntry(monitor);
						return;
					}
				}
			} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
				logger.error(e.getMessage());
				// TODO
			} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
				logger.error(e.getMessage());
				// TODO
			} catch (RestClientException e) {
				logger.error(e.getMessage());
				// TODO
			} catch (Exception e) {
				logger.error(e.getMessage());
				// TODO
			}
		} 
		// service does not answer or answer unknown state, check further if docker or kubernetes are set, else set to stopped
		if (monitor.getDockerService(this.getDocker()) == null && monitor.getKubernetes(this.getKubernetes()) == null) {
			this.state = MonServiceStates.STOPPED_ID;
			createEntry(monitor);
			return;
		}
		DockerService ds = monitor.getDockerService(this.getDocker());
		if (ds != null) {
			ds.check(this, monitor);
			return;
		} else if (monitor.getKubernetes(this.getKubernetes()) != null) {
			// TODO check for Kubernetes
			return;
		}
		// TODO Is it necessary to inspect process list of a machine?
		this.state = MonServiceStates.STOPPED_ID;
		createEntry(monitor);
		return;
		
	}
	
	@Transactional
	public void createEntry(Monitor monitor) {
		if (getIsProseo()) {
			MonServiceStateOperation ms = new MonServiceStateOperation();
			ms.setMonService(monitor.getMonService(getName()));
			Optional<MonServiceState> aState = RepositoryService.getMonServiceStateRepository().findById(getState());
			ms.setMonServiceState(aState.get());
			ms.setDatetime(Instant.now());
			ms = RepositoryService.getMonServiceStateOperationRepository().save(ms);
		} else {
			MonExtServiceStateOperation ms = new MonExtServiceStateOperation();
			ms.setMonExtService(monitor.getMonExtService(getName()));
			Optional<MonServiceState> aState = RepositoryService.getMonServiceStateRepository().findById(getState());
			ms.setMonServiceState(aState.get());
			ms.setDatetime(Instant.now());
			ms = RepositoryService.getMonExtServiceStateOperationRepository().save(ms);
		}
	}
}
