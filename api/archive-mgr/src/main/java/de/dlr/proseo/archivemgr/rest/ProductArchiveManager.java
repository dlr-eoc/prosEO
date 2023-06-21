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
import de.dlr.proseo.archivemgr.utils.StringUtils;
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
		
		if (archiveExistsByCode(restArchive.getCode())) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.DUPLICATED_ARCHIVE, restArchive.getCode()));
		}
		
		// all checks inside
		ProductArchive modelArchive = new ProductArchiveRestMapper(restArchive).toModel();		
		
		try {
			modelArchive = RepositoryService.getProductArchiveRepository().save(modelArchive);
		}
		catch (Exception e) {
			throw e; 			
		}
		
		logger.log(ProductArchiveMgrMessage.ARCHIVE_CREATED, modelArchive.getName());
		
		return new ProductArchiveModelMapper(modelArchive).toRest();
	}
	
	/**
	 * Checks if archive exists, filtered by code
	 * 
	 * @param code code of the archive
	 * @return true if exists
	 */
	public boolean archiveExistsByCode(String code) {
		
		if (null == code) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_CODE_MISSING));
		}
			
		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findByCode(code);
		
		return modelArchive != null ? true : false; 		
	}
	
	/**
	 * Checks if archive exists, filtered by id
	 * 
	 * @param id id of the archive
	 * @return true if exists
	 */
	public boolean archiveExistsById(Long id) {
		
		if (null == id) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_ID_MISSING));
		}
			
		Optional<ProductArchive> modelArchive = RepositoryService.getProductArchiveRepository().findById(id);

		return !modelArchive.isEmpty() ? true : false; 		
	}
	
	/**
	 * Gets a product archive, searched by code
	 * 
	 * @param code the code of the product archive
	 * @return product archive
	 * @throws IllegalArgumentException if no product archive matching the given code could be found
	 */
	public RestProductArchive getArchiveByCode(String code) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getArchive({})", code);
		
		if (!archiveExistsByCode(code)) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND, code));
		}
		
		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findByCode(code);
		
		logger.log(ProductArchiveMgrMessage.ARCHIVE_RETRIEVED, code);
		
		return new ProductArchiveModelMapper(modelArchive).toRest();
	}

	/**
	 * List of all product archives archives filtered by code
	 * 
	 * @param code the code of the product archive
	 * @return a list of product archives, if code == null, returns all archives
	 * @throws NoResultException if no product archives matching the given search
	 *                           criteria could be found
	 */
	public List<RestProductArchive> getArchivesByCode(String code) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getArchives({})", code);

		List<RestProductArchive> result = new ArrayList<>();
		List<ProductArchive> modelArchives;

		if (null == code) {		
			modelArchives = RepositoryService.getProductArchiveRepository().findAll();
		}
		else {			
			modelArchives = RepositoryService.getProductArchiveRepository().findArchivesByCode(code);			
		}
		
		for (ProductArchive modelArchive : modelArchives) {
			
			if (logger.isDebugEnabled())
				logger.debug("Found product archive with ID {}", modelArchive.getId());

			RestProductArchive restArchive = new ProductArchiveModelMapper(modelArchive).toRest();

			result.add(restArchive);
		}
		
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProductArchiveMgrMessage.ARCHIVE_LIST_EMPTY));
		}

		logger.log(ProductArchiveMgrMessage.ARCHIVE_LIST_RETRIEVED, result.size(), code);

		return result;
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
		
		if (!archiveExistsById(id)) {
			throw new NoResultException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND, id));
		}
		
		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findById(id).get();

		logger.log(ProductArchiveMgrMessage.ARCHIVE_RETRIEVED, id);

		return new ProductArchiveModelMapper(modelArchive).toRest();
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
		
		if (null == restArchive) {
			throw new EntityNotFoundException(logger.log(ProductArchiveMgrMessage.ARCHIVE_MISSING));
		}
		
		if (!archiveExistsById(id)) {
			throw new EntityNotFoundException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND, id));
		}

		ProductArchive modelArchive = RepositoryService.getProductArchiveRepository().findById(id).get();
		modelArchive = new ProductArchiveModelMapper(modelArchive).get();
		
		ProductArchive changedArchive = new ProductArchiveRestMapper(restArchive).toModel();
		
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
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_ID_MISSING));
		}

		if (!archiveExistsById(id)) {
			throw new EntityNotFoundException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND));
		}
			
		RepositoryService.getProductArchiveRepository().deleteById(id);
		
		if (archiveExistsById(id)) {
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
		
		if (logger.isTraceEnabled())
			logger.trace(">>> setChangedFields({}, {})", modelArchive, changedArchive);
		
		if (null == modelArchive || null == changedArchive) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_MISSING));
		}
				
		if (!StringUtils.equalStrings(modelArchive.getCode(), changedArchive.getCode())) {		
			modelArchive.setCode(changedArchive.getCode());
		}

		if (!StringUtils.equalStrings(modelArchive.getName(), changedArchive.getName())) {		
			modelArchive.setName(changedArchive.getName());
		}
		
		if (!modelArchive.getArchiveType().equals(changedArchive.getArchiveType())) {
			modelArchive.setArchiveType(changedArchive.getArchiveType());
		}
		
		if (!StringUtils.equalStrings(modelArchive.getBaseUri(), changedArchive.getBaseUri())) {		
			modelArchive.setBaseUri(changedArchive.getBaseUri());
		}
		
		if (!StringUtils.equalStrings(modelArchive.getContext(), changedArchive.getContext())) {		
			modelArchive.setContext(changedArchive.getContext());
		}
		
		if (!modelArchive.getTokenRequired().equals(changedArchive.getTokenRequired())) {
			modelArchive.setTokenRequired(changedArchive.getTokenRequired());
		}
		
		if (!StringUtils.equalStrings(modelArchive.getTokenUri(), changedArchive.getTokenUri())) {		
			modelArchive.setTokenUri(changedArchive.getTokenUri());
		}
		
		if (!StringUtils.equalStrings(modelArchive.getUsername(), changedArchive.getUsername())) {		
			modelArchive.setUsername(changedArchive.getUsername());
		}
		
		if (!StringUtils.equalStrings(modelArchive.getClientId(), changedArchive.getClientId())) {		
			modelArchive.setClientId(changedArchive.getClientId());
		}
		
		if (!StringUtils.equalStrings(modelArchive.getClientSecret(), changedArchive.getClientSecret())) {		
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
		
		if (!StringUtils.equalStrings(modelArchive.getTokenUri(), changedArchive.getTokenUri())) {
			archiveChanged = true;
		}
		
		if (!StringUtils.equalStrings(modelArchive.getUsername(), changedArchive.getUsername())) {
			archiveChanged = true;
		}
		
		if (!StringUtils.equalStrings(modelArchive.getClientId(), changedArchive.getClientId())) {
			archiveChanged = true;
		}

		if (!StringUtils.equalStrings(modelArchive.getClientSecret(), changedArchive.getClientSecret())) {
			archiveChanged = true;
		}
		
		if (!modelArchive.getSendAuthInBody().equals(changedArchive.getSendAuthInBody())) {
			archiveChanged = true;
		}
		
		return archiveChanged; 
	}


	
	
}
