/**
 * OrbitControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.rest.model.Mission;
import de.dlr.proseo.ordermgr.rest.model.Orbit;
import de.dlr.proseo.ordermgr.rest.model.OrbitUtil;

/**
 * Spring MVC controller for the prosEO Order Manager; implements the services required to manage spacecraft orbits
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class OrbitControllerImpl implements OrbitController {
		
	/* Message ID constants */
	private static final int MSG_ID_ORBIT_NOT_FOUND = 2001;
	private static final int MSG_ID_ENCLOSING_ORBIT_NOT_FOUND = 2002;
	private static final int MSG_ID_COMPONENT_ORBIT_NOT_FOUND = 2003;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2004;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_ORBIT_NOT_FOUND = "No mission found for ID %d (%d)";
	private static final String MSG_ENCLOSING_ORBIT_NOT_FOUND = "Enclosing mission with ID %d not found (%d)";
	private static final String MSG_COMPONENT_ORBIT_NOT_FOUND = "Component orbit with ID %d not found (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Orbit deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ingestor ";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrbitControllerImpl.class);

	@Override
	public ResponseEntity<List<Orbit>> getOrbits(String missionCode, String spacecraftCode, Long orbitNumberFrom,
			Long orbitNumberTo, Date starttimefrom, Date starttimeto) {
		List<de.dlr.proseo.ordermgr.rest.model.Orbit> result = new ArrayList<>();
		
		// Simple case: no search criteria set
		if (null == missionCode && null == spacecraftCode && null ==orbitNumberFrom && null == orbitNumberTo 
				&& null == starttimefrom && null == starttimeto) {
			for (de.dlr.proseo.model.Orbit  orbit: RepositoryService.getOrbitRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
				Orbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
				if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
				result.add(resultOrbit);
			}
			
		}
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<List<Orbit>> createOrbit(@Valid List<Orbit> orbit) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "POST not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Orbit> getOrbitById(Long id) {
		// TODO Auto-generated method stub

		/*String message = String.format(MSG_PREFIX + "GET for id %s not implemented (%d)", id, 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);*/
		
		if (logger.isTraceEnabled()) logger.trace(">>> getOrbitById({})", id);
		
		Optional<de.dlr.proseo.model.Orbit> modelOrbit = RepositoryService.getOrbitRepository().findById(id);
		
		if (modelOrbit.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_ORBIT_NOT_FOUND, id, MSG_ID_ORBIT_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(OrbitUtil.toRestOrbit(modelOrbit.get()), HttpStatus.OK);
	
	}

	@Override
	public ResponseEntity<Mission> updateOrbit(Long id, @Valid Orbit orbit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteOrbitById(Long id) {
		// TODO Auto-generated method stub
		//return null;
		

		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrbitById({})", id);
		
		// Test whether the orbit id is valid
		Optional<de.dlr.proseo.model.Orbit> modelOrbit = RepositoryService.getOrbitRepository().findById(id);
		if (modelOrbit.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_ORBIT_NOT_FOUND, id, MSG_ID_ORBIT_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
		// Delete the orbit
		RepositoryService.getOrbitRepository().deleteById(id);

		// Test whether the deletion was successful
		modelOrbit = RepositoryService.getOrbitRepository().findById(id);
		if (!modelOrbit.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_DELETION_UNSUCCESSFUL, id, MSG_ID_DELETION_UNSUCCESSFUL);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
	
	}

}
