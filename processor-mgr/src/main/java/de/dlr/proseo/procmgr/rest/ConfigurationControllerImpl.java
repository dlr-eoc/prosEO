/**
 * ConfigurationControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.procmgr.rest.model.Configuration;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage configuration versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ConfigurationControllerImpl implements ConfigurationController {
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-processor-mgr ";

	private static Logger logger = LoggerFactory.getLogger(ConfigurationControllerImpl.class);

	@Override
	public ResponseEntity<List<Configuration>> getConfigurations(String mission, String processorName,
			String configurationVersion) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<Configuration> createConfiguration(@Valid Configuration configuration) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "POST not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Configuration> getConfigurationById(Long id) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "GET for id %s not implemented (%d)", id, 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Configuration> updateConfiguration(Long id, @Valid Configuration configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteConfigurationById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

}
