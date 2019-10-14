/**
 * OrbitControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.mysema.scalagen.defs;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.rest.model.Orbit;
import de.dlr.proseo.ordermgr.rest.model.OrbitUtil;

/**
 * Spring MVC controller for the prosEO Order Manager; implements the services required to manage spacecraft orbits
 * 
 * @author Ranjitha Vignesh
 *
 */
@Component
public class OrbitControllerImpl implements OrbitController {
		
	/* Message ID constants */
	private static final int MSG_ID_ORBIT_NOT_FOUND = 1005;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 1004;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_ORBIT_MISSING = 1006;


	/* Message string constants */
	private static final String MSG_ORBIT_NOT_FOUND = "No orbit found for ID %d (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Orbit deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-orbitcontroller ";
	private static final String MSG_ORBIT_MISSING = "(E%d) Orbit not set";


	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrbitControllerImpl.class);

	/**
	 * List of all orbits filtered by mission, spacecraft, start time range , orbit number range
	 * 
	 * @param missionCode the mission 
	 * @param spacecraftCode the spacecraft
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @param orbitNumberFrom included orbits beginning
	 * @param orbitNumberTo included orbits end
	 * @return a response entity with either a list of products and HTTP status OK or an error message and an HTTP status indicating failure
	 */
	
//	/**
//	 * Log an informational message with the prosEO message prefix
//	 * 
//	 * @param messageFormat the message text with parameter placeholders in String.format() style
//	 * @param messageId a (unique) message id
//	 * @param messageParameters the message parameters (optional, depending on the message format)
//	 */
//	private void logInfo(String messageFormat, int messageId, Object... messageParameters) {
//		// Prepend message ID to parameter list
//		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
//		messageParamList.add(0, messageId);
//		
//		// Log the error message
//		logger.info(String.format(messageFormat, messageParamList.toArray()));
//	}
	
	/**
	 * Log an error and return the corresponding HTTP message header
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return an HttpHeaders object with a formatted error message
	 */
	private HttpHeaders errorHeaders(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);
		
		// Create an HTTP "Warning" header
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, MSG_PREFIX + message);
		return responseHeaders;
	}
	
	@Override
	public ResponseEntity<List<Orbit>> getOrbits(String spacecraftCode, Long orbitNumberFrom,
			Long orbitNumberTo, Date starttimefrom, Date starttimeto) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrbit{}");

		List<de.dlr.proseo.ordermgr.rest.model.Orbit> result = new ArrayList<>();
		
//		// Simple case: no search criteria set
//			for (de.dlr.proseo.model.Orbit  orbit: RepositoryService.getOrbitRepository().findAll()) {
//				if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
//				Orbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
//				if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
//				result.add(resultOrbit);
//			}
//			
//			return new ResponseEntity<>(result, HttpStatus.OK);		
		
		// Find using search parameters
		//Check if Spacecraft code isn't blank and returns orbits matching to the spacecraft code
		if("" != spacecraftCode) {
			if(0 != orbitNumberFrom && 0 != orbitNumberTo) {
				//Gets all matching Orbits for the matching spacecraft code and Orbit number range
				List <de.dlr.proseo.model.Orbit> matchOrbits = RepositoryService.getOrbitRepository()
						.findBySpacecraftCodeAndOrbitNumberBetween(spacecraftCode, orbitNumberFrom.intValue(), orbitNumberTo.intValue());
			
				//Return all Orbits within given orbit number range and start time range
				//TBA how to pass starttime as paramemter
				if(null != starttimefrom && null != starttimeto) {
					for (de.dlr.proseo.model.Orbit orbit : matchOrbits) {
						if (orbit.getStartTime() == starttimefrom.toInstant() && orbit.getStopTime() == starttimeto .toInstant()) {
							if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
							Orbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
							if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
							result.add(resultOrbit);						
						}
					}
					return new ResponseEntity<>(result, HttpStatus.OK);	
				}
				//Return all Orbits within given orbit number range
				for (de.dlr.proseo.model.Orbit  orbit: matchOrbits) {
					if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
					Orbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
					if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
					result.add(resultOrbit);
				}				
				return new ResponseEntity<>(result, HttpStatus.OK);					
			}
			
			//Returns all orbits matching the spacecraft code within the start time range
			else if (null != starttimefrom && null != starttimeto) {
				for (de.dlr.proseo.model.Orbit orbit : RepositoryService.getOrbitRepository()
						.findBySpacecraftCodeAndStartTimeBetween(spacecraftCode, starttimefrom.toInstant(), starttimeto.toInstant())) {
					if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
					Orbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
					if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
					result.add(resultOrbit);	

				}
				return new ResponseEntity<>(result, HttpStatus.OK);								
				
			}
			else {
				for(de.dlr.proseo.model.Orbit orbit : RepositoryService.getOrbitRepository().findAll()) {
					logger.info("SPacecraft Input value: = "+ spacecraftCode);
					logger.info("orbit spacecraft code: = "+orbit.getSpacecraft().getCode());				
					if(spacecraftCode.equals(orbit.getSpacecraft().getCode())) {
						if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
						Orbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
						if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
						result.add(resultOrbit);				
					}
				}
				
			}
			
			return new ResponseEntity<>(result, HttpStatus.OK);								
		}
		
		//Returns all orbits in the DB when spacecraft code is blank /null 
		//check if this is applicable at ll as spacecracft code is a mandatory parameter
		//working
		else {
			for (de.dlr.proseo.model.Orbit  orbit: RepositoryService.getOrbitRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
				Orbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
				if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
				result.add(resultOrbit);
			}
		}
		return new ResponseEntity<>(result, HttpStatus.OK);								
	}
	
	/**
	 * Create an orbit from the given Json object 
	 * @param orbit the Json object to create the orbit from
	 * @return a response containing a Json object corresponding to the orbit after persistence (with ID and version for all 
	 * 		   contained objects) and HTTP status "CREATED"
	 */
	@Override
	public ResponseEntity<Orbit> createOrbit(@Valid Orbit orbit) {		
		if (logger.isTraceEnabled()) logger.trace(">>> createOrbit({})", orbit.getClass());
		
		if (null == orbit) {
			return new ResponseEntity<>(
					errorHeaders(MSG_ORBIT_MISSING, MSG_ID_ORBIT_MISSING), HttpStatus.BAD_REQUEST);
		}
		
		de.dlr.proseo.model.Orbit modelOrbit = OrbitUtil.toModelOrbit(orbit);
		
		modelOrbit = RepositoryService.getOrbitRepository().save(modelOrbit);
		
		return new ResponseEntity<>(OrbitUtil.toRestOrbit(modelOrbit), HttpStatus.CREATED);
	}

	/**
	 * Find the orbit with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the found orbit and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no orbit with the given ID exists
	 */
	@Override
	public ResponseEntity<Orbit> getOrbitById(Long id) {
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

	/**
	 * Update the orbit with the given ID with the attribute values of the given Json object. 
	 * @param id the ID of the orbit to update
	 * @param orbit a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the obit after modification (with ID and version for all 
	 * 		   contained objects) and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no orbit with the given ID exists
	 */
	@Override
	public ResponseEntity<Orbit> modifyOrbit(Long id, @Valid Orbit orbit) {
		
//		return null;

		if (logger.isTraceEnabled()) logger.trace(">>> modifyOrbit({})", id);
		
		Optional<de.dlr.proseo.model.Orbit> optModelOrbit = RepositoryService.getOrbitRepository().findById(id);
		
		if (optModelOrbit.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_ORBIT_NOT_FOUND, id, MSG_ID_ORBIT_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		de.dlr.proseo.model.Orbit modelOrbit = optModelOrbit.get();
		
		// Update modified attributes
		boolean orbitChanged = false;
		de.dlr.proseo.model.Orbit changedOrbit = OrbitUtil.toModelOrbit(orbit);
		
		if (!modelOrbit.getOrbitNumber().equals(changedOrbit.getOrbitNumber())) {
			orbitChanged = true;
			modelOrbit.setOrbitNumber(changedOrbit.getOrbitNumber());
		}
		
		if (!modelOrbit.getStartTime().equals(changedOrbit.getStartTime())) {
			orbitChanged = true;
			modelOrbit.setStartTime(changedOrbit.getStartTime());
		}
		
		if (!modelOrbit.getStopTime().equals(changedOrbit.getStopTime())) {
			orbitChanged = true;
			modelOrbit.setStopTime(changedOrbit.getStopTime());
		}
		
		if (!modelOrbit.getSpacecraft().equals(changedOrbit.getSpacecraft())) {
			orbitChanged = true;
			modelOrbit.setSpacecraft(changedOrbit.getSpacecraft());
		}
		
		// Save orbit only if anything was actually changed
		if (orbitChanged)	{
			modelOrbit.incrementVersion();
			modelOrbit = RepositoryService.getOrbitRepository().save(modelOrbit);
		}
		
		return new ResponseEntity<>(OrbitUtil.toRestOrbit(modelOrbit), HttpStatus.OK);
	
	
	}

	/**
	 * Delete an orbit by ID
	 * 
	 * @param the ID of the orbit to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, "NOT_FOUND", if the orbit did not
	 *         exist, or "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteOrbitById(Long id) {
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
