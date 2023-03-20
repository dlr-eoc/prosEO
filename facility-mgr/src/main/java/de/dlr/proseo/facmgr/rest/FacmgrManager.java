package de.dlr.proseo.facmgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.facmgr.rest.model.FacmgrUtil;
import de.dlr.proseo.facmgr.rest.model.RestProcessingFacility;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.FacilityMgrMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
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

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(FacmgrManager.class);
	
	public RestProcessingFacility createFacility(RestProcessingFacility facility) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createFacility({})", (null == facility ? "MISSING" : facility.getName()));
		
		if (null == facility) {
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.FACILITY_MISSING));
		}
		
		// Make sure the facility does not yet exist
		ProcessingFacility modelFacility = RepositoryService.getFacilityRepository().findByName(facility.getName());
		if (null != modelFacility) {
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.DUPLICATE_FACILITY, facility.getName()));
		}
		
		modelFacility = FacmgrUtil.toModelFacility(facility);
		
		modelFacility = RepositoryService.getFacilityRepository().save(modelFacility);
		logger.log(FacilityMgrMessage.FACILITY_CREATED, facility.getName());
		return FacmgrUtil.toRestFacility(modelFacility);
	}
	/**
	 * List of all facilities filtered by mission and name
	 * 
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
			throw new NoResultException(logger.log(FacilityMgrMessage.FACILITY_LIST_EMPTY));
			
		}
		logger.log(FacilityMgrMessage.FACILITY_LIST_RETRIEVED, result.size(), name);

		return result;
	}

	
	/**
	 * Find the facility with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the facility found
	 * @throws IllegalArgumentException if no facility ID was given
	 * @throws NoResultException if no facility with the given ID exists
	 */
	public RestProcessingFacility getFacilityById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getFacilityById({})", id);
		
		if (null == id) {
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.FACILITY_MISSING, id));
		}	
		Optional<ProcessingFacility> modelFacility = RepositoryService.getFacilityRepository().findById(id);
		
		if (modelFacility.isEmpty()) {
			throw new NoResultException(logger.log(FacilityMgrMessage.FACILITY_NOT_FOUND, id));
		}		
		logger.log(FacilityMgrMessage.FACILITY_RETRIEVED, id);
		
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
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.FACILITY_MISSING, id));
		}
		
		Optional<ProcessingFacility> optModelFacility = RepositoryService.getFacilityRepository().findById(id);
				
		if (optModelFacility.isEmpty()) {
			throw new EntityNotFoundException(logger.log(FacilityMgrMessage.FACILITY_NOT_FOUND, id));
		}
		ProcessingFacility modelFacility = optModelFacility.get();
		
		// Update modified attributes
		boolean facilityChanged = false;
		ProcessingFacility changedFacility = FacmgrUtil.toModelFacility(restFacility);
		
		if (!modelFacility.getName().equals(changedFacility.getName())) {
			facilityChanged = true;
			modelFacility.setName(changedFacility.getName());
		}
		if (null == modelFacility.getDescription()) {
			if (null == changedFacility.getDescription()) {
				// No change
			} else {
				facilityChanged = true;
				modelFacility.setDescription(changedFacility.getDescription());
			}
		} else if (!modelFacility.getDescription().equals(changedFacility.getDescription())) {
			facilityChanged = true;
			modelFacility.setDescription(changedFacility.getDescription());
		}	
		if (null == modelFacility.getFacilityState()) {
			if (null == changedFacility.getFacilityState()) {
				// No change
			} else {
				facilityChanged = true;
				modelFacility.setFacilityState(changedFacility.getFacilityState());
			}
		} else if (!modelFacility.getFacilityState().equals(changedFacility.getFacilityState())) {
			facilityChanged = true;
			try {
				modelFacility.setFacilityState(changedFacility.getFacilityState());
			} catch (IllegalStateException e) {
				throw new IllegalArgumentException(logger.log(GeneralMessage.ILLEGAL_FACILITY_STATE_TRANSITION,
						modelFacility.getFacilityState().toString(), changedFacility.getFacilityState().toString()));
			}
		}	
		if (null == modelFacility.getProcessingEngineUrl()) {
			if (null == changedFacility.getProcessingEngineUrl()) {
				// No change
			} else {
				facilityChanged = true;
				modelFacility.setProcessingEngineUrl(changedFacility.getProcessingEngineUrl());
			}
		} else if (!modelFacility.getProcessingEngineUrl().equals(changedFacility.getProcessingEngineUrl())) {
			facilityChanged = true;
			modelFacility.setProcessingEngineUrl(changedFacility.getProcessingEngineUrl());
		}	
		if (null == modelFacility.getProcessingEngineToken()) {
			if (null == changedFacility.getProcessingEngineToken()) {
				// No change
			} else {
				facilityChanged = true;
				modelFacility.setProcessingEngineToken(changedFacility.getProcessingEngineToken());
			}
		} else if (!modelFacility.getProcessingEngineToken().equals(changedFacility.getProcessingEngineToken())) {
			facilityChanged = true;
			modelFacility.setProcessingEngineToken(changedFacility.getProcessingEngineToken());
		}	
		if (null == modelFacility.getMaxJobsPerNode()) {
			if (null == changedFacility.getMaxJobsPerNode()) {
				// No change
			} else {
				facilityChanged = true;
				modelFacility.setMaxJobsPerNode(changedFacility.getMaxJobsPerNode());
			}
		} else if (!modelFacility.getMaxJobsPerNode().equals(changedFacility.getMaxJobsPerNode())) {
			facilityChanged = true;
			modelFacility.setMaxJobsPerNode(changedFacility.getMaxJobsPerNode());
		}	
		if (!modelFacility.getStorageManagerUrl().equals(changedFacility.getStorageManagerUrl())) {
			facilityChanged = true;
			modelFacility.setStorageManagerUrl(changedFacility.getStorageManagerUrl());
		}
		if (!Objects.equals(modelFacility.getExternalStorageManagerUrl(), changedFacility.getExternalStorageManagerUrl())) {
			facilityChanged = true;
			modelFacility.setExternalStorageManagerUrl(changedFacility.getExternalStorageManagerUrl());
		}
		if (null == modelFacility.getLocalStorageManagerUrl()) {
			if (null == changedFacility.getLocalStorageManagerUrl()) {
				// No change
			} else {
				facilityChanged = true;
				modelFacility.setLocalStorageManagerUrl(changedFacility.getLocalStorageManagerUrl());
			}
		} else if (!modelFacility.getLocalStorageManagerUrl().equals(changedFacility.getLocalStorageManagerUrl())) {
			facilityChanged = true;
			modelFacility.setLocalStorageManagerUrl(changedFacility.getLocalStorageManagerUrl());
		}	
		if (!modelFacility.getStorageManagerUser().equals(changedFacility.getStorageManagerUser())) {
			facilityChanged = true;
			modelFacility.setStorageManagerUser(changedFacility.getStorageManagerUser());
		}	
		if (!modelFacility.getStorageManagerPassword().equals(changedFacility.getStorageManagerPassword())) {
			facilityChanged = true;
			modelFacility.setStorageManagerPassword(changedFacility.getStorageManagerPassword());
		}	
		if (!modelFacility.getDefaultStorageType().equals(changedFacility.getDefaultStorageType())) {
			facilityChanged = true;
			modelFacility.setDefaultStorageType(changedFacility.getDefaultStorageType());
		}	
		// Save order only if anything was actually changed
		if (facilityChanged)	{
			modelFacility.incrementVersion();
			modelFacility = RepositoryService.getFacilityRepository().save(modelFacility);
			logger.log(FacilityMgrMessage.FACILITY_MODIFIED, id);
		} else {
			logger.log(FacilityMgrMessage.FACILITY_NOT_MODIFIED, id);
		}
		return FacmgrUtil.toRestFacility(modelFacility);
	}
	
	/**
	 * Delete an facility by ID
	 * 
	 * @param id the ID of the facility to delete
	 * @throws EntityNotFoundException if the facility to delete does not exist in the database
	 * @throws IllegalArgumentException if the facility to delete still has stored products
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteFacilityById(Long id) throws EntityNotFoundException, IllegalArgumentException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteFacilityById({})", id);

		// Test whether the facility id is valid
		Optional<ProcessingFacility> modelFacility = RepositoryService.getFacilityRepository().findById(id);
		if (modelFacility.isEmpty()) {
			throw new EntityNotFoundException(logger.log(FacilityMgrMessage.FACILITY_NOT_FOUND));
		}
		
		// Test whether the facility still has stored products
		if (!RepositoryService.getProductFileRepository().findByProcessingFacilityId(modelFacility.get().getId()).isEmpty()) {
			throw new IllegalArgumentException(
					logger.log(FacilityMgrMessage.FACILITY_HAS_PRODUCTS, modelFacility.get().getName()));
		};
		
		// Delete the facility
		RepositoryService.getFacilityRepository().deleteById(id);

		// Test whether the deletion was successful
		modelFacility = RepositoryService.getFacilityRepository().findById(id);
		if (!modelFacility.isEmpty()) {
			throw new RuntimeException(logger.log(FacilityMgrMessage.DELETION_UNSUCCESSFUL, id));
		}
		
		logger.log(FacilityMgrMessage.FACILITY_DELETED, id);
		
	}
	
}
