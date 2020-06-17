package de.dlr.proseo.facmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.facmgr.rest.model.FacmgrUtil;
import de.dlr.proseo.facmgr.rest.model.RestProcessingFacility;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.service.RepositoryService;


/**
 * Service methods required to create, modify and delete processing facility in the prosEO database,
 * and to query the database about such facilities
 * 
 * @author Ranjitha Vignesh
 */
@Component
@Transactional
public class FacmgrManager {
	/* Message ID constants */
	private static final int MSG_ID_FACILITY_NOT_FOUND = 1014;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 1015;
	//private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_FACILITY_MISSING = 1016;
	private static final int MSG_ID_FACILITY_DELETED = 1017;
	private static final int MSG_ID_FACILITY_RETRIEVED = 1018;
	private static final int MSG_ID_FACILITY_MODIFIED = 1019;
	private static final int MSG_ID_FACILITY_NOT_MODIFIED = 1020;
	private static final int MSG_ID_FACILITY_CREATED = 1021;
	private static final int MSG_ID_FACILITY_LIST_EMPTY = 1022;
	private static final int MSG_ID_FACILITY_LIST_RETRIEVED = 1023;



	/* Message string constants */
	private static final String MSG_FACILITY_NOT_FOUND = "(E%d) No facility found for ID %d";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Facility deletion unsuccessful for ID %d";
	private static final String MSG_FACILITY_LIST_EMPTY = "(E%d) No facilities found for search criteria";
	private static final String MSG_FACILITY_MISSING = "(E%d) Facility not set";
	private static final String MSG_FACILITY_DELETED = "(I%d) Facility with id %d deleted";
	private static final String MSG_FACILITY_ID_MISSING = "(E%d) Facility ID not set";
	private static final String MSG_FACILITY_RETRIEVED = "(I%d) Facility with ID %s retrieved";
	private static final String MSG_FACILITY_NOT_MODIFIED = "(I%d) Facility with id %d not modified (no changes)";
	private static final String MSG_FACILITY_MODIFIED = "(I%d) Facility with id %d modified";
	private static final String MSG_FACILITY_CREATED = "(I%d) Facility with identifier %s created";
	private static final String MSG_FACILITY_LIST_RETRIEVED = "(I%d) Facility list of size %d retrieved for facility '%s'";

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FacmgrManager.class);
	
	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholder in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info message
	 */
	private String logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.info(message);
		
		return message;
	}
	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);
		
		return message;
	}

	public RestProcessingFacility createFacility(RestProcessingFacility facility) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createFacility({})", (null == facility ? "MISSING" : facility.getName()));
		
		if (null == facility) {
			throw new IllegalArgumentException(logError(MSG_FACILITY_MISSING, MSG_ID_FACILITY_MISSING));
		}		
		
		ProcessingFacility modelFacility = FacmgrUtil.toModelFacility(facility);
		
		modelFacility = RepositoryService.getFacilityRepository().save(modelFacility);
		logInfo(MSG_FACILITY_CREATED, MSG_ID_FACILITY_CREATED, facility.getName());
		return FacmgrUtil.toRestFacility(modelFacility);
	}
	/**
	 * List of all facilities filtered by mission and name
	 * 
	 * @param mission the mission code
	 * @param name the name of the facility
	 * @return a list of facilities
	 * @throws NoResultException if no facilities matching the given search criteria could be found
	 */
	
	public List<RestProcessingFacility> getFacility(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> getFacilities({})", name);
		List<RestProcessingFacility> result = new ArrayList<>();
		
		if (null == name) {
			// Simple case: no search criteria set
			for (ProcessingFacility facility: RepositoryService.getFacilityRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found facility with ID {}", facility.getId());
				RestProcessingFacility resultFacility = FacmgrUtil.toRestFacility(facility);
				if (logger.isDebugEnabled()) logger.debug("Created result facilities with ID {}", resultFacility.getId());

				result.add(resultFacility);
			}
		}else {
			// Find using search parameters
			String jpqlQuery = "select p from ProcessingFacility p where 1 = 1";
			if (null != name) {
				jpqlQuery += " and p.name = :name";
			}
			Query query = em.createQuery(jpqlQuery);
			if (null != name) {
				query.setParameter("name", name);
			}
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof ProcessingFacility) {
					result.add(FacmgrUtil.toRestFacility((ProcessingFacility) resultObject));
				}
			}

		}
		if (result.isEmpty()) {
			throw new NoResultException(logError(MSG_FACILITY_LIST_EMPTY, MSG_ID_FACILITY_LIST_EMPTY));
			
		}
		logInfo(MSG_FACILITY_LIST_RETRIEVED, MSG_ID_FACILITY_LIST_RETRIEVED, result.size(), name);

		return result;
	}

	
	/**
	 * Find the facility with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the facility found
	 * @throws IllegalArgumentException if no order ID was given
	 * @throws NoResultException if no order with the given ID exists
	 */
	public RestProcessingFacility getFacilityById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getFacilityById({})", id);
		
		if (null == id) {
			throw new IllegalArgumentException(logError(MSG_FACILITY_ID_MISSING, MSG_ID_FACILITY_MISSING, id));
		}	
		Optional<ProcessingFacility> modelFacility = RepositoryService.getFacilityRepository().findById(id);
		
		if (modelFacility.isEmpty()) {
			throw new NoResultException(logError(MSG_FACILITY_NOT_FOUND, MSG_ID_FACILITY_NOT_FOUND, id));
		}		
		logInfo(MSG_FACILITY_RETRIEVED, MSG_ID_FACILITY_RETRIEVED, id);
		
		return FacmgrUtil.toRestFacility(modelFacility.get());
		
	}
	
	/**
	 * Update the facility with the given ID with the attribute values of the given Json object. 	 * 
	 * @param id the ID of the facility to update
	 * @param restFacility a Json object containing the modified (and unmodified) attributes
	 * @return a Json object corresponding to the facility after modification (with ID and version for all contained objects)
	 * @throws EntityNotFoundException if no product with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws ConcurrentModificationException if the facility has been modified since retrieval by the client
	 */
	public RestProcessingFacility modifyFacility(Long id, RestProcessingFacility restFacility) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyFacility({})", id);
		
		if (null == id) {
			throw new IllegalArgumentException(logError(MSG_FACILITY_ID_MISSING, MSG_ID_FACILITY_MISSING, id));
		}
		
		Optional<ProcessingFacility> optModelFacility = RepositoryService.getFacilityRepository().findById(id);
				
		if (optModelFacility.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_FACILITY_NOT_FOUND, MSG_ID_FACILITY_NOT_FOUND, id));
		}
		ProcessingFacility modelFacility = optModelFacility.get();
		
		// Update modified attributes
		boolean facilityChanged = false;
		ProcessingFacility changedFacility = FacmgrUtil.toModelFacility(restFacility);
		
		if (!modelFacility.getName().equals(changedFacility.getName())) {
			facilityChanged = true;
			modelFacility.setName(changedFacility.getName());
		}
		if (!modelFacility.getDescription().equals(changedFacility.getDescription())) {
			facilityChanged = true;
			modelFacility.setDescription(changedFacility.getDescription());
		}	
		if (!modelFacility.getProcessingEngineUrl().equals(changedFacility.getProcessingEngineUrl())) {
			facilityChanged = true;
			modelFacility.setProcessingEngineUrl(changedFacility.getProcessingEngineUrl());
		}	
		if (!modelFacility.getStorageManagerUrl().equals(changedFacility.getStorageManagerUrl())) {
			facilityChanged = true;
			modelFacility.setStorageManagerUrl(changedFacility.getStorageManagerUrl());
		}	
//		if (!modelFacility.getDefaultStorageType().equals(changedFacility.getDefaultStorageType())) {
//			facilityChanged = true;
//			modelFacility.setDefaultStorageType(changedFacility.getDefaultStorageType());
//		}	
		// Save order only if anything was actually changed
		if (facilityChanged)	{
			modelFacility.incrementVersion();
			modelFacility = RepositoryService.getFacilityRepository().save(modelFacility);
			logInfo(MSG_FACILITY_MODIFIED, MSG_ID_FACILITY_MODIFIED, id);
			} else {
					logInfo(MSG_FACILITY_NOT_MODIFIED, MSG_ID_FACILITY_NOT_MODIFIED, id);
			}
			return FacmgrUtil.toRestFacility(modelFacility);
	}
	
	/**
	 * Delete an order by ID
	 * 
	 * @param the ID of the order to delete
	 * @throws EntityNotFoundException if the order to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteFacilityById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteFacilityById({})", id);

		// Test whether the facility id is valid
		Optional<ProcessingFacility> modelFacility = RepositoryService.getFacilityRepository().findById(id);
		if (modelFacility.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_FACILITY_NOT_FOUND, MSG_ID_FACILITY_NOT_FOUND));
		}
		// Delete the order
		RepositoryService.getFacilityRepository().deleteById(id);

		// Test whether the deletion was successful
		modelFacility = RepositoryService.getFacilityRepository().findById(id);
		if (!modelFacility.isEmpty()) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
		}
		
		logInfo(MSG_FACILITY_DELETED, MSG_ID_FACILITY_DELETED, id);
		
	}
	
}
