/**
 * ProductManager.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestDownloadHistory;
import de.dlr.proseo.ingestor.rest.model.RestParameter;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.ingestor.rest.model.RestProductFile;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.DownloadHistory;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.enums.ProductQuality;
import de.dlr.proseo.model.enums.ProductVisibility;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Service methods required to create, modify and delete products in the prosEO
 * database, and to query the database about such products
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductManager {

	/* Other string constants */
	private static final String FACILITY_QUERY_SQL = "SELECT count(*) FROM product_processing_facilities ppf WHERE ppf.product_id = :product_id";

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductManager.class);

	/**
	 * Read the product with the given ID from the database
	 *
	 * @param id the ID to look for
	 * @return the requested database model product
	 * @throws IllegalArgumentException if no product ID was given
	 * @throws NoResultException        if no product with the given ID exists
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	private Product readProduct(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (null == id) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_ID_MISSING, id));
		}

		Optional<Product> modelProduct = RepositoryService.getProductRepository().findById(id);

		if (modelProduct.isEmpty()) {
			throw new NoResultException(logger.log(IngestorMessage.PRODUCT_NOT_FOUND, id));
		}

		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(modelProduct.get().getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProduct.get().getProductClass().getMission().getCode(), securityService.getMission()));
		}

		// Ensure product class is visible for user
		ProductVisibility visibility = modelProduct.get().getProductClass().getVisibility();
		switch (visibility) {
		case PUBLIC:
			break;
		case RESTRICTED:
			if (securityService.hasRole(UserRole.PRODUCT_READER_RESTRICTED)) {
				break;
			}
			// Fall through to test READER_ALL
		default: // Internal
			if (securityService.hasRole(UserRole.PRODUCT_READER_ALL)) {
				break;
			}
			// Product not visible for user
			throw new SecurityException(
					logger.log(IngestorMessage.VISIBILITY_VIOLATION, modelProduct.get().getProductClass().getProductType()));
		}
		Product product = modelProduct.get();
		return product;
	}

	/**
	 * Delete a product by ID
	 *
	 * @param id the ID of the product to delete
	 * @throws EntityNotFoundException if the product to delete does not exist in
	 *                                 the database
	 * @throws IllegalStateException   if the product to delete still as files at
	 *                                 some Processing Facility
	 * @throws SecurityException       if a cross-mission data access was attempted
	 * @throws RuntimeException        if the deletion was not performed as expected
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void deleteProductById(Long id)
			throws EntityNotFoundException, IllegalStateException, SecurityException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteProductById({})", id);

		// Test whether the product id is valid
		Optional<Product> modelProduct = RepositoryService.getProductRepository().findById(id);
		if (modelProduct.isEmpty()) {
			throw new EntityNotFoundException(logger.log(IngestorMessage.PRODUCT_NOT_FOUND, id));
		}

		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(modelProduct.get().getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProduct.get().getProductClass().getMission().getCode(), securityService.getMission()));
		}

		// Make sure product (including all component products) does not exist on any
		// Processing Facility
		if (hasProductFiles(modelProduct.get())) {
			throw new IllegalStateException(logger.log(IngestorMessage.PRODUCT_HAS_FILES, modelProduct.get().getId()));
		}

		// Delete the product
		RepositoryService.getProductRepository().deleteById(id);

		// Test whether the deletion was successful
		modelProduct = RepositoryService.getProductRepository().findById(id);
		if (!modelProduct.isEmpty()) {
			throw new RuntimeException(logger.log(IngestorMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(IngestorMessage.PRODUCT_DELETED, id);
	}

	/**
	 * Checks (recursively) whether the product or any of its component products has
	 * files at a processing facility
	 *
	 * @param product the product to check
	 * @return true, if some processing facility with files for this product was
	 *         found, false otherwise
	 */
	private boolean hasProductFiles(Product product) {
		if (logger.isTraceEnabled())
			logger.trace(">>> hasProductFiles({})", product.getId());

		Query query = em.createNativeQuery(FACILITY_QUERY_SQL);
		query.setParameter("product_id", product.getId());

		int resultCount = ((Number) query.getSingleResult()).intValue();
		if (logger.isDebugEnabled())
			logger.debug("Number of processing facility entries found: " + resultCount);

		return 0 < resultCount;
	}

	/**
	 * List of all products filtered by mission, product class, , production mode,
	 * file class, quality and time ranges
	 *
	 * @param mission       the mission code (will be set to logged in mission, if
	 *                      not given; otherwise must match logged in mission)
	 * @param productClass  an array of product types
	 * @param mode          the processing mode
	 * @param fileClass     the file class
	 * @param quality       the quality
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo   latest sensing start time
	 * @param genTimeFrom   earliest generation time
	 * @param genTimeTo     latest generation time
	 * @param recordFrom    first record of filtered and ordered result to return
	 * @param recordTo      last record of filtered and ordered result to return
	 * @param jobStepId     get input products of job step
	 * @param orderBy       an array of strings containing a column name and an
	 *                      optional sort direction (ASC/DESC), separated by white
	 *                      space
	 * @return a list of products
	 * @throws NoResultException if no products matching the given search criteria
	 *                           could be found
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public List<RestProduct> getProducts(String mission, String[] productClass, String mode, String fileClass, String quality,
			String startTimeFrom, String startTimeTo, String genTimeFrom, String genTimeTo, Integer recordFrom, Integer recordTo,
			Long jobStepId, String[] orderBy) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProducts({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})", mission,
					(null == productClass ? "null" : Arrays.asList(productClass).toString()), mode, fileClass, quality,
					startTimeFrom, startTimeTo, genTimeFrom, genTimeTo, recordFrom, recordTo, orderBy);

		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(
						logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission()));
			}
		}

		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
		}

		Long numberOfResults = Long.parseLong(this.countProducts(mission, productClass, mode, fileClass, quality, startTimeFrom,
				startTimeTo, genTimeFrom, genTimeTo, jobStepId));
		Integer maxResults = ingestorConfig.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults && (numberOfResults - recordFrom) > maxResults) {
			throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
					logger.log(GeneralMessage.TOO_MANY_RESULTS, "products", numberOfResults, ingestorConfig.getMaxResults()));
		}

		List<RestProduct> result = new ArrayList<>();

		// Find using search parameters
		Query query = createProductsQuery(mission, productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, genTimeFrom,
				genTimeTo, recordFrom, recordTo, jobStepId, orderBy, false);
		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);
		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof Product) {
				// Filter depending on product visibility and user authorization
				Product product = (Product) resultObject;
				result.add(ProductUtil.toRestProduct(product));
			}
		}

		if (result.isEmpty()) {
			throw new NoResultException(logger.log(IngestorMessage.PRODUCT_LIST_EMPTY));
		}

		logger.log(IngestorMessage.PRODUCT_LIST_RETRIEVED, result.size(), mission,
				(null == productClass ? "null" : Arrays.asList(productClass).toString()), startTimeFrom, startTimeTo);

		return result;
	}

	/**
	 * Get the number of products available, possibly filtered by mission, product
	 * class, production mode, file class, quality and time ranges
	 *
	 * @param mission       the mission code (will be set to logged in mission, if
	 *                      not given; otherwise must match logged in mission)
	 * @param productClass  an array of product types
	 * @param mode          the processing mode
	 * @param fileClass     the file class
	 * @param quality       the quality
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo   latest sensing start time
	 * @param genTimeFrom   earliest generation time
	 * @param genTimeTo     latest generation time
	 * @param jobStepId     get input products of job step
	 * @return the number of products found as string
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public String countProducts(String mission, String[] productClass, String mode, String fileClass, String quality,
			String startTimeFrom, String startTimeTo, String genTimeFrom, String genTimeTo, Long jobStepId)
			throws SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> countProducts({}, {}, {}, {}, {}, {}, {}, {}, {})", mission, productClass, mode, fileClass, quality,
					startTimeFrom, startTimeTo, genTimeFrom, genTimeTo);

		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(
						logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission()));
			}
		}
		Query query = createProductsQuery(mission, productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, genTimeFrom,
				genTimeTo, null, null, jobStepId, null, true);
		Object resultObject = query.getSingleResult();
		if (resultObject instanceof Long) {
			return ((Long) resultObject).toString();
		}
		if (resultObject instanceof String) {
			return (String) resultObject;
		}
		return "0";
	}

	/**
	 * Create a product from the given Json object (does NOT create associated
	 * product files!)
	 *
	 * @param product the Json object to create the product from
	 * @return a Json object corresponding to the product after persistence (with ID
	 *         and version for all contained objects)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public RestProduct createProduct(RestProduct product) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createProduct({})", (null == product ? "MISSING" : product.getProductClass()));

		if (null == product) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_MISSING));
		}

		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(product.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, product.getMissionCode(),
					securityService.getMission()));
		}

		// Ensure that mandatory attributes are set
		if (null == product.getProductClass() || product.getProductClass().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "productClass", "product creation"));
		}
		if (null == product.getSensingStartTime() || product.getSensingStartTime().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "sensingStartTime", "product creation"));
		}
		if (null == product.getSensingStopTime() || product.getSensingStopTime().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "sensingStopTime", "product creation"));
		}
		if (null == product.getGenerationTime() || product.getGenerationTime().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "generationTime", "product creation"));
		}

		// If list attributes were explicitly set to null, initialize with empty list to
		// avoid NullPointerExceptions
		if (null == product.getDownloadHistory()) {
			product.setDownloadHistory(new ArrayList<RestDownloadHistory>());
		}
		if (null == product.getComponentProductIds()) {
			product.setComponentProductIds(new ArrayList<Long>());
		}
		if (null == product.getProductFile()) {
			product.setProductFile(new ArrayList<RestProductFile>());
		}
		if (null == product.getParameters()) {
			product.setParameters(new ArrayList<RestParameter>());
		}

		Product modelProduct = ProductUtil.toModelProduct(product);

		// Check metadata database for product with same characteristics
		TypedQuery<Product> query = em
			.createQuery("select p from Product p where " + "p.productClass.mission.code = :missionCode and "
					+ "p.productClass.productType = :productType and " + "p.sensingStartTime = :sensingStart and "
					+ "p.sensingStopTime = :sensingStop", Product.class)
			.setParameter("missionCode", product.getMissionCode())
			.setParameter("productType", product.getProductClass())
			.setParameter("sensingStart", modelProduct.getSensingStartTime())
			.setParameter("sensingStop", modelProduct.getSensingStopTime());
		for (Product candidateProduct : query.getResultList()) {
			if (candidateProduct.equals(modelProduct)) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_EXISTS, candidateProduct.getId()));
			}
		}

		// Create a database model product
		if (null == modelProduct.getUuid()) {
			modelProduct.setUuid(UUID.randomUUID());
		} else {
			// Test if given UUID is not yet in use
			if (null != RepositoryService.getProductRepository().findByUuid(modelProduct.getUuid())) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.DUPLICATE_PRODUCT_UUID, product.getUuid()));
			}
		}

		// Add product class
		ProductClass modelProductClass = RepositoryService.getProductClassRepository()
			.findByMissionCodeAndProductType(product.getMissionCode(), product.getProductClass());
		if (null == modelProductClass) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_CLASS_INVALID, product.getProductClass()));
		}
		modelProduct.setProductClass(modelProductClass);

		// Add component products
		if (null != product.getComponentProductIds() && !product.getComponentProductIds().isEmpty()) {
			Set<ProductClass> allowedComponentClasses = new HashSet<>();
			for (SimpleSelectionRule rule : modelProductClass.getRequiredSelectionRules()) {
				allowedComponentClasses.add(rule.getSourceProductClass());
			}
			for (Long componentProductId : product.getComponentProductIds()) {
				Optional<Product> componentProduct = RepositoryService.getProductRepository().findById(componentProductId);
				if (componentProduct.isEmpty()) {
					throw new IllegalArgumentException(logger.log(IngestorMessage.COMPONENT_PRODUCT_NOT_FOUND, componentProductId));
				} else if (!allowedComponentClasses.contains(componentProduct.get().getProductClass())) {
					throw new IllegalArgumentException(logger.log(IngestorMessage.COMPONENT_PRODUCT_CLASS_INVALID,
							componentProduct.get().getProductClass().getProductType(), product.getProductClass(),
							product.getMissionCode()));
				} else {
					modelProduct.getComponentProducts().add(componentProduct.get());
				}
			}
		}

		// Add enclosing product
		if (null != product.getEnclosingProductId()) {
			Optional<Product> enclosingProduct = RepositoryService.getProductRepository().findById(product.getEnclosingProductId());
			if (enclosingProduct.isEmpty()) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.ENCLOSING_PRODUCT_NOT_FOUND, product.getEnclosingProductId()));
			} else {
				// Check that the product class of the enclosing product is valid for the
				// product's product class
				Set<ProductClass> allowedEnclosingClasses = new HashSet<>();
				for (SimpleSelectionRule rule : modelProductClass.getSupportedSelectionRules()) {
					allowedEnclosingClasses.add(rule.getTargetProductClass());
				}
				if (!allowedEnclosingClasses.contains(enclosingProduct.get().getProductClass())) {
					throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_CLASS_INVALID,
							enclosingProduct.get().getProductClass().getProductType(), product.getProductClass(),
							product.getMissionCode()));
				}
				// OK - set the enclosing product
				modelProduct.setEnclosingProduct(enclosingProduct.get());
			}
		}

		// Add orbit, if given
		if (null != product.getOrbit()) {
			Orbit orbit = RepositoryService.getOrbitRepository()
				.findByMissionCodeAndSpacecraftCodeAndOrbitNumber(product.getMissionCode(), product.getOrbit().getSpacecraftCode(),
						product.getOrbit().getOrbitNumber().intValue());
			if (null == orbit) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.ORBIT_NOT_FOUND, product.getOrbit().getOrbitNumber(),
						product.getOrbit().getSpacecraftCode()));
			}
			modelProduct.setOrbit(orbit);
		}
		// Check validity of scalar attributes
		if (null != modelProduct.getFileClass()
				&& !modelProductClass.getMission().getFileClasses().contains(modelProduct.getFileClass())) {
			throw new IllegalArgumentException(
					logger.log(IngestorMessage.FILE_CLASS_INVALID, product.getFileClass(), product.getMissionCode()));
		}
		if (null != modelProduct.getMode()
				&& !modelProductClass.getMission().getProcessingModes().contains(modelProduct.getMode())) {
			throw new IllegalArgumentException(
					logger.log(IngestorMessage.MODE_INVALID, product.getMode(), product.getMissionCode()));
		}

		// Add configured processor, if given
		if (null != product.getConfiguredProcessor()) {
			ConfiguredProcessor configuredProcessor = RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(product.getMissionCode(), product.getConfiguredProcessor().getIdentifier());
			if (null == configuredProcessor) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.CONFIGURED_PROCESSOR_NOT_FOUND,
						product.getConfiguredProcessor().getIdentifier()));
			}
			modelProduct.setConfiguredProcessor(configuredProcessor);
		}

		// Everything OK, store new product in database
		modelProduct = RepositoryService.getProductRepository().save(modelProduct);

		logger.log(IngestorMessage.PRODUCT_CREATED, product.getProductClass(), product.getMissionCode());

		return ProductUtil.toRestProduct(modelProduct);
	}

	/**
	 * Find the product with the given ID
	 *
	 * @param id the ID to look for
	 * @return a Json object corresponding to the product found
	 * @throws IllegalArgumentException if no product ID was given
	 * @throws NoResultException        if no product with the given ID exists
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public RestProduct getProductById(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProductById({})", id);

		Product product = readProduct(id);

		logger.log(IngestorMessage.PRODUCT_RETRIEVED, id);

		return ProductUtil.toRestProduct(product);
	}

	/**
	 * Update the product with the given ID with the attribute values of the given
	 * Json object. This method will NOT modify associated product files.
	 *
	 * @param id      the ID of the product to update
	 * @param product a Json object containing the modified (and unmodified)
	 *                attributes
	 * @return a Json object corresponding to the product after modification (with
	 *         ID and version for all contained objects)
	 * @throws EntityNotFoundException         if no product with the given ID
	 *                                         exists
	 * @throws IllegalArgumentException        if any of the input data was invalid
	 * @throws ConcurrentModificationException if the product has been modified
	 *                                         since retrieval by the client
	 * @throws SecurityException               if a cross-mission data access was
	 *                                         attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public RestProduct modifyProduct(Long id, RestProduct product)
			throws EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyProduct({})", id);

		Optional<Product> optModelProduct = RepositoryService.getProductRepository().findById(id);

		if (optModelProduct.isEmpty()) {
			throw new EntityNotFoundException(logger.log(IngestorMessage.PRODUCT_NOT_FOUND, id));
		}
		Product modelProduct = optModelProduct.get();

		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(modelProduct.getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProduct.getProductClass().getMission().getCode(), securityService.getMission()));
		}

		// Make sure we are allowed to change the product (no intermediate update)
		if (modelProduct.getVersion() != product.getVersion().intValue()) {
			throw new ConcurrentModificationException(logger.log(IngestorMessage.CONCURRENT_UPDATE, id));
		}

		// Ensure that mandatory attributes are set
		if (null == product.getProductClass() || product.getProductClass().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "productClass", "product modification"));
		}
		if (null == product.getSensingStartTime() || product.getSensingStartTime().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "sensingStartTime", "product modification"));
		}
		if (null == product.getSensingStopTime() || product.getSensingStopTime().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "sensingStopTime", "product modification"));
		}
		if (null == product.getGenerationTime() || product.getGenerationTime().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "generationTime", "product modification"));
		}

		// If list attributes were explicitly set to null, initialize with empty list to
		// avoid NullPointerExceptions
		if (null == product.getDownloadHistory()) {
			product.setDownloadHistory(new ArrayList<RestDownloadHistory>());
		}
		if (null == product.getComponentProductIds()) {
			product.setComponentProductIds(new ArrayList<Long>());
		}
		if (null == product.getProductFile()) {
			product.setProductFile(new ArrayList<RestProductFile>());
		}
		if (null == product.getParameters()) {
			product.setParameters(new ArrayList<RestParameter>());
		}

		// Update modified attributes
		boolean productChanged = false;
		Product changedProduct = ProductUtil.toModelProduct(product);

		if (!modelProduct.getProductClass().getMission().getCode().equals(product.getMissionCode())
				|| !modelProduct.getProductClass().getProductType().equals(product.getProductClass())) {
			ProductClass modelProductClass = RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(product.getMissionCode(), product.getProductClass());
			if (null == modelProductClass) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_CLASS_INVALID, product.getProductClass()));
			}
			productChanged = true;
			modelProduct.setProductClass(modelProductClass);
		}
		if (!Objects.equals(modelProduct.getFileClass(), changedProduct.getFileClass())) {
			if (null != changedProduct.getFileClass() 
					&& !modelProduct.getProductClass().getMission().getFileClasses().contains(changedProduct.getFileClass())) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.FILE_CLASS_INVALID, product.getFileClass(), product.getMissionCode()));
			}
			productChanged = true;
			if (logger.isTraceEnabled())
				logger.trace("Changing file class from {} to {}", modelProduct.getFileClass(), changedProduct.getFileClass());
			modelProduct.setFileClass(changedProduct.getFileClass());
		}
		if (!Objects.equals(modelProduct.getMode(), changedProduct.getMode())) {
			if (null != changedProduct.getMode()
					&& !modelProduct.getProductClass().getMission().getProcessingModes().contains(changedProduct.getMode())) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.MODE_INVALID, product.getMode(), product.getMissionCode()));
			}
			productChanged = true;
			if (logger.isTraceEnabled())
				logger.trace("Changing mode from {} to {}", modelProduct.getMode(), changedProduct.getMode());
			modelProduct.setMode(changedProduct.getMode());
		}
		if (!modelProduct.getProductQuality().equals(changedProduct.getProductQuality())) {
			productChanged = true;
			modelProduct.setProductQuality(changedProduct.getProductQuality());
		}
		if (!modelProduct.getSensingStartTime().equals(changedProduct.getSensingStartTime())) {
			productChanged = true;
			modelProduct.setSensingStartTime(changedProduct.getSensingStartTime());
		}
		if (!modelProduct.getSensingStopTime().equals(changedProduct.getSensingStopTime())) {
			productChanged = true;
			modelProduct.setSensingStopTime(changedProduct.getSensingStopTime());
		}
		if (null == modelProduct.getRawDataAvailabilityTime() && null != changedProduct.getRawDataAvailabilityTime()
				|| null != modelProduct.getRawDataAvailabilityTime()
						&& !modelProduct.getRawDataAvailabilityTime().equals(changedProduct.getRawDataAvailabilityTime())) {
			productChanged = true;
			modelProduct.setRawDataAvailabilityTime(changedProduct.getRawDataAvailabilityTime());
		}
		if (null == modelProduct.getGenerationTime() && null != changedProduct.getGenerationTime()
				|| null != modelProduct.getGenerationTime()
						&& !modelProduct.getGenerationTime().equals(changedProduct.getGenerationTime())) {
			productChanged = true;
			modelProduct.setGenerationTime(changedProduct.getGenerationTime());
		}
		if (null == modelProduct.getPublicationTime() && null != changedProduct.getPublicationTime()
				|| null != modelProduct.getPublicationTime()
						&& !modelProduct.getPublicationTime().equals(changedProduct.getPublicationTime())) {
			productChanged = true;
			modelProduct.setPublicationTime(changedProduct.getPublicationTime());
		}
		if (null == modelProduct.getEvictionTime() && null != changedProduct.getEvictionTime()
				|| null != modelProduct.getEvictionTime()
						&& !modelProduct.getEvictionTime().equals(changedProduct.getEvictionTime())) {
			productChanged = true;
			modelProduct.setEvictionTime(changedProduct.getEvictionTime());
		}
		if (null == modelProduct.getProductionType() && null != changedProduct.getProductionType()
				|| null != modelProduct.getProductionType()
						&& !modelProduct.getProductionType().equals(changedProduct.getProductionType())) {
			productChanged = true;
			modelProduct.setProductionType(changedProduct.getProductionType());
		}

		// Update orbit relationship
		if (null == modelProduct.getOrbit() && null == product.getOrbit()) {
			// OK - no orbit on both sides
		} else if (null == product.getOrbit()) {
			// Orbit was set, but is no more
			productChanged = true;
			modelProduct.setOrbit(null);
		} else if (null == modelProduct.getOrbit()
				|| !modelProduct.getOrbit().getOrbitNumber().equals(product.getOrbit().getOrbitNumber().intValue())) {
			Orbit orbit = RepositoryService.getOrbitRepository()
				.findByMissionCodeAndSpacecraftCodeAndOrbitNumber(product.getMissionCode(), product.getOrbit().getSpacecraftCode(),
						product.getOrbit().getOrbitNumber().intValue());
			if (null == orbit) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.ORBIT_NOT_FOUND, product.getOrbit().getOrbitNumber(),
						product.getOrbit().getSpacecraftCode()));
			}
			modelProduct.setOrbit(orbit);
		}

		// Update relationship to enclosing product
		if (null == modelProduct.getEnclosingProduct() && null == product.getEnclosingProductId()) {
			// OK - no enclosing product on both sides
		} else if (null == product.getEnclosingProductId()) {
			// Enclosing product was set, but is no more
			productChanged = true;
			Product modelEnclosingProduct = modelProduct.getEnclosingProduct();
			modelEnclosingProduct.getComponentProducts().remove(modelProduct);
			RepositoryService.getProductRepository().save(modelEnclosingProduct);
			modelProduct.setEnclosingProduct(null);
		} else {
			// Enclosing product shall be set, check whether it has been changed
			if (null == modelProduct.getEnclosingProduct() /* new */
					|| modelProduct.getEnclosingProduct().getId() != product.getEnclosingProductId().longValue() /* changed */) {
				Optional<Product> enclosingProduct = RepositoryService.getProductRepository()
					.findById(product.getEnclosingProductId());
				if (enclosingProduct.isEmpty()) {
					throw new IllegalArgumentException(
							logger.log(IngestorMessage.ENCLOSING_PRODUCT_NOT_FOUND, product.getEnclosingProductId()));
				} else {
					// Check that the product class of the enclosing product is valid for the
					// product's product class
					Set<ProductClass> allowedEnclosingClasses = new HashSet<>();
					for (SimpleSelectionRule rule : modelProduct.getProductClass().getSupportedSelectionRules()) {
						allowedEnclosingClasses.add(rule.getTargetProductClass());
					}
					if (!allowedEnclosingClasses.contains(enclosingProduct.get().getProductClass())) {
						throw new IllegalArgumentException(logger.log(IngestorMessage.ENCLOSING_PRODUCT_CLASS_INVALID,
								enclosingProduct.get().getProductClass().getProductType(), product.getProductClass(),
								product.getMissionCode()));
					}
					// OK - set the enclosing product
					productChanged = true;
					if (null != modelProduct.getEnclosingProduct()) {
						// Enclosing product has changed, remove this product from old enclosing product
						Product modelEnclosingProduct = modelProduct.getEnclosingProduct();
						modelEnclosingProduct.getComponentProducts().remove(modelProduct);
						RepositoryService.getProductRepository().save(modelEnclosingProduct);
					}
					// Add this product to new enclosing product
					enclosingProduct.get().getComponentProducts().add(modelProduct);
					RepositoryService.getProductRepository().save(enclosingProduct.get());
					modelProduct.setEnclosingProduct(enclosingProduct.get());
				}
			}
		}

		// Check for added component products
		if (null != product.getComponentProductIds() && !product.getComponentProductIds().isEmpty()) {
			Set<ProductClass> allowedComponentClasses = new HashSet<>();
			for (SimpleSelectionRule rule : modelProduct.getProductClass().getRequiredSelectionRules()) {
				allowedComponentClasses.add(rule.getSourceProductClass());
			}

			ADDED_PRODUCTS: for (Long componentProductId : product.getComponentProductIds()) {
				for (Product modelComponentProduct : modelProduct.getComponentProducts()) {
					if (modelComponentProduct.getId() == componentProductId.longValue()) {
						continue ADDED_PRODUCTS;
					}
				}
				// Fall through, so there is a new component product
				Optional<Product> componentProduct = RepositoryService.getProductRepository().findById(componentProductId);
				if (componentProduct.isEmpty()) {
					throw new IllegalArgumentException(logger.log(IngestorMessage.COMPONENT_PRODUCT_NOT_FOUND, componentProductId));
				} else if (!allowedComponentClasses.contains(componentProduct.get().getProductClass())) {
					throw new IllegalArgumentException(logger.log(IngestorMessage.COMPONENT_PRODUCT_CLASS_INVALID,
							componentProduct.get().getProductClass().getProductType(), product.getProductClass(),
							product.getMissionCode()));
				} else {
					productChanged = true;
					// Set enclosing product for new component product
					componentProduct.get().setEnclosingProduct(modelProduct);
					RepositoryService.getProductRepository().save(componentProduct.get());
					modelProduct.getComponentProducts().add(componentProduct.get());
				}
			}
		}
		// Check for removed component products
		for (Product modelComponentProduct : modelProduct.getComponentProducts()) {
			if (product.getComponentProductIds().contains(modelComponentProduct.getId())) {
				continue;
			}
			productChanged = true;
			// Remove enclosing product from component product
			modelComponentProduct.setEnclosingProduct(null);
			RepositoryService.getProductRepository().save(modelComponentProduct);
			modelProduct.getComponentProducts().remove(modelComponentProduct);
		}

		// Update configured processor relationship
		if (null == modelProduct.getConfiguredProcessor() && null == product.getConfiguredProcessor()) {
			// OK - no configured processor on both sides
		} else if (null == product.getConfiguredProcessor()) {
			// Configured processor was set, but is no more
			productChanged = true;
			modelProduct.setConfiguredProcessor(null);
		} else if (null == modelProduct.getConfiguredProcessor() || !modelProduct.getConfiguredProcessor()
			.getIdentifier()
			.equals(product.getConfiguredProcessor().getIdentifier())) {
			ConfiguredProcessor configuredProcessor = RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(product.getMissionCode(), product.getConfiguredProcessor().getIdentifier());
			if (null == configuredProcessor) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.CONFIGURED_PROCESSOR_NOT_FOUND,
						product.getConfiguredProcessor().getIdentifier()));
			}
			modelProduct.setConfiguredProcessor(configuredProcessor);
		}

		// Check for added or changed parameters
		for (String changedParamKey : changedProduct.getParameters().keySet()) {
			Parameter changedParam = changedProduct.getParameters().get(changedParamKey);
			if (modelProduct.getParameters().containsKey(changedParamKey)) {
				Parameter modelParam = modelProduct.getParameters().get(changedParamKey);
				if (modelParam.equals(changedParam)) {
					continue;
				}
			}
			productChanged = true;
			modelProduct.getParameters().put(changedParamKey, changedParam);
		}
		// Check for removed parameters
		for (String modelParamKey : modelProduct.getParameters().keySet()) {
			if (changedProduct.getParameters().containsKey(modelParamKey)) {
				// If found, must be equal after checking for added/changed parameters
				continue;
			}
			productChanged = true;
			modelProduct.getParameters().remove(modelParamKey);
		}

		// Save product only if anything was actually changed
		if (productChanged) {
			modelProduct.incrementVersion();
			modelProduct = RepositoryService.getProductRepository().save(modelProduct);
			logger.log(IngestorMessage.PRODUCT_MODIFIED, id);
		} else {
			logger.log(IngestorMessage.PRODUCT_NOT_MODIFIED, id);
		}

		return ProductUtil.toRestProduct(modelProduct);
	}

	/**
	 * Find the product with the given universally unique product identifier
	 *
	 * @param uuid the UUID to look for
	 * @return a Json object corresponding to the product found
	 * @throws IllegalArgumentException if no or an invalid product UUID was given
	 * @throws NoResultException        if no product with the given UUID exists
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public RestProduct getProductByUuid(String uuid) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProductByUuid({})", uuid);

		// Check input parameter
		if (null == uuid || 0 == uuid.length()) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_UUID_MISSING));
		}

		UUID uuidToSearch = null;
		try {
			uuidToSearch = UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_UUID_INVALID, uuid));
		}

		// Find the product in the database
		Product product = RepositoryService.getProductRepository().findByUuid(uuidToSearch);
		if (null == product) {
			throw new NoResultException(logger.log(IngestorMessage.PRODUCT_NOT_FOUND_BY_UUID, uuid));
		}

		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(product.getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					product.getProductClass().getMission().getCode(), securityService.getMission()));
		}

		// Ensure product class is visible for user
		ProductVisibility visibility = product.getProductClass().getVisibility();
		switch (visibility) {
		case PUBLIC:
			break;
		case RESTRICTED:
			if (securityService.hasRole(UserRole.PRODUCT_READER_RESTRICTED)) {
				break;
			}
			// Fall through to test READER_ALL
		default: // Internal
			if (securityService.hasRole(UserRole.PRODUCT_READER_ALL)) {
				break;
			}
			// Product not visible for user
			throw new SecurityException(
					logger.log(IngestorMessage.VISIBILITY_VIOLATION, product.getProductClass().getProductType()));
		}

		logger.log(IngestorMessage.PRODUCT_RETRIEVED_BY_UUID, uuid);

		return ProductUtil.toRestProduct(product);
	}

	/**
	 * Create a JPQL query to retrieve the requested set of products
	 *
	 * @param mission       the mission code (will be set to logged in mission, if
	 *                      not given; otherwise must match logged in mission)
	 * @param productClass  an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo   latest sensing start time
	 * @param recordFrom    first record of filtered and ordered result to return
	 * @param recordTo      last record of filtered and ordered result to return
	 * @param jobStepId     get input products of job step
	 * @param orderBy       an array of strings containing a column name and an
	 *                      optional sort direction (ASC/DESC), separated by white
	 *                      space
	 * @return JPQL Query
	 */
	private Query createProductsQuery(String mission, String[] productClass, String mode, String fileClass, String quality,
			String startTimeFrom, String startTimeTo, String genTimeFrom, String genTimeTo, Integer recordFrom, Integer recordTo,
			Long jobStepId, String[] orderBy, Boolean count) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createProductsQuery({}, {}, {}, {}, {}, {}, {}, {}, {})", mission, productClass, startTimeFrom,
					startTimeTo, recordFrom, recordTo, jobStepId, orderBy, count);

		// Find using search parameters
		String jpqlQuery = null;
		String join = "";
		if (jobStepId != null) {
			if (count) {
				jpqlQuery = "select count(p) from ProductQuery pq join pq.satisfyingProducts p " + join
						+ " where pq.jobStep.id = :jobStepId and p.productClass.mission.code = :missionCode";
			} else {
				jpqlQuery = "select p from ProductQuery pq join pq.satisfyingProducts p " + join
						+ " where pq.jobStep.id = :jobStepId and p.productClass.mission.code = :missionCode";
			}
		} else {
			if (count) {
				jpqlQuery = "select count(p) from Product p " + join + " where p.productClass.mission.code = :missionCode";
			} else {
				jpqlQuery = "select p from Product p " + join + " where p.productClass.mission.code = :missionCode";
			}
		}
		if (null != productClass && 0 < productClass.length) {
			jpqlQuery += " and p.productClass.productType in (";
			for (int i = 0; i < productClass.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += ":productClass" + i;
			}
			jpqlQuery += ")";
		}
		if (null != mode) {
			jpqlQuery += " and p.mode = :mode";
		}
		if (null != fileClass) {
			jpqlQuery += " and p.fileClass = :fileClass";
		}
		if (null != quality) {
			jpqlQuery += " and p.productQuality = :quality";
		}
		if (null != startTimeFrom) {
			jpqlQuery += " and p.sensingStartTime >= :startTimeFrom";
		}
		if (null != startTimeTo) {
			jpqlQuery += " and p.sensingStartTime <= :startTimeTo";
		}
		if (null != genTimeFrom) {
			jpqlQuery += " and p.generationTime >= :genTimeFrom";
		}
		if (null != genTimeTo) {
			jpqlQuery += " and p.generationTime <= :genTimeTo";
		}
		// visibility
		List<ProductVisibility> visibilities = new ArrayList<>();
		visibilities.add(ProductVisibility.PUBLIC);
		if (securityService.hasRole(UserRole.PRODUCT_READER_RESTRICTED) || securityService.hasRole(UserRole.PRODUCT_READER_ALL)) {
			visibilities.add(ProductVisibility.RESTRICTED);
		}
		if (securityService.hasRole(UserRole.PRODUCT_READER_ALL)) {
			visibilities.add(ProductVisibility.INTERNAL);
		}
		if (0 < visibilities.size()) {
			jpqlQuery += " and p.productClass.visibility in (";
			for (int i = 0; i < visibilities.size(); ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += ":visibility" + i;
			}
			jpqlQuery += ")";
		}

		// order by
		if (null != orderBy && 0 < orderBy.length) {
			jpqlQuery += " order by ";
			for (int i = 0; i < orderBy.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += "p.";
				jpqlQuery += orderBy[i];
			}
		}

		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", mission);
		if (null != productClass && 0 < productClass.length) {
			for (int i = 0; i < productClass.length; ++i) {
				query.setParameter("productClass" + i, productClass[i]);
			}
		}
		if (null != mode) {
			query.setParameter("mode", mode);
		}
		if (null != fileClass) {
			query.setParameter("fileClass", fileClass);
		}
		if (null != quality) {
			query.setParameter("quality", ProductQuality.valueOf(quality));
		}
		if (null != startTimeFrom) {
			query.setParameter("startTimeFrom", OrbitTimeFormatter.parseDateTime(startTimeFrom));
		}
		if (null != startTimeTo) {
			query.setParameter("startTimeTo", OrbitTimeFormatter.parseDateTime(startTimeTo));
		}
		if (null != genTimeFrom) {
			query.setParameter("genTimeFrom", OrbitTimeFormatter.parseDateTime(genTimeFrom));
		}
		if (null != genTimeTo) {
			query.setParameter("genTimeTo", OrbitTimeFormatter.parseDateTime(genTimeTo));
		}
		if (0 < visibilities.size()) {
			for (int i = 0; i < visibilities.size(); ++i) {
				query.setParameter("visibility" + i, visibilities.get(i));
			}
		}

		if (jobStepId != null) {
			query.setParameter("jobStepId", jobStepId);
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

	/**
	 * Get the primary data file (or ZIP file, if available) for the product as data
	 * stream (optionally range-restricted), returns a redirection link to the
	 * Storage Manager of a random Processing Facility
	 *
	 * @param id       the ID of the product to download
	 * @param fromByte the first byte of the data stream to download (optional,
	 *                 default is file start, i.e. byte 0)
	 * @param toByte   the last byte of the data stream to download (optional,
	 *                 default is file end, i.e. file size - 1)
	 * @return a redirect URL in the HTTP Location header
	 * @throws IllegalArgumentException if no product ID was given
	 * @throws NoResultException        if no product with the given ID exists or if
	 *                                  it does not have a data file
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public String downloadProductById(Long id, Long fromByte, Long toByte)
			throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadProductById({}, {}, {})", id, fromByte, toByte);

		Product product = readProduct(id);

		// Check whether the product is actually available on some processing facility
		if (product.getProductFile().isEmpty()) {
			throw new NoResultException(logger.log(IngestorMessage.PRODUCT_NOT_AVAILABLE, id));
		}

		// Select the first product file to transfer (they should be identical anyway)
		ProductFile productFile = product.getProductFile().iterator().next();
		String fileName = (null == productFile.getZipFileName() ? productFile.getProductFileName() : productFile.getZipFileName());

		// Get the service URI of the Storage Manager service
		String storageManagerUrl = productFile.getProcessingFacility().getStorageManagerUrl();

		// Get a new download token
		String downloadToken = createDownloadToken(fileName);

		// Build the download URI: Set pathInfo to zipped file if available, to product
		// file otherwise
		URIBuilder uriBuilder = null;
		try {
			uriBuilder = new URIBuilder(storageManagerUrl + "/products/download");
			uriBuilder.addParameter("pathInfo", productFile.getFilePath() + "/" + fileName);
			if (null != fromByte) {
				uriBuilder.addParameter("fromByte", fromByte.toString());
			}
			if (null != toByte) {
				uriBuilder.addParameter("toByte", toByte.toString());
			}
			uriBuilder.addParameter("token", downloadToken);
		} catch (URISyntaxException e) {
			if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
			throw new RuntimeException(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e));
		}

		logger.log(IngestorMessage.PRODUCT_DOWNLOAD_REQUESTED, id);

		return uriBuilder.toString();
	}

	/**
	 * Create a signed JSON Web Token for the given file name using the secret
	 * shared with the Storage Manager (See
	 * https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac)
	 *
	 * @param fileName the file name to create the token for
	 * @return the signed JSON Web Token (JWS) as per RFC 7515 and RFC 7519
	 */
	private String createDownloadToken(String fileName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createDownloadToken({})", fileName);

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();

		JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(fileName)
			.expirationTime(new Date(new Date().getTime() + ingestorConfig.getStorageManagerTokenValidity()))
			.build();

		JWSSigner signer = null;
		try {
			// We need exactly 256 bits (32 bytes) of key length, so a shorter key will be
			// filled with blanks, a longer key will be truncated
			signer = new MACSigner(ingestorConfig.getStorageManagerSecret());
		} catch (KeyLengthException e) {
			throw new RuntimeException(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e));
		}

		SignedJWT signedJWT = new SignedJWT(header, claims);
		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			throw new RuntimeException(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e));
		}

		return signedJWT.serialize();
	}

	/**
	 * Get a JSON Web Token for creating a download link to a Storage Manager
	 *
	 * @param id       the ID of the product to download
	 * @param fileName the name of the file to download (default primary data file
	 *                 or ZIP file, if available)
	 * @return the signed JSON Web Token (JWS) as per RFC 7515 and RFC 7519
	 * @throws IllegalArgumentException if no product ID was given
	 * @throws NoResultException        if no product with the given ID or no file
	 *                                  with the given name exists
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public String getDownloadTokenById(Long id, String fileName)
			throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getDownloadTokenById({}, {})", id, fileName);

		Product product = readProduct(id);

		// Check whether the product is actually available on some processing facility
		if (product.getProductFile().isEmpty()) {
			throw new NoResultException(logger.log(IngestorMessage.PRODUCT_NOT_AVAILABLE, id));
		}

		// Check the file name
		if (null == fileName) {
			// Select the first product file to transfer (they should be identical anyway)
			ProductFile productFile = product.getProductFile().iterator().next();
			fileName = (null == productFile.getZipFileName() ? productFile.getProductFileName() : productFile.getZipFileName());
		} else {
			// Check whether any of the product files has a data, ZIP or auxiliary file of
			// that name
			boolean found = false;
			for (ProductFile productFile : product.getProductFile()) {
				if (fileName.equals(productFile.getProductFileName()) || fileName.equals(productFile.getZipFileName())
						|| productFile.getAuxFileNames().contains(fileName)) {
					found = true;

					// Create download history entry
					DownloadHistory historyEntry = new DownloadHistory();
					historyEntry.setProductFile(productFile);
					historyEntry.setProductFileName(productFile.getProductFileName());
					historyEntry.setProductFileSize(productFile.getFileSize());
					historyEntry.setUsername(securityService.getUser());
					historyEntry.setDateTime(Instant.now());

					product.getDownloadHistory().add(historyEntry);
				}
			}
			if (!found) {
				throw new NoResultException(logger.log(IngestorMessage.PRODUCTFILE_NOT_AVAILABLE, id, fileName));
			}
		}
		// Get a new download token
		String downloadToken = createDownloadToken(fileName);

		logger.log(IngestorMessage.PRODUCT_DOWNLOAD_TOKEN_REQUESTED, id, fileName);

		return downloadToken;
	}

}
