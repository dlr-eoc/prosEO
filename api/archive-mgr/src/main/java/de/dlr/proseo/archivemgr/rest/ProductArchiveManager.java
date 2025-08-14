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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.archivemgr.ProductArchiveManagerConfiguration;
import de.dlr.proseo.archivemgr.rest.model.ProductArchiveModelMapper;
import de.dlr.proseo.archivemgr.rest.model.ProductArchiveRestMapper;
import de.dlr.proseo.archivemgr.rest.model.RestProductArchive;
import de.dlr.proseo.archivemgr.utils.StringUtils;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.FacilityMgrMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProductArchiveMgrMessage;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.enums.ArchiveType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;

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

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** The product archive manager configuration */
	@Autowired
	ProductArchiveManagerConfiguration config;

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
			throw new IllegalArgumentException(
					logger.log(ProductArchiveMgrMessage.DUPLICATED_ARCHIVE, restArchive.getCode()));
		}

		// all checks inside
		ProductArchive modelArchive = new ProductArchiveRestMapper(restArchive, securityService.getMission()).toModel();

		try {
			modelArchive = RepositoryService.getProductArchiveRepository().save(modelArchive);
		} catch (Exception e) {
			throw e;
		}

		logger.log(ProductArchiveMgrMessage.ARCHIVE_CREATED, modelArchive.getCode(), modelArchive.getId());

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
	 * @throws IllegalArgumentException if no product archive matching the given
	 *                                  code could be found
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
		} else {
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
	 * Create database query to count or get objects
	 *
	 * @param code        the archive code
	 * @param name        the archive name
	 * @param archiveType the archive type
	 * @param count       if true create query for count of objects
	 * @return a database query
	 */
	public Query createArchivesQuery(Long id, String code, String name, String archiveType, Boolean count) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createArchivesQuery({}, {}, {})", id, name, archiveType);

		String jpqlQuery = "";
		if (count) {
			jpqlQuery = "select count(pa) from ProductArchive pa";
		} else {
			jpqlQuery = "select pa from ProductArchive pa";
		}

		// adds WHERE condition
		if ((null != id) || (null != name) || (null != archiveType)) {
			
			jpqlQuery += " where ";
			String and = " ";
			if (null != id) {
				jpqlQuery += and + "id = :id";
				and = " and ";
			}

			if (null != code && !code.isEmpty()) {
				jpqlQuery += and + "code = :code";
				and = " and ";
			}

			if (null != name && !name.isEmpty()) {
				jpqlQuery += and + "upper(name) like :name";
				and = " and ";
			}

			if (null != archiveType && !archiveType.isEmpty()) {
				jpqlQuery += and + "archiveType = :archiveType";
				and = " and ";
			}
		}

		if (!count) {
			jpqlQuery += " ORDER BY pa.code";
		}

		Query query = em.createQuery(jpqlQuery);

		if (null != id) {
			query.setParameter("id", id);
		}

		if (null != name) {
			query.setParameter("name", name);
		}

		try {
			if (null != archiveType) {
				query.setParameter("archiveType", ArchiveType.valueOf(archiveType));
			}
		} catch (Exception e) {
			throw new NoResultException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND, name, archiveType));
		}

		return query;
	}

	
	/**
	 * Get product archives by name and archive type
	 *
	 * @param code        the archive code
	 * @param name        the archive name
	 * @param archiveType the archive type
	 * @param recordFrom  first record of filtered and ordered result to return
	 * @param recordTo    last record of filtered and ordered result to return
	 * @return a list of Json objects representing product archives satisfying the
	 *         search criteria
	 * @throws NoResultException if no product archives matching the given search
	 *                           criteria could be found
	 */
	public List<RestProductArchive> getArchives(Long id, String code, String name, String archiveType, Integer recordFrom,
			Integer recordTo) throws NoResultException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getArchives({}, {}, {}, {}, {}, {})", id, code, name, archiveType, recordFrom, recordTo);

		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
		}

		Long numberOfResults = Long.parseLong(this.countArchives(id, code, name, archiveType));
		Integer maxResults = config.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults
				&& (numberOfResults - recordFrom) > maxResults) {
			throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, logger.log(GeneralMessage.TOO_MANY_RESULTS,
					"productArchives", numberOfResults, config.getMaxResults()));
		}

		List<RestProductArchive> result = new ArrayList<>();

		Query query = createArchivesQuery(id, code, name, archiveType, false);

		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);

		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof ProductArchive) {
				result.add(new ProductArchiveModelMapper((ProductArchive) resultObject).toRest());
			}
		}

		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProductArchiveMgrMessage.ARCHIVE_NOT_FOUND, name, archiveType));
		}

		logger.log(ProductArchiveMgrMessage.ARCHIVE_LIST_RETRIEVED, result.size(), name, archiveType);

		return result;
	}

	/**
	 * Count the product archives matching the specified name and archive type
	 *
	 * @param name        the product archive name
	 * @param archiveType the product archive type
	 * @return the number of product archives found as string
	 */
	public String countArchives(Long id, String code, String name, String archiveType) {

		if (logger.isTraceEnabled())
			logger.trace(">>> countArchives({}, {}, {}, {})", id, code, name, archiveType);
		Query query = createArchivesQuery(id, code, name, archiveType, true);

		Object resultObject = query.getSingleResult();

		String result = "";
		if (resultObject instanceof Long) {
			result = ((Long) resultObject).toString();
		}
		if (resultObject instanceof String) {
			result = (String) resultObject;
		}

		return result;
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

		ProductArchive changedArchive = new ProductArchiveRestMapper(restArchive, securityService.getMission())
				.toModel();

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
	 * @param modelArchive   model archive
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

		if (!StringUtils.equalStrings(modelArchive.getPassword(), changedArchive.getPassword())) {
			modelArchive.setPassword(changedArchive.getPassword());
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

		if (!modelArchive.getAvailableProductClasses().equals(changedArchive.getAvailableProductClasses())) {
			modelArchive.setAvailableProductClasses(changedArchive.getAvailableProductClasses());
		}

	}

	/**
	 * Checks if an archive was changed
	 *
	 * @param modelArchive   Model Archive to check
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

		if (!modelArchive.getAvailableProductClasses().equals(changedArchive.getAvailableProductClasses())) {
			archiveChanged = true;
		}

		return archiveChanged;
	}

}