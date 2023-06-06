/**
 * ProductArchiveManager.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.archivemgr.rest.model.ProductArchiveModelMapper;
import de.dlr.proseo.archivemgr.rest.model.ProductArchiveRestMapper;
import de.dlr.proseo.archivemgr.rest.model.RestProductArchive;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.FacilityMgrMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProductArchiveMgrMessage;
import de.dlr.proseo.model.ProductArchive;
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
		
		if (archiveExists(restArchive.getCode())) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.DUPLICATED_ARCHIVE, restArchive.getCode()));
		}

		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findByCode(restArchive.getCode());
		
		// all checks inside
		modelArchive = new ProductArchiveRestMapper(restArchive).toModel();		
		
		modelArchive = RepositoryService.getProductArchiveRepository().save(modelArchive);
		
		logger.log(ProductArchiveMgrMessage.ARCHIVE_CREATED, modelArchive.getName());
		return new ProductArchiveModelMapper(modelArchive).toRest();
	}

	/**
	 * List of all product archives archives filtered by name
	 * 
	 * @param name the name of the product archive
	 * @return a list of product archives, if name == null, returns all archives
	 * @throws NoResultException if no product archives matching the given search
	 *                           criteria could be found
	 */
	public List<RestProductArchive> getArchives(String name) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getArchives({})", name);

		List<RestProductArchive> result = new ArrayList<>();
		List<ProductArchive> modelArchives;

		if (null == name) {
			modelArchives = RepositoryService.getProductArchiveRepository().findAll();
		}
		else {
			modelArchives = RepositoryService.getProductArchiveRepository().findByName(name);			
		}
		
		for (ProductArchive modelArchive : modelArchives) {
			
			if (logger.isDebugEnabled())
				logger.debug("Found product archive with ID {}", modelArchive.getId());

			RestProductArchive restArchive = new ProductArchiveModelMapper(modelArchive).toRest();
			
			if (logger.isDebugEnabled())
				logger.debug("Created result rest product archive with ID {}", restArchive.getId());

			result.add(restArchive);
		}
		
		// TODO: Delete legacy code after confirmation  
		 /*		
		// Simple case: no search criteria set
		if (null == name) {

			for (ProductArchive archive : RepositoryService.getProductArchiveRepository().findAll()) {
				if (logger.isDebugEnabled())
					logger.debug("Found product archive with ID {}", archive.getId());

				RestProductArchive restArchive =new ProductArchiveModelMapper(archive).toRest();;
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
					result.add(new ProductArchiveModelMapper((ProductArchive) resultObject).toRest());
				}
			}
		}
		*/

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

		return new ProductArchiveModelMapper(modelArchive.get()).toRest();
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

		ProductArchive changedArchive = new ProductArchiveRestMapper(restArchive).toModel();
		
		// TODO: Maybe use here !equals for ProductArchive
		boolean archiveChanged = isArchiveChanged(modelArchive, changedArchive);
					
		// Save order only if anything was actually changed
		if (archiveChanged) {
			
			setChangedFields(modelArchive, changedArchive);

			modelArchive.incrementVersion();
			modelArchive = RepositoryService.getProductArchiveRepository().save(modelArchive);
			
			logger.log(ProductArchiveMgrMessage.ARCHIVE_MODIFIED, id);
			
		} else {
			
			logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_MODIFIED, id);
		}
		
		return new ProductArchiveModelMapper(modelArchive).toRest();
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
		
		if (null == id) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_MISSING, id));
		}

		// Test whether the product archive id is valid
		Optional<ProductArchive> modelArchive = RepositoryService.getProductArchiveRepository().findById(id);
		if (modelArchive.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND));
		}
		
		RepositoryService.getProductArchiveRepository().deleteById(id);

		// Test whether the deletion was successful
		modelArchive = RepositoryService.getProductArchiveRepository().findById(id);
		if (!modelArchive.isEmpty()) {
			throw new RuntimeException(logger.log(FacilityMgrMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(ProductArchiveMgrMessage.ARCHIVE_DELETED, id);
	}
	
	/**
	 * Sets changed fields in modelArchive from changedArchive
	 *
	 * @param modelArchive model archive
	 * @param changedArchive changed archive
	 */
	private void setChangedFields(ProductArchive modelArchive, ProductArchive changedArchive) {
		
		if (!modelArchive.getCode().equals(changedArchive.getCode())) {		
			modelArchive.setCode(changedArchive.getCode());
		}

		if (!modelArchive.getName().equals(changedArchive.getName())) {	
			modelArchive.setName(changedArchive.getName());
		}
		
		if (!modelArchive.getArchiveType().equals(changedArchive.getArchiveType())) {
			modelArchive.setArchiveType(changedArchive.getArchiveType());
		}
		
		if (!modelArchive.getBaseUri().equals(changedArchive.getBaseUri())) {
			modelArchive.setBaseUri(changedArchive.getBaseUri());
		}
		
		if (!modelArchive.getContext().equals(changedArchive.getContext())) {
			modelArchive.setContext(changedArchive.getContext());
		}
		
		if (!modelArchive.getTokenRequired().equals(changedArchive.getTokenRequired())) {
			modelArchive.setTokenRequired(changedArchive.getTokenRequired());
		}
		
		if (!modelArchive.getTokenUri().equals(changedArchive.getTokenUri())) {
			modelArchive.setTokenUri(changedArchive.getTokenUri());
		}
		
		if (!modelArchive.getUsername().equals(changedArchive.getUsername())) {
			modelArchive.setUsername(changedArchive.getUsername());
		}
		
		if (!modelArchive.getClientId().equals(changedArchive.getClientId())) {
			modelArchive.setClientId(changedArchive.getClientId());
		}
		
		if (!modelArchive.getClientSecret().equals(changedArchive.getClientSecret())) {
			modelArchive.setClientSecret(changedArchive.getClientSecret());
		}
		
		if (!modelArchive.getSendAuthInBody().equals(changedArchive.getSendAuthInBody())) {
			modelArchive.setSendAuthInBody(changedArchive.getSendAuthInBody());
		}
	}

	/**
	 * Checks if an archive was changed
	 * 
	 * @param modelArchive Model Archive to check
	 * @param changedArchive changed archive
	 * @return true, if the changedArchive was changed
	 */
	private boolean isArchiveChanged(ProductArchive modelArchive, ProductArchive changedArchive) {
		
		boolean archiveChanged = false;
		
		if (!modelArchive.getCode().equals(changedArchive.getCode())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getName().equals(changedArchive.getName())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getArchiveType().equals(changedArchive.getArchiveType())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getBaseUri().equals(changedArchive.getBaseUri())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getContext().equals(changedArchive.getContext())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getTokenRequired().equals(changedArchive.getTokenRequired())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getTokenUri().equals(changedArchive.getTokenUri())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getUsername().equals(changedArchive.getUsername())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getClientId().equals(changedArchive.getClientId())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getClientSecret().equals(changedArchive.getClientSecret())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getSendAuthInBody().equals(changedArchive.getSendAuthInBody())) {
			archiveChanged = true;
		}
		
		return archiveChanged; 
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
	 * Checks if archive exists
	 * 
	 * @param code code of the archive
	 * @return true if exists
	 */
	private boolean archiveExists(String code) {
		
		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findByCode(code);
		
		return modelArchive != null ? true : false; 		
	}
}
