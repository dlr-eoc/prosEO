/**
 * OrbitControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.NoResultException;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.rest.OrbitController;
import de.dlr.proseo.model.rest.model.RestOrbit;
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
	// private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_ORBIT_MISSING = 1006;
	private static final int MSG_ID_ORBIT_INCOMPLETE = 1007;


	/* Message string constants */
	private static final String MSG_ORBIT_NOT_FOUND = "No orbit found for ID %d (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Orbit deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-orbitcontroller ";
	private static final String MSG_ORBIT_MISSING = "(E%d) Orbit not set";
	private static final String MSG_ORBIT_INCOMPLETE = "(E%d) Spacecraft Code not set in the search";


	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

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
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, MSG_PREFIX + message.replaceAll("\n", " "));
		return responseHeaders;
	}

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
		return errorHeaders(message);
	}
	
	/**
	 * List of all orbits filtered by spacecraft code, orbit number range, starttime range
	 * 
	 * @param spacecraftCode 
	 * @param orbitNumberFrom
	 * @param orbitNumber To
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @return HTTP status "OK" and a list of products or
	 *         HTTP status "NOT_FOUND" and an error message, if no products matching the search criteria were found
	 */
	
	@Override
	public ResponseEntity<List<RestOrbit>> getOrbits(String spacecraftCode, Long orbitNumberFrom,
			Long orbitNumberTo, @DateTimeFormat Date starttimefrom, @DateTimeFormat Date starttimeto) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrbit{}");
		
		/* Check arguments */
		if (null == spacecraftCode || "".equals(spacecraftCode)) {
			return new ResponseEntity<>(
					errorHeaders(MSG_ORBIT_INCOMPLETE, MSG_ID_ORBIT_INCOMPLETE), HttpStatus.BAD_REQUEST);
		}

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		List<RestOrbit> resultList = null;
		try {
			resultList = transactionTemplate.execute((status) -> {
				List<RestOrbit> result = new ArrayList<>();		
				// Find using search parameters
				// Returns orbits matching to the spacecraft code
				if(null != orbitNumberFrom && 0 != orbitNumberFrom.intValue() && null != orbitNumberTo && 0 != orbitNumberTo.intValue()) {
					//Gets all matching Orbits for the matching spacecraft code and Orbit number range
					List <Orbit> matchOrbits = RepositoryService.getOrbitRepository()
							.findBySpacecraftCodeAndOrbitNumberBetween(spacecraftCode, orbitNumberFrom.intValue(), orbitNumberTo.intValue());
				
					//Return all Orbits within given orbit number range and start time range
					if(null != starttimefrom && null != starttimeto) {
						for (de.dlr.proseo.model.Orbit orbit : matchOrbits) {
							logger.info("Orbit.starttime: "+orbit.getStartTime());
							logger.info("Orbit.stoptime: "+orbit.getStopTime());

							if (!(orbit.getStartTime().isBefore(starttimefrom.toInstant())) && 
									!(orbit.getStopTime().isAfter(starttimeto .toInstant()))) {
								if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
								RestOrbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
								if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
								result.add(resultOrbit);						
							}
						}
					}
					//Return all Orbits within given orbit number range
					for (de.dlr.proseo.model.Orbit  orbit: matchOrbits) {
						if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
						RestOrbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
						if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
						result.add(resultOrbit);
					}				
				}
				
				//Returns all orbits matching the spacecraft code within the start time range
				else if (null != starttimefrom && null != starttimeto) {
					for (Orbit orbit : RepositoryService.getOrbitRepository()
							.findBySpacecraftCodeAndStartTimeBetween(spacecraftCode, starttimefrom.toInstant(), starttimeto.toInstant())) {
						if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
						RestOrbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
						if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
						result.add(resultOrbit);	

					}
				}
				else {
					for(Orbit orbit : RepositoryService.getOrbitRepository().findAll()) {
						logger.info("SPacecraft Input value: = "+ spacecraftCode);
						logger.info("orbit spacecraft code: = "+orbit.getSpacecraft().getCode());				
						if(spacecraftCode.equals(orbit.getSpacecraft().getCode())) {
							if (logger.isDebugEnabled()) logger.debug("Found orbit with ID {}", orbit.getId());
							RestOrbit resultOrbit = OrbitUtil.toRestOrbit(orbit);
							if (logger.isDebugEnabled()) logger.debug("Created result orbit with ID {}", resultOrbit.getId());
							result.add(resultOrbit);				
						}
					}
					
				}
				return result;
			});
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<>(resultList, HttpStatus.OK);								
	}
	
	/**
	 * Create orbit/s from the given Json object 
	 * @param orbit the List of Json object to create the orbit from
	 * @return a response containing a List of Json object corresponding to the orbit after persistence (with ID and version for all 
	 * 		   contained objects) and HTTP status "CREATED"
	 */
	@Override
	public ResponseEntity<List<RestOrbit>> createOrbits(@Valid List<RestOrbit> orbit) {		
		if (logger.isTraceEnabled()) logger.trace(">>> createOrbit({})", orbit.getClass());
		
		/* Check argument */
		if (null == orbit || orbit.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_ORBIT_MISSING, MSG_ID_ORBIT_MISSING), HttpStatus.BAD_REQUEST);
		}
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		List<RestOrbit> restOrbitList = null;
		try {
			restOrbitList = transactionTemplate.execute((status) -> {
				List<Orbit> modelOrbits = new ArrayList<de.dlr.proseo.model.Orbit> ();
				
				List<RestOrbit> restOrbits = new ArrayList<RestOrbit> ();
				//Insert every valid Rest orbit into the DB
				for(RestOrbit tomodelOrbit : orbit) {
					Orbit modelOrbit = OrbitUtil.toModelOrbit(tomodelOrbit);
					
					//Adding spacecraft object to modelOrbit
					Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(tomodelOrbit.getSpacecraftCode());
					modelOrbit.setSpacecraft(spacecraft);
					
					modelOrbit = RepositoryService.getOrbitRepository().save(modelOrbit);
					modelOrbits.add(modelOrbit);
				}
				 
				//Return every inserted orbit 
				for(Orbit torestOrbit : modelOrbits ) {
					RestOrbit restOrbit = OrbitUtil.toRestOrbit(torestOrbit);
					restOrbits.add(restOrbit);
				}
				
				return restOrbits;
			});
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<>(restOrbitList, HttpStatus.CREATED);
	}

	/**
	 * Find the orbit with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the found orbit and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no orbit with the given ID exists
	 */
	@Override
	public ResponseEntity<RestOrbit> getOrbitById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrbitById({})", id);
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		RestOrbit restOrbit = null;
		try {
			restOrbit = transactionTemplate.execute((status) -> {
				Optional<Orbit> modelOrbit = RepositoryService.getOrbitRepository().findById(id);
				
				if (modelOrbit.isEmpty()) {
					throw new NoResultException(String.format(MSG_ORBIT_NOT_FOUND, id, MSG_ID_ORBIT_NOT_FOUND));
				}
				
				return OrbitUtil.toRestOrbit(modelOrbit.get());
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<>(restOrbit, HttpStatus.OK);
	
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
	public ResponseEntity<RestOrbit> modifyOrbit(Long id, @Valid RestOrbit orbit) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyOrbit({})", id);
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		RestOrbit restOrbit = null;
		try {
			restOrbit = transactionTemplate.execute((status) -> {
				Optional<Orbit> optModelOrbit = RepositoryService.getOrbitRepository().findById(id);
				
				if (optModelOrbit.isEmpty()) {
					throw new NoResultException(String.format(MSG_ORBIT_NOT_FOUND, id, MSG_ID_ORBIT_NOT_FOUND));
				}
				Orbit modelOrbit = optModelOrbit.get();
				
				// Update modified attributes
				boolean orbitChanged = false;
				Orbit changedOrbit = OrbitUtil.toModelOrbit(orbit);

				//Adding spacecraft object to modelOrbit
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(orbit.getSpacecraftCode());
				changedOrbit.setSpacecraft(spacecraft);
				
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
				
				return OrbitUtil.toRestOrbit(modelOrbit);
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<>(restOrbit, HttpStatus.OK);
	
	
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
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		
		try {
			transactionTemplate.execute((status) -> {
				// Test whether the orbit id is valid
				Optional<Orbit> modelOrbit = RepositoryService.getOrbitRepository().findById(id);
				if (modelOrbit.isEmpty()) {
					throw new NoResultException(String.format(MSG_ORBIT_NOT_FOUND, id, MSG_ID_ORBIT_NOT_FOUND));
				}		
				// Delete the orbit
				RepositoryService.getOrbitRepository().deleteById(id);

				// Test whether the deletion was successful
				modelOrbit = RepositoryService.getOrbitRepository().findById(id);
				if (!modelOrbit.isEmpty()) {
					throw new RuntimeException(String.format(MSG_DELETION_UNSUCCESSFUL, id, MSG_ID_DELETION_UNSUCCESSFUL));
				}
				
				return null;
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (RuntimeException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
	
	}

	

}
