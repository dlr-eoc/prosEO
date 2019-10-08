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

import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.rest.model.Mission;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;
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
	
	/* Message string constants */
	private static final String MSG_ORBIT_NOT_FOUND = "No orbit found for ID %d (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Orbit deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-orbitcontroller ";

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
	
	/**
	 * Create an orbit from the given Json object 
	 * @param orbit the Json object to create the orbit from
	 * @return a response containing a Json object corresponding to the orbit after persistence (with ID and version for all 
	 * 		   contained objects) and HTTP status "CREATED"
	 */
	@Override
	public ResponseEntity<List<Orbit>> createOrbit(@Valid List<Orbit> orbit) {
		if (logger.isTraceEnabled()) logger.trace(">>> createOrbit({})", orbit.toString());		
		
		List<de.dlr.proseo.model.Orbit> modelOrbits = new ArrayList<de.dlr.proseo.model.Orbit>();
		List<Orbit> restOrbits = new ArrayList<Orbit>();
		
		//creates orbits in DB
		for(int i=0;i<orbit.size();i++) {
			de.dlr.proseo.model.Orbit modelOrbit = OrbitUtil.toModelOrbit(orbit.get(i));
			modelOrbit = RepositoryService.getOrbitRepository().save(modelOrbit);	
			modelOrbits.add(modelOrbit);
		}
		//fetch created orbits from DB
		for (int i=0; i<modelOrbits.size();i++) {

			Orbit restOrbit = OrbitUtil.toRestOrbit(modelOrbits.get(i));
			restOrbits.add(restOrbit);			
		}		
		return new ResponseEntity<>(restOrbits, HttpStatus.CREATED);	
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
		
		return null;

/*		if (logger.isTraceEnabled()) logger.trace(">>> modifyOrbit({})", id);
		
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
		
		// Save mission only if anything was actually changed
		if (orbitChanged)	{
			modelOrbit.incrementVersion();
			modelOrbit = RepositoryService.getOrbitRepository().save(modelOrbit);
		}
		
		return new ResponseEntity<>(OrbitUtil.toRestOrbit(modelOrbit), HttpStatus.OK);
*/	
	
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
