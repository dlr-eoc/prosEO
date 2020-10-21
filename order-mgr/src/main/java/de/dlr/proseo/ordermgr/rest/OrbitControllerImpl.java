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
import de.dlr.proseo.model.service.SecurityService;
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
	private static final int MSG_ID_ORBIT_NOT_FOUND = 1050;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 1051;
	private static final int MSG_ID_ORBIT_MISSING = 1052;
	private static final int MSG_ID_ORBIT_INCOMPLETE = 1053;
	private static final int MSG_ID_NO_ORBITS_FOUND = 1054;
	private static final int MSG_ID_SPACECRAFT_NOT_FOUND = 1055;
	private static final int MSG_ID_ORBITS_RETRIEVED = 1056;
	private static final int MSG_ID_ORBITS_CREATED = 1057;
	private static final int MSG_ID_ORBIT_RETRIEVED = 1058;
	private static final int MSG_ID_ORBIT_UPDATED = 1059;
	private static final int MSG_ID_ORBIT_DELETED = 1060;

	// Same as in other services
	private static final int MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS = 2028;
	// private static final int MSG_ID_NOT_IMPLEMENTED = 9000;

	/* Message string constants */
	private static final String MSG_ORBIT_NOT_FOUND = "(E%d) No orbit found for ID %d";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Orbit deletion unsuccessful for ID %d";
	private static final String MSG_ORBIT_MISSING = "(E%d) Orbit not set";
	private static final String MSG_ORBIT_INCOMPLETE = "(E%d) Spacecraft code not set in the search";
	private static final String MSG_NO_ORBITS_FOUND = "(E%d) No orbits found for given search criteria";
	private static final String MSG_SPACECRAFT_NOT_FOUND = "(E%d) Spacecraft %s not found";

	private static final String MSG_ORBITS_RETRIEVED = "(I%d) %d orbits retrieved";
	private static final String MSG_ORBITS_CREATED = "(I%d) %d orbits created or updated";
	private static final String MSG_ORBIT_RETRIEVED = "(I%d) Orbit %d retrieved";
	private static final String MSG_ORBIT_UPDATED = "(I%d) Orbit %d updated";
	private static final String MSG_ORBIT_DELETED = "(I%d) Orbit %d deleted";

	// Same as in other services
	private static final String MSG_ILLEGAL_CROSS_MISSION_ACCESS = "(E%d) Illegal cross-mission access to mission %s (logged in to %s)";
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-orbitcontroller ";

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

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
	 * @param spacecraftCode the spacecraft code to filter by (may be null)
	 * @param orbitNumberFrom the minimum order number requested (may be null)
	 * @param orbitNumberTo the maximum order number requested (may be null)
	 * @param startTimeFrom earliest sensing start time requested (may be null)
	 * @param startTimeTo latest sensing start time requested (may be null)
	 * @return HTTP status "OK" and a list of orbits or
	 *         HTTP status "NOT_FOUND" and an error message, if no orbits matching the search criteria were found, or
	 *         HTTP status "BAD_REQUEST" and an error message, if the request parameters were inconsistent, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "INTERNAL_SERVER_ERROR" on any unexpected exception
	 */
	
	@Override
	public ResponseEntity<List<RestOrbit>> getOrbits(String spacecraftCode, Long orbitNumberFrom,
			Long orbitNumberTo, @DateTimeFormat Date startTimeFrom, @DateTimeFormat Date startTimeTo) {
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
					if(null != startTimeFrom && null != startTimeTo) {
						for (de.dlr.proseo.model.Orbit orbit : matchOrbits) {
							logger.info("Orbit.starttime: "+orbit.getStartTime());
							logger.info("Orbit.stoptime: "+orbit.getStopTime());

							if (!(orbit.getStartTime().isBefore(startTimeFrom.toInstant())) && 
									!(orbit.getStopTime().isAfter(startTimeTo.toInstant()))) {
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
				else if (null != startTimeFrom && null != startTimeTo) {
					for (Orbit orbit : RepositoryService.getOrbitRepository()
							.findBySpacecraftCodeAndStartTimeBetween(spacecraftCode, startTimeFrom.toInstant(), startTimeTo.toInstant())) {
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
				if (result.isEmpty()) {
					throw new NoResultException(String.format(MSG_NO_ORBITS_FOUND, MSG_ID_NO_ORBITS_FOUND));
				}
				return result;
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// Ensure user is authorized for the mission to read
		if (!securityService.isAuthorizedForMission(resultList.get(0).getMissionCode())) {
			String message = String.format(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
					resultList.get(0).getMissionCode(), securityService.getMission());			
			logger.error(message);
			return new ResponseEntity<>(errorHeaders(message), HttpStatus.FORBIDDEN);
		}
		
		logger.info(String.format(MSG_ORBITS_RETRIEVED, MSG_ID_ORBITS_RETRIEVED, resultList.size()));
		
		return new ResponseEntity<>(resultList, HttpStatus.OK);								
	}
	
	/**
	 * Create orbit/s from the given Json object 
	 * @param orbit the List of Json object to create the orbit from
	 * @return a response containing 
	 *         HTTP status "CREATED" and a List of Json object corresponding to the orbit after persistence
     *             (with ID and version for all contained objects) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST" and an error message, if the orbit data was invalid, or
	 *         HTTP status "INTERNAL_SERVER_ERROR" and an error message, if any other error occurred
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
				List<RestOrbit> restOrbits = new ArrayList<>();
				//Insert every valid Rest orbit into the DB
				for(RestOrbit tomodelOrbit : orbit) {
					Orbit modelOrbit = OrbitUtil.toModelOrbit(tomodelOrbit);
					
					// Check for existing orbits and update them!
					Orbit updateOrbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(
							tomodelOrbit.getSpacecraftCode(), tomodelOrbit.getOrbitNumber().intValue());
					if (null == updateOrbit) {
						//Adding spacecraft object to modelOrbit
						Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(tomodelOrbit.getSpacecraftCode());
						if (null == spacecraft) {
							throw new IllegalArgumentException(String.format(
									MSG_SPACECRAFT_NOT_FOUND, MSG_ID_SPACECRAFT_NOT_FOUND, tomodelOrbit.getSpacecraftCode()));
						}
						modelOrbit.setSpacecraft(spacecraft);
					} else {
						updateOrbit.setStartTime(modelOrbit.getStartTime());
						updateOrbit.setStopTime(modelOrbit.getStopTime());
						modelOrbit = updateOrbit;
					}
					
					// Ensure user is authorized for the mission to update
					if (!securityService.isAuthorizedForMission(modelOrbit.getSpacecraft().getMission().getCode())) {
						throw new SecurityException(String.format(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
								modelOrbit.getSpacecraft().getMission().getCode(), securityService.getMission()));			
					}
					
					modelOrbit = RepositoryService.getOrbitRepository().save(modelOrbit);
					restOrbits.add(OrbitUtil.toRestOrbit(modelOrbit));
				}
				 
				return restOrbits;
			});
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SecurityException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
		
		logger.info(String.format(MSG_ORBITS_CREATED, MSG_ID_ORBITS_CREATED, restOrbitList.size()));
		
		return new ResponseEntity<>(restOrbitList, HttpStatus.CREATED);
	}

	/**
	 * Find the orbit with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a response entity containing
	 *         HTTP status "OK" and a Json object corresponding to the found orbit or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no orbit with the given ID exists, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "INTERNAL_SERVER_ERROR" and an error message, if any other error occurred
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
					throw new NoResultException(String.format(MSG_ORBIT_NOT_FOUND, MSG_ID_ORBIT_NOT_FOUND, id));
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
		
		// Ensure user is authorized for the mission to read
		if (!securityService.isAuthorizedForMission(restOrbit.getMissionCode())) {
			String message = String.format(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
					restOrbit.getMissionCode(), securityService.getMission());			
			logger.error(message);
			return new ResponseEntity<>(errorHeaders(message), HttpStatus.FORBIDDEN);
		}
		
		logger.info(String.format(MSG_ORBIT_RETRIEVED, MSG_ID_ORBIT_RETRIEVED, restOrbit.getOrbitNumber()));
		
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
					throw new NoResultException(String.format(MSG_ORBIT_NOT_FOUND, MSG_ID_ORBIT_NOT_FOUND, id));
				}
				Orbit modelOrbit = optModelOrbit.get();
				
				// Update modified attributes
				boolean orbitChanged = false;
				Orbit changedOrbit = OrbitUtil.toModelOrbit(orbit);

				//Adding spacecraft object to modelOrbit
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByCode(orbit.getSpacecraftCode());
				if (null == spacecraft) {
					throw new IllegalArgumentException(String.format(
							MSG_SPACECRAFT_NOT_FOUND, MSG_ID_SPACECRAFT_NOT_FOUND, orbit.getSpacecraftCode()));
				}
				changedOrbit.setSpacecraft(spacecraft);
				
				// Ensure user is authorized for the mission to update
				if (!securityService.isAuthorizedForMission(spacecraft.getMission().getCode())) {
					throw new SecurityException(String.format(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
							spacecraft.getMission().getCode(), securityService.getMission()));			
				}
				
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
		} catch (SecurityException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
		
		logger.info(String.format(MSG_ORBIT_UPDATED, MSG_ID_ORBIT_UPDATED, restOrbit.getOrbitNumber()));
		
		return new ResponseEntity<>(restOrbit, HttpStatus.OK);
	}

	/**
	 * Delete an orbit by ID
	 * 
	 * @param id the ID of the orbit to delete
	 * @return a response entity with 
	 *         HTTP status "NO_CONTENT", if the deletion was successful, or
     *         HTTP status "NOT_FOUND", if the orbit did not exist, or
     *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
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
					throw new NoResultException(String.format(MSG_ORBIT_NOT_FOUND, MSG_ID_ORBIT_NOT_FOUND, id));
				}		

				// Ensure user is authorized for the mission to update
				if (!securityService.isAuthorizedForMission(modelOrbit.get().getSpacecraft().getMission().getCode())) {
					throw new SecurityException(String.format(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
							modelOrbit.get().getSpacecraft().getMission().getCode(), securityService.getMission()));			
				}
				
				// Delete the orbit
				RepositoryService.getOrbitRepository().deleteById(id);

				// Test whether the deletion was successful
				modelOrbit = RepositoryService.getOrbitRepository().findById(id);
				if (!modelOrbit.isEmpty()) {
					throw new RuntimeException(String.format(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
				}
				
				return null;
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SecurityException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
		
		logger.info(String.format(MSG_ORBIT_DELETED, MSG_ID_ORBIT_DELETED, id));
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
	}
}
