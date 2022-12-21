/**
 * PlannerSemaphoreClient.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

import java.time.Duration;

import javax.ws.rs.ProcessingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.ingestor.rest.ProductIngestor;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.IngestorMessage;

/**
 * Interface class to request and release semaphores from the Production Planner
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class PlannerSemaphoreClient {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(PlannerSemaphoreClient.class);
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
	/** Product ingestor */
	@Autowired
	ProductIngestor productIngestor;
			
	/**
	 * Ask production planner for a slot to manipulate product(s)
	 * 
	 * @param user The user
	 * @param password The password
	 * @return true after semaphore was available 
	 */
	public Boolean acquireSemaphore(String user, String password) {
		if (logger.isTraceEnabled()) logger.trace(">>> acquireSemaphore({}, PWD)", user);
		
		// Skip if production planner is not configured
		if (ingestorConfig.getProductionPlannerUrl().isBlank()) {
			return true;
		}
		
		String url = ingestorConfig.getProductionPlannerUrl() + "/semaphore/acquire";
		RestTemplate restTemplate = rtb
				.setConnectTimeout(Duration.ofMillis(ingestorConfig.getProductionPlannerTimeout()))
				.basicAuthentication(user, password)
				.build();
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		if (!HttpStatus.OK.equals(response.getStatusCode())) {
			throw new ProcessingException(
					logger.log(IngestorMessage.ERROR_ACQUIRE_SEMAPHORE, response.getStatusCode().toString()));
		}
		return true;
	}

	/**
	 * Release semaphore of production planner
	 * 
	 * @param user The user
	 * @param password The password
	 * @return true after semaphore was released 
	 */
	public Boolean releaseSemaphore(String user, String password) {
		if (logger.isTraceEnabled()) logger.trace(">>> releaseSemaphore({}, PWD)", user);
		
		// Skip if production planner is not configured
		if (ingestorConfig.getProductionPlannerUrl().isBlank()) {
			return true;
		}
		
		String url = ingestorConfig.getProductionPlannerUrl() + "/semaphore/release";
		RestTemplate restTemplate = rtb
				.setConnectTimeout(Duration.ofMillis(ingestorConfig.getProductionPlannerTimeout()))
				.basicAuthentication(user, password)
				.build();
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		if (!HttpStatus.OK.equals(response.getStatusCode())) {
			throw new ProcessingException(
					logger.log(IngestorMessage.ERROR_RELEASE_SEMAPHORE, response.getStatusCode().toString()));
		}
		return true;
	}

}
