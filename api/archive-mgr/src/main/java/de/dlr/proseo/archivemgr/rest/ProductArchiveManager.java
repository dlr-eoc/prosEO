/**
 * ProductArchiveManager.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.HashSet;

import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.archivemgr.rest.model.ProductArchiveUtil;
import de.dlr.proseo.archivemgr.rest.model.RestProductArchive;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.FacilityMgrMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProductArchiveMgrMessage;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.dao.ProductClassRepository;
import de.dlr.proseo.model.enums.ArchiveType;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Service methods required to create, modify and delete product archives in the
 * prosEO database, and to query the database about such archives
 * 
 * @author Denys Chaykovskiy
 */
@Component
@Transactional
public class ProductArchiveManager {

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveManager.class);

	/**
	 * Creation of the product archive
	 * 
	 * @param restArchive Rest Product Archive
	 * @return created Rest Product Archive
	 * @throws IllegalArgumentException if mandatory parameters missed or wrong
	 * 
	 */
	public RestProductArchive createArchive(RestProductArchive restArchive) throws IllegalArgumentException {

		if (logger.isTraceEnabled())
			logger.trace(">>> createArchive({})", (null == restArchive ? "MISSING" : restArchive.getCode()));

		if (null == restArchive) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_MISSING));
		}

		// Make sure the facility does not yet exist
		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findByCode(restArchive.getCode());

		if (null != modelArchive) {
			throw new IllegalArgumentException(
					logger.log(ProductArchiveMgrMessage.DUPLICATED_ARCHIVE, restArchive.getCode()));
		}
		
		modelArchive = ProductArchiveUtil.toModelProductArchive(restArchive);	

		// TODO: Make it in a method, maybe in a separate class
		// here, not in util
		String code = restArchive.getCode();
		ProductClassRepository repository = RepositoryService.getProductClassRepository();
		
		Set<ProductClass> modelProductClasses = new HashSet<>();
		
		for (String productType : restArchive.getAvailableProductClasses() ) {
			
			ProductClass productClass = repository.findByMissionCodeAndProductType(code, productType);

			// ProductClass with such code and productType not found
			if (null == productClass) {
				
				throw new IllegalArgumentException(
						logger.log(ProductArchiveMgrMessage.PRODUCT_CLASS_NOT_FOUND, code, productType));			
			}
			
			// adding product class, if not duplicated
			if (!modelProductClasses.add(productClass)) {
				
				throw new IllegalArgumentException(
						logger.log(ProductArchiveMgrMessage.DUPLICATED_PRODUCT_CLASS, productClass.getProductType(), restArchive.getCode()));			
			}
	
		}
		
		modelArchive.setAvailableProductClasses(modelProductClasses);
		
		// TODO: Check and remove to separate methods 
		// Set default values where possible
		if (null == modelArchive.getArchiveType()) {
			modelArchive.setArchiveType(ArchiveType.AIP);
		}

		// Ensure that mandatory attributes are set
		if (modelArchive.getTokenRequired() && null == modelArchive.getTokenUri()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "TokenUri", "produch archive creation"));
		}

		modelArchive = RepositoryService.getProductArchiveRepository().save(modelArchive);
		logger.log(FacilityMgrMessage.FACILITY_CREATED, modelArchive.getName());
		return ProductArchiveUtil.toRestProductArchive(modelArchive);
	}

	/**
	 * List of all product archives archives filtered by name
	 * 
	 * @param name the name of the product archive
	 * @return a list of product archives
	 * @throws NoResultException if no product archives matching the given search
	 *                           criteria could be found
	 */
	public List<RestProductArchive> getArchives(String name) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getArchives({})", name);

		List<RestProductArchive> result = new ArrayList<>();

		// Simple case: no search criteria set
		if (null == name) {

			for (ProductArchive archive : RepositoryService.getProductArchiveRepository().findAll()) {
				if (logger.isDebugEnabled())
					logger.debug("Found product archive with ID {}", archive.getId());

				RestProductArchive restArchive = ProductArchiveUtil.toRestProductArchive(archive);
				if (logger.isDebugEnabled())
					logger.debug("Created result rest product archive with ID {}", restArchive.getId());

				result.add(restArchive);
			}
		} 
		
		// TODO: Use JPA to access to db
		// Search by name
		else {

			String jpqlQuery = "select p from ProductArchive p where p.name = :name";
			Query query = em.createQuery(jpqlQuery);
			query.setParameter("name", name);

			for (Object resultObject : query.getResultList()) {
				if (resultObject instanceof ProductArchive) {
					result.add(ProductArchiveUtil.toRestProductArchive((ProductArchive) resultObject));
				}
			}
		}

		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProductArchiveMgrMessage.ARCHIVE_LIST_EMPTY));
		}

		logger.log(ProductArchiveMgrMessage.ARCHIVE_LIST_RETRIEVED, result.size(), name);

		return result;
	}

	/**
	 * Find the product archive with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the product archive found
	 * @throws IllegalArgumentException if no product archive ID was given
	 * @throws NoResultException        if no product archive with the given ID
	 *                                  exists
	 */
	public RestProductArchive getArchiveById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getArchiveById({})", id);

		if (null == id) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_MISSING, id));
		}
		Optional<ProductArchive> modelArchive = RepositoryService.getProductArchiveRepository().findById(id);

		if (modelArchive.isEmpty()) {
			throw new NoResultException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND, id));
		}

		logger.log(ProductArchiveMgrMessage.ARCHIVE_RETRIEVED, id);

		return ProductArchiveUtil.toRestProductArchive(modelArchive.get());
	}

	/**
	 * Update the product archive with the given ID with the attribute values of the
	 * given Json object. Unchanged values must be provided, too, or they will be
	 * changed to null.
	 * 
	 * @param id          the ID of the product archive to update
	 * @param restArchive a Json object containing the modified (and unmodified)
	 *                    attributes
	 * @return a Json object corresponding to the product archive after modification
	 *         (with ID and version for all contained objects)
	 * @throws EntityNotFoundException         if no product with the given ID
	 *                                         exists
	 * @throws IllegalArgumentException        if any of the input data was invalid
	 * @throws ConcurrentModificationException if the facility has been modified
	 *                                         since retrieval by the client
	 */
	public RestProductArchive modifyArchive(Long id, RestProductArchive restArchive) {

		if (logger.isTraceEnabled())
			logger.trace(">>> modifyArchive({})", id);

		if (null == id) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_MISSING, id));
		}

		Optional<ProductArchive> optModelArchive = RepositoryService.getProductArchiveRepository().findById(id);

		if (optModelArchive.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND, id));
		}
		ProductArchive modelArchive = optModelArchive.get();
		
		checkMandatoryAttributes(modelArchive);


		// TODO: Check all fields, also if mandatory, maybe refactoring in methods
		// Update modified attributes
		boolean archiveChanged = false;
		ProductArchive changedArchive = ProductArchiveUtil.toModelProductArchive(restArchive);
		
		/*
		archiveChanged = isArchiveChanged(modelArchive, changedArchive);
		
		if (archiveChanged) {
			setChangedFields(modelArchive, changedArchive);
		}
		*/
		

		if (!modelArchive.getName().equals(changedArchive.getName())) {
			archiveChanged = true;
			modelArchive.setName(changedArchive.getName());
		}
		
		
		
//		if (!modelFacility.getDescription().equals(changedFacility.getDescription())) {
//			facilityChanged = true;
//			modelFacility.setDescription(changedFacility.getDescription());
//		}
//		if (!modelFacility.getFacilityState().equals(changedFacility.getFacilityState())) {
//			facilityChanged = true;
//			try {
//				modelFacility.setFacilityState(changedFacility.getFacilityState());
//			} catch (IllegalStateException e) {
//				throw new IllegalArgumentException(logger.log(GeneralMessage.ILLEGAL_FACILITY_STATE_TRANSITION,
//						modelFacility.getFacilityState().toString(), changedFacility.getFacilityState().toString()));
//			}
//		}
//		if (!modelFacility.getProcessingEngineUrl().equals(changedFacility.getProcessingEngineUrl())) {
//			facilityChanged = true;
//			modelFacility.setProcessingEngineUrl(changedFacility.getProcessingEngineUrl());
//		}
//		if (!modelFacility.getProcessingEngineToken().equals(changedFacility.getProcessingEngineToken())) {
//			facilityChanged = true;
//			modelFacility.setProcessingEngineToken(changedFacility.getProcessingEngineToken());
//		}
//		if (!modelFacility.getMaxJobsPerNode().equals(changedFacility.getMaxJobsPerNode())) {
//			facilityChanged = true;
//			modelFacility.setMaxJobsPerNode(changedFacility.getMaxJobsPerNode());
//		}
//		if (!modelFacility.getStorageManagerUrl().equals(changedFacility.getStorageManagerUrl())) {
//			facilityChanged = true;
//			modelFacility.setStorageManagerUrl(changedFacility.getStorageManagerUrl());
//		}
//		if (!modelFacility.getExternalStorageManagerUrl().equals(changedFacility.getExternalStorageManagerUrl())) {
//			facilityChanged = true;
//			modelFacility.setExternalStorageManagerUrl(changedFacility.getExternalStorageManagerUrl());
//		}
//		if (!modelFacility.getLocalStorageManagerUrl().equals(changedFacility.getLocalStorageManagerUrl())) {
//			facilityChanged = true;
//			modelFacility.setLocalStorageManagerUrl(changedFacility.getLocalStorageManagerUrl());
//		}
//		if (!modelFacility.getStorageManagerUser().equals(changedFacility.getStorageManagerUser())) {
//			facilityChanged = true;
//			modelFacility.setStorageManagerUser(changedFacility.getStorageManagerUser());
//		}
//		if (!modelFacility.getStorageManagerPassword().equals(changedFacility.getStorageManagerPassword())) {
//			facilityChanged = true;
//			modelFacility.setStorageManagerPassword(changedFacility.getStorageManagerPassword());
//		}
//		if (!modelFacility.getDefaultStorageType().equals(changedFacility.getDefaultStorageType())) {
//			facilityChanged = true;
//			modelFacility.setDefaultStorageType(changedFacility.getDefaultStorageType());
//		}
//		
		// Save order only if anything was actually changed
		if (archiveChanged) {
			modelArchive.incrementVersion();
			modelArchive = RepositoryService.getProductArchiveRepository().save(modelArchive);
			logger.log(ProductArchiveMgrMessage.ARCHIVE_MODIFIED, id);
		} else {
			logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_MODIFIED, id);
		}
		return ProductArchiveUtil.toRestProductArchive(modelArchive);

	}

	/**
	 * Checks that mandatory attributes are set
	 * 
	 * @param modelArchive Model Archive
	 */
	private void checkMandatoryAttributes(ProductArchive modelArchive) {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> checkMandatoryAttributes()");

		if (null == modelArchive.getCode() || modelArchive.getCode().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Code", "Product archive modifcation"));
		}
		
		if (null == modelArchive.getName() || modelArchive.getName().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Name", "Product archive modifcation"));
		}
		
		if (null == modelArchive.getArchiveType()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "ArchiveType", "Product archive modifcation"));
		}
		
		if (null == modelArchive.getBaseUri() || modelArchive.getBaseUri().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "BaseUri", "Product archive modifcation"));
		}
		
		if (null == modelArchive.getContext() || modelArchive.getContext().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Context", "Product archive modifcation"));
		}
		
		if (null == modelArchive.getTokenRequired()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "TokenRequired", "Product archive modifcation"));
		}
	}

	/**
	 * Delete a product archive by ID
	 * 
	 * @param id the ID of the product archive to delete
	 * @throws EntityNotFoundException  if the product archive to delete does not
	 *                                  exist in the database
	 * @throws IllegalArgumentException if the product archive to delete still has
	 *                                  stored products
	 * @throws RuntimeException         if the deletion was not performed as
	 *                                  expected
	 */
	public void deleteArchiveById(Long id) throws EntityNotFoundException, IllegalArgumentException, RuntimeException {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteArchiveById({})", id);

		// Test whether the product id is valid
		Optional<ProductArchive> modelArchive = RepositoryService.getProductArchiveRepository().findById(id);
		if (modelArchive.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND));
		}
		
		// Delete the product
		RepositoryService.getProductArchiveRepository().deleteById(id);

		// Test whether the deletion was successful
		modelArchive = RepositoryService.getProductArchiveRepository().findById(id);
		if (!modelArchive.isEmpty()) {
			throw new RuntimeException(logger.log(FacilityMgrMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(ProductArchiveMgrMessage.ARCHIVE_DELETED, id);
	}
}
