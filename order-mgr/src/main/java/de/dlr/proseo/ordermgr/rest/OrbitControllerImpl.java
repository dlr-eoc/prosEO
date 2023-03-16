/**
 * OrbitControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderMgrMessage;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.rest.OrbitController;
import de.dlr.proseo.model.rest.model.RestOrbit;
import de.dlr.proseo.ordermgr.OrdermgrConfiguration;
import de.dlr.proseo.ordermgr.rest.model.OrbitUtil;

/**
 * Spring MVC controller for the prosEO Order Manager; implements the services required to manage spacecraft orbits
 * 
 * @author Ranjitha Vignesh
 *
 */
@Component
public class OrbitControllerImpl implements OrbitController {

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** The order manager configuration */
	@Autowired
	private OrdermgrConfiguration config;
	
	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrbitControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDER_MGR);
	
	/**
	 * List of all orbits filtered by spacecraft code, orbit number range, starttime range
	 * 
	 * @param spacecraftCode the spacecraft code to filter by (may be null)
	 * @param orbitNumberFrom the minimum order number requested (may be null)
	 * @param orbitNumberTo the maximum order number requested (may be null)
	 * @param startTimeFrom earliest sensing start time requested (may be null)
	 * @param startTimeTo latest sensing start time requested (may be null)
	 * @param recordFrom first record of filtered and ordered result to return
	 * @param recordTo last record of filtered and ordered result to return
	 * @param orderBy an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white space
	 * @return HTTP status "OK" and a list of orbits or
	 *         HTTP status "NOT_FOUND" and an error message, if no orbits matching the search criteria were found, or
	 *         HTTP status "BAD_REQUEST" and an error message, if the request parameters were inconsistent, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "INTERNAL_SERVER_ERROR" on any unexpected exception
	 *         HTTP status "TOO MANY REQUESTS" if the result list exceeds a configured maximum
	 */

	@Transactional
	@Override
	public ResponseEntity<List<RestOrbit>> getOrbits(String spacecraftCode, Long orbitNumberFrom,
			Long orbitNumberTo, String startTimeFrom, String startTimeTo,
			Integer recordFrom, Integer recordTo, String[] orderBy) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrbit{}");
		
		/* Check arguments */
		if (null == spacecraftCode || "".equals(spacecraftCode)) {
			return new ResponseEntity<>(
					http.errorHeaders(logger.log(OrderMgrMessage.ORBIT_INCOMPLETE)), HttpStatus.BAD_REQUEST);
		}
		
		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
		}

		Long numberOfResults = Long.parseLong(this.countOrbits(spacecraftCode, orbitNumberFrom, orbitNumberTo, startTimeFrom, startTimeTo).getBody());
		Integer maxResults = config.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults
				&& (numberOfResults - recordFrom) > maxResults) {
			return new ResponseEntity<>(
					http.errorHeaders(logger.log(GeneralMessage.TOO_MANY_RESULTS, "orbits", numberOfResults, config.getMaxResults())), HttpStatus.TOO_MANY_REQUESTS);
		}

		List<RestOrbit> resultList = new ArrayList<RestOrbit>();

		// Find using search parameters
		Query query = createOrbitsQuery(spacecraftCode, orbitNumberFrom, orbitNumberTo, startTimeFrom, startTimeTo,
				recordFrom, recordTo, orderBy, false);
		
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Orbit) {
				// Filter depending on product visibility and user authorization
				Orbit orbit = (Orbit) resultObject;
				resultList.add(OrbitUtil.toRestOrbit(orbit));
			}
		}		
		if (resultList.isEmpty()) {
			return new ResponseEntity<>(resultList, HttpStatus.NOT_FOUND);
		}
		
		// Ensure user is authorized for the mission to read
		if (!securityService.isAuthorizedForMission(resultList.get(0).getMissionCode())) {
			String message = logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, resultList.get(0).getMissionCode(), securityService.getMission());
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.FORBIDDEN);
		}
		
		logger.log(OrderMgrMessage.ORBITS_RETRIEVED, resultList.size());
		
		return new ResponseEntity<>(resultList, HttpStatus.OK);								
	}

	
	/**
	 * List of all orbits filtered by spacecraft code, orbit number range, starttime range
	 * 
	 * @param spacecraftCode the spacecraft code to filter by (may be null)
	 * @param orbitNumberFrom the minimum order number requested (may be null)
	 * @param orbitNumberTo the maximum order number requested (may be null)
	 * @param startTimeFrom earliest sensing start time requested (may be null)
	 * @param startTimeTo latest sensing start time requested (may be null) sort direction (ASC/DESC), separated by white space
	 * @return HTTP status "OK" and the number of retrieved orbits or
	 *         HTTP status "NOT_FOUND" and an error message, if no orbits matching the search criteria were found, or
	 *         HTTP status "BAD_REQUEST" and an error message, if the request parameters were inconsistent, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "INTERNAL_SERVER_ERROR" on any unexpected exception
	 */

	@Transactional
	@Override
	public ResponseEntity<String> countOrbits(String spacecraftCode, Long orbitNumberFrom,
			Long orbitNumberTo, String startTimeFrom, String startTimeTo) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrbit{}");
		
		/* Check arguments */
		if (null == spacecraftCode || "".equals(spacecraftCode)) {
			return new ResponseEntity<>(
					http.errorHeaders(logger.log(OrderMgrMessage.ORBIT_INCOMPLETE)), HttpStatus.BAD_REQUEST);
		}

		// Find using search parameters
		Query query = createOrbitsQuery(spacecraftCode, orbitNumberFrom, orbitNumberTo, startTimeFrom, startTimeTo,
				null, null, null, true);
		Object resultObject = query.getSingleResult();
		if (resultObject instanceof Long) {
			return new ResponseEntity<>(((Long)resultObject).toString(), HttpStatus.OK);	
		}
		if (resultObject instanceof String) {
			return new ResponseEntity<>((String) resultObject, HttpStatus.OK);	
		}
		return new ResponseEntity<>("0", HttpStatus.OK);	
			
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
					http.errorHeaders(logger.log(OrderMgrMessage.ORBIT_MISSING)), HttpStatus.BAD_REQUEST);
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
					Orbit updateOrbit = RepositoryService.getOrbitRepository().findByMissionCodeAndSpacecraftCodeAndOrbitNumber(
							tomodelOrbit.getMissionCode(), tomodelOrbit.getSpacecraftCode(), tomodelOrbit.getOrbitNumber().intValue());
					if (null == updateOrbit) {
						//Adding spacecraft object to modelOrbit
						Spacecraft spacecraft = RepositoryService.getSpacecraftRepository()
								.findByMissionAndCode(tomodelOrbit.getMissionCode(), tomodelOrbit.getSpacecraftCode());
						if (null == spacecraft) {
							throw new IllegalArgumentException(logger.log(OrderMgrMessage.SPACECRAFT_NOT_FOUND,
									tomodelOrbit.getSpacecraftCode(), tomodelOrbit.getMissionCode()));
						}
						modelOrbit.setSpacecraft(spacecraft);
					} else {
						updateOrbit.setStartTime(modelOrbit.getStartTime());
						updateOrbit.setStopTime(modelOrbit.getStopTime());
						modelOrbit = updateOrbit;
					}
					
					// Ensure user is authorized for the mission to update
					if (!securityService.isAuthorizedForMission(modelOrbit.getSpacecraft().getMission().getCode())) {
						throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
								modelOrbit.getSpacecraft().getMission().getCode(), securityService.getMission()));			
					}
					
					modelOrbit = RepositoryService.getOrbitRepository().save(modelOrbit);
					restOrbits.add(OrbitUtil.toRestOrbit(modelOrbit));
				}
				 
				return restOrbits;
			});
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (TransactionException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
		
		logger.log(OrderMgrMessage.ORBITS_CREATED, restOrbitList.size());
		
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
					throw new NoResultException(logger.log(OrderMgrMessage.ORBIT_NOT_FOUND, id));
				}
				
				return OrbitUtil.toRestOrbit(modelOrbit.get());
			});
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// Ensure user is authorized for the mission to read
		if (!securityService.isAuthorizedForMission(restOrbit.getMissionCode())) {
			String message = logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					restOrbit.getMissionCode(), securityService.getMission());
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.FORBIDDEN);
		}
		
		logger.log(OrderMgrMessage.ORBIT_RETRIEVED, restOrbit.getOrbitNumber());
		
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
					throw new NoResultException(logger.log(OrderMgrMessage.ORBIT_NOT_FOUND, id));
				}
				Orbit modelOrbit = optModelOrbit.get();
				
				// Update modified attributes
				boolean orbitChanged = false;
				Orbit changedOrbit = OrbitUtil.toModelOrbit(orbit);

				//Adding spacecraft object to modelOrbit
				Spacecraft spacecraft = RepositoryService.getSpacecraftRepository()
						.findByMissionAndCode(orbit.getMissionCode(), orbit.getSpacecraftCode());
				if (null == spacecraft) {
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.SPACECRAFT_NOT_FOUND, orbit.getSpacecraftCode()));
				}
				changedOrbit.setSpacecraft(spacecraft);
				
				// Ensure user is authorized for the mission to update
				if (!securityService.isAuthorizedForMission(spacecraft.getMission().getCode())) {
					throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
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
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
		
		HttpStatus httpStatus = HttpStatus.OK;
		if (orbit.getVersion() == restOrbit.getVersion()) {
			httpStatus = HttpStatus.NOT_MODIFIED;
			logger.log(OrderMgrMessage.ORBIT_NOT_MODIFIED, id);
		} else {
			logger.log(OrderMgrMessage.ORBIT_UPDATED, restOrbit.getOrbitNumber());
		}
		
		return new ResponseEntity<>(restOrbit, httpStatus);
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
					throw new NoResultException(logger.log(OrderMgrMessage.ORBIT_NOT_FOUND, id));
				}		

				// Ensure user is authorized for the mission to update
				if (!securityService.isAuthorizedForMission(modelOrbit.get().getSpacecraft().getMission().getCode())) {
					throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
							modelOrbit.get().getSpacecraft().getMission().getCode(), securityService.getMission()));			
				}
				
				// Delete the orbit
				RepositoryService.getOrbitRepository().deleteById(id);

				// Test whether the deletion was successful
				modelOrbit = RepositoryService.getOrbitRepository().findById(id);
				if (!modelOrbit.isEmpty()) {
					throw new RuntimeException(logger.log(OrderMgrMessage.ORBIT_DELETION_UNSUCCESSFUL, id));
				}
				
				return null;
			});
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
		
		logger.log(OrderMgrMessage.ORBIT_DELETED, id);
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
	}

	private Query createOrbitsQuery(String spacecraftCode, Long orbitNumberFrom,
			Long orbitNumberTo, String startTimeFrom, String startTimeTo,
			Integer recordFrom, Integer recordTo, String[] orderBy, Boolean count) {

		// Find using search parameters
		String jpqlQuery = null;
		if (count) {
			jpqlQuery = "select count(x) from Orbit x where x.spacecraft.code = :spacecraftCode";
		} else {
			jpqlQuery = "select x from Orbit x where x.spacecraft.code = :spacecraftCode";
		}
		if (null != orbitNumberFrom) {
			jpqlQuery += " and x.orbitNumber >= :orbitNumberFrom";
		}
		if (null != orbitNumberTo) {
			jpqlQuery += " and x.orbitNumber <= :orbitNumberTo";
		}
		if (null != startTimeFrom) {
			jpqlQuery += " and x.startTime >= :startTimeFrom";
		}
		if (null != startTimeTo) {
			jpqlQuery += " and x.startTime <= :startTimeTo";
		}
		// order by
		if (null != orderBy && 0 < orderBy.length) {
			jpqlQuery += " order by ";
			for (int i = 0; i < orderBy.length; ++i) {
				if (0 < i) jpqlQuery += ", ";
				jpqlQuery += "x.";
				jpqlQuery += orderBy[i];
			}
		}

		Query query = em.createQuery(jpqlQuery);
		query.setParameter("spacecraftCode", spacecraftCode);

		if (null != orbitNumberFrom) {
			query.setParameter("orbitNumberFrom", Integer.valueOf(orbitNumberFrom.toString()));
		}
		if (null != orbitNumberTo) {
			query.setParameter("orbitNumberTo", Integer.valueOf(orbitNumberTo.toString()));
		}
		if (null != startTimeFrom) {
			query.setParameter("startTimeFrom", OrbitTimeFormatter.parseDateTime(startTimeFrom));
		}
		if (null != startTimeTo) {
			query.setParameter("startTimeTo", OrbitTimeFormatter.parseDateTime(startTimeTo));
		}
		
		// length of record list
		if (recordFrom != null && recordFrom >= 0) {
			query.setFirstResult(recordFrom.intValue());
		}
		if (recordTo != null && recordTo >= 0) {
			query.setMaxResults(recordTo.intValue() - recordFrom.intValue());
		}
		return query;
	}
}
