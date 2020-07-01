/**
 * ProductManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;

/**
 * Service methods required to create, modify and delete products in the prosEO database,
 * and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional
public class ProductManager {
	
	/* Message ID constants */
	private static final int MSG_ID_PRODUCT_MISSING = 2000;
	private static final int MSG_ID_PRODUCT_NOT_FOUND = 2001;
	private static final int MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND = 2002;
	private static final int MSG_ID_COMPONENT_PRODUCT_NOT_FOUND = 2003;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2004;
	private static final int MSG_ID_PRODUCT_DELETED = 2005;
	private static final int MSG_ID_PRODUCT_LIST_RETRIEVED = 2006;
	private static final int MSG_ID_PRODUCT_CREATED = 2007;
	private static final int MSG_ID_PRODUCT_RETRIEVED = 2008;
	private static final int MSG_ID_PRODUCT_MODIFIED = 2009;
	private static final int MSG_ID_PRODUCT_NOT_MODIFIED = 2010;
	private static final int MSG_ID_PRODUCT_LIST_EMPTY = 2011;
	private static final int MSG_ID_MISSION_OR_PRODUCT_CLASS_INVALID = 2012;
	private static final int MSG_ID_PRODUCT_ID_MISSING = 2013;
	private static final int MSG_ID_FILE_CLASS_INVALID = 2014;
	private static final int MSG_ID_MODE_INVALID = 2015;
	private static final int MSG_ID_CONCURRENT_UPDATE = 2016;
	private static final int MSG_ID_COMPONENT_PRODUCT_CLASS_INVALID = 2017;
	private static final int MSG_ID_ENCLOSING_PRODUCT_CLASS_INVALID = 2018;
	private static final int MSG_ID_ORBIT_NOT_FOUND = 2019;
	private static final int MSG_ID_PRODUCT_UUID_MISSING = 2020;
	private static final int MSG_ID_PRODUCT_UUID_INVALID = 2021;
	private static final int MSG_ID_PRODUCT_NOT_FOUND_BY_UUID = 2022;
	private static final int MSG_ID_PRODUCT_RETRIEVED_BY_UUID = 2023;
	private static final int MSG_ID_DUPLICATE_PRODUCT_UUID = 2024;
	private static final int MSG_ID_CONFIGURED_PROCESSOR_NOT_FOUND = 2025;
	private static final int MSG_ID_PRODUCT_HAS_FILES = 2026;
//	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;	
	
	/* Message string constants */
	private static final String MSG_PRODUCT_MISSING = "(E%d) Product not set";
	private static final String MSG_PRODUCT_ID_MISSING = "(E%d) Product ID not set";
	private static final String MSG_PRODUCT_NOT_FOUND = "(E%d) No product found for ID %d";
	private static final String MSG_PRODUCT_LIST_EMPTY = "(E%d) No products found for search criteria";
	private static final String MSG_MISSION_OR_PRODUCT_CLASS_INVALID = "(E%d) Mission code %s and/or product type %s invalid";
	private static final String MSG_ENCLOSING_PRODUCT_NOT_FOUND = "(E%d) Enclosing product with ID %d not found";
	private static final String MSG_COMPONENT_PRODUCT_NOT_FOUND = "(E%d) Component product with ID %d not found";
	private static final String MSG_ORBIT_NOT_FOUND = "(E%d) Orbit %d for spacecraft %s not found";
	private static final String MSG_FILE_CLASS_INVALID = "(E%d) File class %s invalid for mission %s";
	private static final String MSG_MODE_INVALID = "(E%d) Processing mode %s invalid for mission %s";
	private static final String MSG_COMPONENT_PRODUCT_CLASS_INVALID = "(E%d) Component product class %s invalid for product class %s in mission %s";
	private static final String MSG_ENCLOSING_PRODUCT_CLASS_INVALID = "(E%d) Enclosing product class %s invalid for product class %s in mission %s";
	private static final String MSG_CONCURRENT_UPDATE = "(E%d) The product with ID %d has been modified since retrieval by the client";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Product deletion unsuccessful for ID %d";
	private static final String MSG_PRODUCT_UUID_MISSING = "(E%d) Product UUID not set";
	private static final String MSG_PRODUCT_UUID_INVALID = "(E%d) Product UUID %s invalid";
	private static final String MSG_PRODUCT_NOT_FOUND_BY_UUID = "(E%d) No product found for UUID %s";
	private static final String MSG_DUPLICATE_PRODUCT_UUID = "(E%d) Duplicate product UUID %s";
	private static final String MSG_CONFIGURED_PROCESSOR_NOT_FOUND = "(E%d) Configured processor %s not found";
	private static final String MSG_PRODUCT_HAS_FILES = "(E%d) Product with ID %d has existing files and cannot be deleted";
	
	private static final String MSG_PRODUCT_DELETED = "(I%d) Product with id %d deleted";
	private static final String MSG_PRODUCT_LIST_RETRIEVED = "(I%d) Product list of size %d retrieved for mission '%s', product classes '%s', start time '%s', stop time '%s'";
	private static final String MSG_PRODUCT_CREATED = "(I%d) Product of type %s created for mission %s";
	private static final String MSG_PRODUCT_RETRIEVED_BY_UUID = "(I%d) Product with UUID %s retrieved";
	private static final String MSG_PRODUCT_MODIFIED = "(I%d) Product with id %d modified";
	private static final String MSG_PRODUCT_NOT_MODIFIED = "(I%d) Product with id %d not modified (no changes)";
	private static final String MSG_PRODUCT_RETRIEVED = "(I%d) Product with UUID %s retrieved";
	
	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductManager.class);
	
	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
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
	
	/**
	 * Delete a product by ID
	 * 
	 * @param the ID of the product to delete
	 * @throws EntityNotFoundException if the product to delete does not exist in the database
	 * @throws IllegalStateException if the product to delete still as files at some Processing Facility
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteProductById(Long id) throws EntityNotFoundException, IllegalStateException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductById({})", id);
		
		// Test whether the product id is valid
		Optional<Product> modelProduct = RepositoryService.getProductRepository().findById(id);
		if (modelProduct.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND, id));
		}
		
		// Make sure product does not exist on any Processing Facility
		if (!modelProduct.get().getProductFile().isEmpty()) {
			throw new IllegalStateException(logError(MSG_PRODUCT_HAS_FILES, MSG_ID_PRODUCT_HAS_FILES, modelProduct.get().getId()));
		}
		
		// Delete the product
		RepositoryService.getProductRepository().deleteById(id);

		// Test whether the deletion was successful
		modelProduct = RepositoryService.getProductRepository().findById(id);
		if (!modelProduct.isEmpty()) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
		}
		
		logInfo(MSG_PRODUCT_DELETED, MSG_ID_PRODUCT_DELETED, id);
	}

	/**
	 * List of all products filtered by mission, product class, start time range
	 * 
	 * @param mission the mission code
	 * @param productClass an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @return a list of products
	 * @throws NoResultException if no products matching the given search criteria could be found
	 */
	public List<RestProduct> getProducts(String mission, String[] productClass,
			Date startTimeFrom, Date startTimeTo) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProducts({}, {}, {}, {})", mission, productClass, startTimeFrom, startTimeTo);
		
		List<RestProduct> result = new ArrayList<>();
		
		if (null == mission && (null == productClass || 0 == productClass.length) && null == startTimeFrom && null == startTimeTo) {
			// Simple case: no search criteria set
			for (Product product: RepositoryService.getProductRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found product with ID {}", product.getId());
				RestProduct resultProduct = ProductUtil.toRestProduct(product);
				if (logger.isDebugEnabled()) logger.debug("Created result product with ID {}", resultProduct.getId());

				result.add(resultProduct);
			}
		} else {
			// Find using search parameters
			String jpqlQuery = "select p from Product p where 1 = 1";
			if (null != mission) {
				jpqlQuery += " and p.productClass.mission.code = :missionCode";
			}
			if (null != productClass && 0 < productClass.length) {
				jpqlQuery += " and p.productClass.productType in (";
				for (int i = 0; i < productClass.length; ++i) {
					if (0 < i) jpqlQuery += ", ";
					jpqlQuery += ":productClass" + i;
				}
				jpqlQuery += ")";
			}
			if (null != startTimeFrom) {
				jpqlQuery += " and p.sensingStartTime >= :startTimeFrom";
			}
			if (null != startTimeTo) {
				jpqlQuery += " and p.sensingStartTime <= :startTimeTo";
			}
			Query query = em.createQuery(jpqlQuery);
			if (null != mission) {
				query.setParameter("missionCode", mission);
			}
			if (null != productClass && 0 < productClass.length) {
				for (int i = 0; i < productClass.length; ++i) {
					query.setParameter("productClass" + i, productClass[i]);
				}
			}
			if (null != startTimeFrom) {
				query.setParameter("startTimeFrom",startTimeFrom.toInstant());
			}
			if (null != startTimeTo) {
				query.setParameter("startTimeTo", startTimeTo.toInstant());
			}
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof Product) {
					result.add(ProductUtil.toRestProduct((Product) resultObject));
				}
			}
		}
		
		if (result.isEmpty()) {
			throw new NoResultException(logError(MSG_PRODUCT_LIST_EMPTY, MSG_ID_PRODUCT_LIST_EMPTY));
		}
		
		logInfo(MSG_PRODUCT_LIST_RETRIEVED, MSG_ID_PRODUCT_LIST_RETRIEVED, result.size(), mission, productClass, startTimeFrom, startTimeTo);
		
		return result;
	}

	/**
	 * Create a product from the given Json object (does NOT create associated product files!)
	 * 
	 * @param product the Json object to create the product from
	 * @return a Json object corresponding to the product after persistence (with ID and version for all contained objects)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestProduct createProduct(RestProduct product) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createProduct({})", (null == product ? "MISSING" : product.getProductClass()));
		
		if (null == product) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_MISSING, MSG_ID_PRODUCT_MISSING));
		}

		// Create a database model product
		Product modelProduct = ProductUtil.toModelProduct(product);
		if (null == modelProduct.getUuid()) {
			modelProduct.setUuid(UUID.randomUUID());
		} else {
			// Test if given UUID is not yet in use
			if (null != RepositoryService.getProductRepository().findByUuid(modelProduct.getUuid())) {
				throw new IllegalArgumentException(logError(MSG_DUPLICATE_PRODUCT_UUID, MSG_ID_DUPLICATE_PRODUCT_UUID, 
						product.getUuid()));
			}
		}
		
		// Add product class
		ProductClass modelProductClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
				product.getMissionCode(), product.getProductClass());
		if (null == modelProductClass) {
			throw new IllegalArgumentException(logError(MSG_MISSION_OR_PRODUCT_CLASS_INVALID, MSG_ID_MISSION_OR_PRODUCT_CLASS_INVALID, 
					product.getMissionCode(), product.getProductClass()));
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
					throw new IllegalArgumentException(logError(MSG_COMPONENT_PRODUCT_NOT_FOUND, MSG_ID_COMPONENT_PRODUCT_NOT_FOUND,
							componentProductId));
				} else if (!allowedComponentClasses.contains(componentProduct.get().getProductClass())) {
					throw new IllegalArgumentException(logError(MSG_COMPONENT_PRODUCT_CLASS_INVALID, MSG_ID_COMPONENT_PRODUCT_CLASS_INVALID,
							componentProduct.get().getProductClass().getProductType(), product.getProductClass(), product.getMissionCode()));
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
						logError(MSG_ENCLOSING_PRODUCT_NOT_FOUND, MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND, product.getEnclosingProductId()));
			} else {
				// Check that the product class of the enclosing product is valid for the product's product class
				Set<ProductClass> allowedEnclosingClasses = new HashSet<>();
				for (SimpleSelectionRule rule : modelProductClass.getSupportedSelectionRules()) {
					allowedEnclosingClasses.add(rule.getTargetProductClass());
				}
				if (!allowedEnclosingClasses.contains(enclosingProduct.get().getProductClass())) {
					throw new IllegalArgumentException(logError(MSG_ENCLOSING_PRODUCT_CLASS_INVALID, MSG_ID_ENCLOSING_PRODUCT_CLASS_INVALID,
							enclosingProduct.get().getProductClass().getProductType(), product.getProductClass(), product.getMissionCode()));
				}
				// OK - set the enclosing product
				modelProduct.setEnclosingProduct(enclosingProduct.get());
			} 
		}

		// Add orbit, if given
		if (null != product.getOrbit()) {
			Orbit orbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(
					product.getOrbit().getSpacecraftCode(), product.getOrbit().getOrbitNumber().intValue());
			if (null == orbit) {
				throw new IllegalArgumentException(logError(MSG_ORBIT_NOT_FOUND, MSG_ID_ORBIT_NOT_FOUND,
						product.getOrbit().getOrbitNumber(), product.getOrbit().getSpacecraftCode()));
			}
			modelProduct.setOrbit(orbit);
		}
		// Check validity of scalar attributes
		if (!modelProductClass.getMission().getFileClasses().contains(modelProduct.getFileClass())) {
			throw new IllegalArgumentException(logError(MSG_FILE_CLASS_INVALID, MSG_ID_FILE_CLASS_INVALID, 
					product.getFileClass(), product.getMissionCode()));
		}
		if (null != modelProduct.getMode() && !modelProductClass.getMission().getProcessingModes().contains(modelProduct.getMode())) {
			throw new IllegalArgumentException(logError(MSG_MODE_INVALID, MSG_ID_MODE_INVALID,
					product.getMode(), product.getMissionCode()));
		}
		
		// Add configured processor, if given
		if (null != product.getConfiguredProcessor()) {
			ConfiguredProcessor configuredProcessor = RepositoryService.getConfiguredProcessorRepository()
					.findByIdentifier(product.getConfiguredProcessor().getIdentifier());
			if (null == configuredProcessor) {
				throw new IllegalArgumentException(logError(MSG_CONFIGURED_PROCESSOR_NOT_FOUND, MSG_ID_CONFIGURED_PROCESSOR_NOT_FOUND,
						product.getConfiguredProcessor().getIdentifier()));
			}
			modelProduct.setConfiguredProcessor(configuredProcessor);
		}
		
		// Everything OK, store new product in database
		modelProduct = RepositoryService.getProductRepository().save(modelProduct);
		
		logInfo(MSG_PRODUCT_CREATED, MSG_ID_PRODUCT_CREATED, product.getProductClass(), product.getMissionCode());
		
		return ProductUtil.toRestProduct(modelProduct);
	}

	/**
	 * Find the product with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the product found
	 * @throws IllegalArgumentException if no product ID was given
	 * @throws NoResultException if no product with the given ID exists
	 */
	public RestProduct getProductById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductById({})", id);
		
		if (null == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_ID_MISSING, MSG_ID_PRODUCT_ID_MISSING, id));
		}
		
		Optional<Product> modelProduct = RepositoryService.getProductRepository().findById(id);
		
		if (modelProduct.isEmpty()) {
			throw new NoResultException(logError(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND, id));
		}
		
		logInfo(MSG_PRODUCT_RETRIEVED, MSG_ID_PRODUCT_RETRIEVED, id);
		
		return ProductUtil.toRestProduct(modelProduct.get());
	}

	/**
	 * Update the product with the given ID with the attribute values of the given Json object. This method will NOT modify
	 * associated product files.
	 * 
	 * @param id the ID of the product to update
	 * @param product a Json object containing the modified (and unmodified) attributes
	 * @return a Json object corresponding to the product after modification (with ID and version for all contained objects)
	 * @throws EntityNotFoundException if no product with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws ConcurrentModificationException if the product has been modified since retrieval by the client
	 */
	public RestProduct modifyProduct(Long id, RestProduct product) throws
				EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProduct({})", id);
		
		Optional<Product> optModelProduct = RepositoryService.getProductRepository().findById(id);
		
		if (optModelProduct.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND, id));
		}
		Product modelProduct = optModelProduct.get();
		
		// Make sure we are allowed to change the product (no intermediate update)
		if (modelProduct.getVersion() != product.getVersion().intValue()) {
			throw new ConcurrentModificationException(logError(MSG_CONCURRENT_UPDATE, MSG_ID_CONCURRENT_UPDATE, id));
		}
		
		// Update modified attributes
		boolean productChanged = false;
		Product changedProduct = ProductUtil.toModelProduct(product);
		
		if (!modelProduct.getProductClass().getMission().getCode().equals(product.getMissionCode())
			|| !modelProduct.getProductClass().getProductType().equals(product.getProductClass())) {
			ProductClass modelProductClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
					product.getMissionCode(), product.getProductClass());
			if (null == modelProductClass) {
				throw new IllegalArgumentException(logError(MSG_MISSION_OR_PRODUCT_CLASS_INVALID, MSG_ID_MISSION_OR_PRODUCT_CLASS_INVALID, 
						product.getMissionCode(), product.getProductClass()));
			}
			productChanged = true;
			modelProduct.setProductClass(modelProductClass);
		}
		if (!modelProduct.getFileClass().equals(changedProduct.getFileClass())) {
			if (!modelProduct.getProductClass().getMission().getFileClasses().contains(modelProduct.getFileClass())) {
				throw new IllegalArgumentException(logError(MSG_FILE_CLASS_INVALID, MSG_ID_FILE_CLASS_INVALID, 
						product.getFileClass(), product.getMissionCode()));
			}
			productChanged = true;
			if (logger.isTraceEnabled()) logger.trace("Changing file class from {} to {}", modelProduct.getFileClass(), changedProduct.getFileClass());
			modelProduct.setFileClass(changedProduct.getFileClass());
		}
		if ((null != modelProduct.getMode() && !modelProduct.getMode().equals(changedProduct.getMode())) 
				|| (null == modelProduct.getMode() && null != changedProduct.getMode()) ) {
			if (null != changedProduct.getMode() && !modelProduct.getProductClass().getMission().getProcessingModes().contains(changedProduct.getMode())) {
				throw new IllegalArgumentException(logError(MSG_MODE_INVALID, MSG_ID_MODE_INVALID, 
						product.getMode(), product.getMissionCode()));
			}
			productChanged = true;
			if (logger.isTraceEnabled()) logger.trace("Changing mode from {} to {}", modelProduct.getMode(), changedProduct.getMode());
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
		if (!modelProduct.getGenerationTime().equals(changedProduct.getGenerationTime())) {
			productChanged = true;
			modelProduct.setGenerationTime(changedProduct.getGenerationTime());
		}
		if (!modelProduct.getProductionType().equals(changedProduct.getProductionType())) {
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
			Orbit orbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(
					product.getOrbit().getSpacecraftCode(), product.getOrbit().getOrbitNumber().intValue());
			if (null == orbit) {
				throw new IllegalArgumentException(logError(MSG_ORBIT_NOT_FOUND, MSG_ID_ORBIT_NOT_FOUND,
						product.getOrbit().getOrbitNumber(), product.getOrbit().getSpacecraftCode()));
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
				Optional<Product> enclosingProduct = RepositoryService.getProductRepository().findById(product.getEnclosingProductId());
				if (enclosingProduct.isEmpty()) {
					throw new IllegalArgumentException(logError(MSG_ENCLOSING_PRODUCT_NOT_FOUND, MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND, 
							product.getEnclosingProductId()));
				} else {
					// Check that the product class of the enclosing product is valid for the product's product class
					Set<ProductClass> allowedEnclosingClasses = new HashSet<>();
					for (SimpleSelectionRule rule : modelProduct.getProductClass().getSupportedSelectionRules()) {
						allowedEnclosingClasses.add(rule.getTargetProductClass());
					}
					if (!allowedEnclosingClasses.contains(enclosingProduct.get().getProductClass())) {
						throw new IllegalArgumentException(logError(MSG_ENCLOSING_PRODUCT_CLASS_INVALID, MSG_ID_ENCLOSING_PRODUCT_CLASS_INVALID,
								enclosingProduct.get().getProductClass().getProductType(), product.getProductClass(), product.getMissionCode()));
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
					throw new IllegalArgumentException(logError(MSG_COMPONENT_PRODUCT_NOT_FOUND, MSG_ID_COMPONENT_PRODUCT_NOT_FOUND,
							componentProductId));
				} else if (!allowedComponentClasses.contains(componentProduct.get().getProductClass())) {
					throw new IllegalArgumentException(logError(MSG_COMPONENT_PRODUCT_CLASS_INVALID, MSG_ID_COMPONENT_PRODUCT_CLASS_INVALID,
							componentProduct.get().getProductClass().getProductType(), product.getProductClass(), product.getMissionCode()));
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
		for (Product modelComponentProduct: modelProduct.getComponentProducts()) {
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
		} else if (null == modelProduct.getConfiguredProcessor() 
				|| !modelProduct.getConfiguredProcessor().getIdentifier().equals(product.getConfiguredProcessor().getIdentifier())) {
			ConfiguredProcessor configuredProcessor = RepositoryService.getConfiguredProcessorRepository()
					.findByIdentifier(product.getConfiguredProcessor().getIdentifier());
			if (null == configuredProcessor) {
				throw new IllegalArgumentException(logError(MSG_CONFIGURED_PROCESSOR_NOT_FOUND, MSG_ID_CONFIGURED_PROCESSOR_NOT_FOUND,
						product.getConfiguredProcessor().getIdentifier()));
			}
			modelProduct.setConfiguredProcessor(configuredProcessor);
		}
		
		// Check for added or changed parameters
		for (String changedParamKey: changedProduct.getParameters().keySet()) {
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
		for (String modelParamKey: modelProduct.getParameters().keySet()) {
			if (changedProduct.getParameters().containsKey(modelParamKey)) {
				// If found, must be equal after checking for added/changed parameters
				continue;
			}
			productChanged = true;
			modelProduct.getParameters().remove(modelParamKey);
		}
		
		// Save product only if anything was actually changed
		if (productChanged)	{
			modelProduct.incrementVersion();
			modelProduct = RepositoryService.getProductRepository().save(modelProduct);
			logInfo(MSG_PRODUCT_MODIFIED, MSG_ID_PRODUCT_MODIFIED, id);
		} else {
			logInfo(MSG_PRODUCT_NOT_MODIFIED, MSG_ID_PRODUCT_NOT_MODIFIED, id);
		}
		
		return ProductUtil.toRestProduct(modelProduct);
	}
	
	/**
	 * Find the product with the given universally unique product identifier
	 * 
	 * @param uuid the UUID to look for
	 * @return a Json object corresponding to the product found
	 * @throws IllegalArgumentException if no or an invalid product UUID was given
	 * @throws NoResultException if no product with the given UUID exists
	 */
	public RestProduct getProductByUuid(String uuid) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductByUuid({})", uuid);
		
		// Check input parameter
		if (null == uuid || 0 == uuid.length()) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_UUID_MISSING, MSG_ID_PRODUCT_UUID_MISSING));
		}

		UUID uuidToSearch = null;
		try {
			uuidToSearch = UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_UUID_INVALID, MSG_ID_PRODUCT_UUID_INVALID, uuid));
		}
		
		// Find the product in the database
		Product product = RepositoryService.getProductRepository().findByUuid(uuidToSearch);
		if (null == product) {
			throw new NoResultException(logError(MSG_PRODUCT_NOT_FOUND_BY_UUID, MSG_ID_PRODUCT_NOT_FOUND_BY_UUID, uuid));
		}

		logInfo(MSG_PRODUCT_RETRIEVED_BY_UUID, MSG_ID_PRODUCT_RETRIEVED_BY_UUID, uuid);
		
		return ProductUtil.toRestProduct(product);
	}
}
