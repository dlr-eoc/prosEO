/**
 * BackendConnectionService.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service class to connect to the prosEO backend services from the user interface
 * 
 * @author Dr. Thomas Bassler
 */
@Service
public class BackendConnectionService {
	
	/* Message ID constants */
	private static final int MSG_ID_HTTP_CONNECTION_FAILURE = 2800;
	
	/* Message string constants */
	private static final String MSG_HTTP_CONNECTION_FAILURE = "(E%d) HTTP connection failure (cause: %s)";
	private static final String MSG_PREFIX = "199 proseo-ui-cli ";

	/** The configuration object for the prosEO User Interface */
	@Autowired
	private BackendConfiguration backendConfig;
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BackendConnectionService.class);
	
	/**
	 * Test whether the given user can connect to the Processor Manager with the given password
	 * 
	 * @param username the username to login with
	 * @param password the password to login with
	 * @return true, if the connection can be established, false otherwise
	 */
	public boolean testConnectionProcessorManager(String username, String password) {
		if (logger.isTraceEnabled()) logger.trace(">>> testConnectionProcessorManager({}, PWD)", username);

		// Attempt connection to Processor Manager
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = null;
		try {
			RestTemplate restTemplate = rtb.basicAuthentication(username, password).build();
			String connectionUrl = backendConfig.getProcessorManagerUrl() + "/processorclasses";
			if (logger.isTraceEnabled()) logger.trace("... testing connection {}", connectionUrl);
			entity = restTemplate.getForEntity(connectionUrl, List.class);
		} catch (RestClientException e) {
			logger.error(String.format(MSG_PREFIX + MSG_HTTP_CONNECTION_FAILURE, MSG_ID_HTTP_CONNECTION_FAILURE, e.getMessage()), e);
			if (logger.isTraceEnabled()) logger.trace("<<< testConnectionProcessorManager()");
			return false;
		}
		
		// Check connection result
		if (logger.isTraceEnabled()) logger.trace("<<< testConnectionProcessorManager()");
		if (HttpStatus.OK.equals(entity.getStatusCode()) || HttpStatus.NOT_FOUND.equals(entity.getStatusCode())) {
			// It is not necessary that we actually find something
			return true;
		} else {
			return false;
		}
	}
}
