/**
 * FacmgrManager.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.facmgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.facmgr.rest.model.FacmgrUtil;
import de.dlr.proseo.facmgr.rest.model.RestProcessingFacility;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.FacilityMgrMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Service methods required to create, modify and delete processing facility in the prosEO database, and to query the database about
 * such facilities
 * 
 * @author Ranjitha Vignesh
 */
@Component
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class FacmgrManager {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(FacmgrManager.class);

	/**
	 * Create a processing facility with the specified attributes in the database.
	 * 
	 * @param restFacility The ProcessingFacility to create in REST format
	 * @return The created RestProcessingFacility
	 * @throws IllegalArgumentException in case of invalid input data
	 */
	public RestProcessingFacility createFacility(RestProcessingFacility restFacility) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createFacility({})", (null == restFacility ? "MISSING" : restFacility.getName()));

		if (null == restFacility) {
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.FACILITY_MISSING));
		}
		if (null == restFacility.getName() || restFacility.getName().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "name", "facility creation"));
		}

		// Make sure the facility does not yet exist
		ProcessingFacility modelFacility = RepositoryService.getFacilityRepository().findByName(restFacility.getName());
		if (null != modelFacility) {
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.DUPLICATE_FACILITY, restFacility.getName()));
		}

		modelFacility = FacmgrUtil.toModelFacility(restFacility);

		// Set default values where possible
		if (null == modelFacility.getFacilityState()) {
			modelFacility.setFacilityState(FacilityState.DISABLED);
		}

		// Ensure that mandatory attributes are set
		if (null == modelFacility.getStorageManagerUrl() || modelFacility.getStorageManagerUrl().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "StorageManagerUrl", "facility creation"));
		}
		if (null == modelFacility.getExternalStorageManagerUrl() || modelFacility.getExternalStorageManagerUrl().isBlank()) {
			if (null == modelFacility.getStorageManagerUrl()) {
				throw new IllegalArgumentException(
						logger.log(GeneralMessage.FIELD_NOT_SET, "ExternalStorageManagerUrl", "facility creation"));
			}
			// TODO Set to storage manager URL if empty?
		}
		if (null == modelFacility.getStorageManagerUser() || modelFacility.getStorageManagerUser().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "StorageManagerUser", "facility creation"));
		}
		if (null == modelFacility.getStorageManagerPassword()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "StorageManagerPassword", "facility creation"));
		}
		if (null == modelFacility.getDefaultStorageType() || modelFacility.getStorageManagerPassword().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "DefaultStorageType"));
		}

		// Save and return the new facility
		modelFacility = RepositoryService.getFacilityRepository().save(modelFacility);

		logger.log(FacilityMgrMessage.FACILITY_CREATED, restFacility.getName());

		return FacmgrUtil.toRestFacility(modelFacility);
	}

	/**
	 * Retrieve a list of facilities filtered by mission and name.
	 * 
	 * @param name the name of the facility
	 * @return a list of facilities matching mission and name
	 * @throws NoResultException if no facilities matching the given search criteria could be found
	 */
	public List<RestProcessingFacility> getFacility(String name) throws NoResultException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getFacilities({})", name);

		List<RestProcessingFacility> result = new ArrayList<>();

		if (null == name) {
			// Simple case: no search criteria set
			for (ProcessingFacility facility : RepositoryService.getFacilityRepository().findAll()) {
				if (logger.isDebugEnabled())
					logger.debug("Found facility with ID {}", facility.getId());

				RestProcessingFacility resultFacility = FacmgrUtil.toRestFacility(facility);

				if (logger.isDebugEnabled())
					logger.debug("Created result facilities with ID {}", resultFacility.getId());

				result.add(resultFacility);
			}
		} else {
			// Find using search parameters
			String jpqlQuery = "select p from ProcessingFacility p where 1 = 1";

			if (null != name) {
				jpqlQuery += " and p.name = :name";
			}

			Query query = em.createQuery(jpqlQuery);

			if (null != name) {
				query.setParameter("name", name);
			}

			for (Object resultObject : query.getResultList()) {
				if (resultObject instanceof ProcessingFacility) {
					result.add(FacmgrUtil.toRestFacility((ProcessingFacility) resultObject));
				}
			}
		}

		// Return retrieved facility or throw NoResultException
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(FacilityMgrMessage.FACILITY_LIST_EMPTY));
		}

		logger.log(FacilityMgrMessage.FACILITY_LIST_RETRIEVED, result.size(), name);

		return result;
	}

	/**
	 * Find the facility with the given ID.
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the facility found
	 * @throws IllegalArgumentException if no facility ID was given
	 * @throws NoResultException        if no facility with the given ID exists
	 */
	public RestProcessingFacility getFacilityById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getFacilityById({})", id);

		// Ensure an ID is given
		if (null == id) {
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.FACILITY_MISSING, id));
		}

		// Search for matching processing facility
		Optional<ProcessingFacility> modelFacility = RepositoryService.getFacilityRepository().findById(id);

		// Return facility if exists, otherwise throw NoResultException
		if (modelFacility.isEmpty()) {
			throw new NoResultException(logger.log(FacilityMgrMessage.FACILITY_NOT_FOUND, id));
		}

		logger.log(FacilityMgrMessage.FACILITY_RETRIEVED, id);

		return FacmgrUtil.toRestFacility(modelFacility.get());
	}

	/**
	 * Update the facility with the given ID with the attribute values of the given Json object. Unchanged values must be provided,
	 * too, or they will be changed to null.
	 * 
	 * @param id           the ID of the facility to update
	 * @param restFacility a Json object containing the modified and unmodified attributes
	 * @return a Json object corresponding to the facility after modification (with ID and version for all contained objects)
	 * @throws EntityNotFoundException         if no facility with the given ID exists
	 * @throws IllegalArgumentException        if any of the input data was invalid
	 * @throws ConcurrentModificationException if the facility has been modified since retrieval by the client
	 */
	public RestProcessingFacility modifyFacility(Long id, RestProcessingFacility restFacility)
			throws IllegalArgumentException, EntityNotFoundException, ConcurrentModificationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyFacility({})", id);

		// Ensure an ID is given
		if (null == id) {
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.FACILITY_MISSING, id));
		}

		// Search for matching processing facility and if none is found throw
		// EntityNotFoundException
		Optional<ProcessingFacility> optModelFacility = RepositoryService.getFacilityRepository().findById(id);

		if (optModelFacility.isEmpty()) {
			throw new EntityNotFoundException(logger.log(FacilityMgrMessage.FACILITY_NOT_FOUND, id));
		}

		ProcessingFacility modelFacility = optModelFacility.get();

		// Check that mandatory attributes are set
		if (null == restFacility.getStorageManagerUrl() || restFacility.getStorageManagerUrl().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "StorageManagerUrl", "facility modifcation"));
		}
		if (null == restFacility.getExternalStorageManagerUrl() || restFacility.getExternalStorageManagerUrl().isBlank())
			if (null == restFacility.getStorageManagerUrl()) {
				throw new IllegalArgumentException(
						logger.log(GeneralMessage.FIELD_NOT_SET, "ExternalStorageManagerUrl", "facility modifcation"));
			}
		if (null == restFacility.getStorageManagerUser() || restFacility.getStorageManagerUser().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "StorageManagerUser", "facility modifcation"));
		}
		if (null == restFacility.getStorageManagerPassword()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "StorageManagerPassword", "facility modifcation"));
		}
		if (null == restFacility.getDefaultStorageType() || restFacility.getStorageManagerPassword().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "DefaultStorageType", "facility modification"));
		}

		// Update modified attributes and keep track of change status
		boolean facilityChanged = false;

		ProcessingFacility changedFacility = FacmgrUtil.toModelFacility(restFacility);

		if (!modelFacility.getName().equals(changedFacility.getName())) {
			facilityChanged = true;
			modelFacility.setName(changedFacility.getName());
			// TODO Is that indeed allowed?
		}
		if (!modelFacility.getDescription().equals(changedFacility.getDescription())) {
			facilityChanged = true;
			modelFacility.setDescription(changedFacility.getDescription());
		}
		if (!modelFacility.getFacilityState().equals(changedFacility.getFacilityState())) {
			facilityChanged = true;
			try {
				modelFacility.setFacilityState(changedFacility.getFacilityState());
			} catch (IllegalStateException e) {
				throw new IllegalArgumentException(logger.log(GeneralMessage.ILLEGAL_FACILITY_STATE_TRANSITION,
						modelFacility.getFacilityState().toString(), changedFacility.getFacilityState().toString()));
			}
		}
		if (!modelFacility.getProcessingEngineUrl().equals(changedFacility.getProcessingEngineUrl())) {
			facilityChanged = true;
			modelFacility.setProcessingEngineUrl(changedFacility.getProcessingEngineUrl());
		}
		if (!modelFacility.getProcessingEngineToken().equals(changedFacility.getProcessingEngineToken())) {
			facilityChanged = true;
			modelFacility.setProcessingEngineToken(changedFacility.getProcessingEngineToken());
		}
		if (!modelFacility.getMaxJobsPerNode().equals(changedFacility.getMaxJobsPerNode())) {
			facilityChanged = true;
			modelFacility.setMaxJobsPerNode(changedFacility.getMaxJobsPerNode());
		}
		if (!modelFacility.getStorageManagerUrl().equals(changedFacility.getStorageManagerUrl())) {
			facilityChanged = true;
			modelFacility.setStorageManagerUrl(changedFacility.getStorageManagerUrl());
		}
		if (!modelFacility.getExternalStorageManagerUrl().equals(changedFacility.getExternalStorageManagerUrl())) {
			facilityChanged = true;
			modelFacility.setExternalStorageManagerUrl(changedFacility.getExternalStorageManagerUrl());
		}
		if (!modelFacility.getLocalStorageManagerUrl().equals(changedFacility.getLocalStorageManagerUrl())) {
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

		// Check for concurrent modification
		if (modelFacility.getVersion() != RepositoryService.getFacilityRepository().findById(id).get().getVersion()) {
			throw new ConcurrentModificationException(logger.log(GeneralMessage.CONCURRENT_MODIFICATION, "facility", id));
		}

		// Save order only if anything was actually changed
		if (facilityChanged) {
			modelFacility.incrementVersion();
			modelFacility = RepositoryService.getFacilityRepository().save(modelFacility);
			logger.log(FacilityMgrMessage.FACILITY_MODIFIED, id);
		} else {
			logger.log(FacilityMgrMessage.FACILITY_NOT_MODIFIED, id);
		}

		return FacmgrUtil.toRestFacility(modelFacility);
	}

	/**
	 * Delete the facility with the given ID.
	 * 
	 * @param id the ID of the facility to delete
	 * @throws EntityNotFoundException  if the facility to delete does not exist in the database
	 * @throws IllegalArgumentException if the facility to delete still has stored products
	 * @throws RuntimeException         if the deletion was not performed as expected
	 */
	public void deleteFacilityById(Long id) throws EntityNotFoundException, IllegalArgumentException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFacilityById({})", id);

		// Test whether the facility id is valid
		Optional<ProcessingFacility> modelFacility = RepositoryService.getFacilityRepository().findById(id);
		if (modelFacility.isEmpty()) {
			throw new EntityNotFoundException(logger.log(FacilityMgrMessage.FACILITY_NOT_FOUND));
		}

		// Test whether the facility still has stored products
		if (!RepositoryService.getProductFileRepository().findByProcessingFacilityId(modelFacility.get().getId()).isEmpty()) {
			throw new IllegalArgumentException(logger.log(FacilityMgrMessage.FACILITY_HAS_PRODUCTS, modelFacility.get().getName()));
		}

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