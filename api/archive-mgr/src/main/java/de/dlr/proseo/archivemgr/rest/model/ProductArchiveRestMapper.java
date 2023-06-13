/**
 * ProductArchiveUtil.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest.model;

import java.util.HashSet;
import java.util.Set;

import de.dlr.proseo.archivemgr.utils.StringUtils;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProductArchiveMgrMessage;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.dao.ProductClassRepository;
import de.dlr.proseo.model.enums.ArchiveType;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Rest - Model Mapper for Product Archive
 * 
 * @author Denys Chaykovskiy
 */

public class ProductArchiveRestMapper {
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveRestMapper.class);
	
	/** Rest Archive as input parameter */
	private RestProductArchive restArchive;  
	
	/** Product Archive as output parameter */
	private ProductArchive modelArchive = new ProductArchive();
	
	/**
	 * Constructor with restArchive parameter
	 * 
	 * @param restArchive rest archive
	 */
	public ProductArchiveRestMapper(RestProductArchive restArchive) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> Constructor({})", (null == restArchive ? "MISSING" : restArchive.getId()));

		if (null == restArchive) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_MISSING));
		}
		
		this.restArchive = restArchive; 
		
		setDefaultValuesIfNull();
		
		checkMandatoryFields();
	}
	
	/**
	 * Gets checked in constructor rest product archive
	 * 
	 * @return rest product archive
	 */
	public RestProductArchive get() {
		
		return restArchive;
	}
						
	/**
	 * Convert a REST ProductArchive into a prosEO model ProductArchive 
	 * AvailableProductClasses will not be set here, the field will be null
	 * 
	 * @return model Product Archive
	 * @throws IllegalArgumentException if the REST Product Archive has a wrong archive type
	 */
	public ProductArchive toModel() throws IllegalArgumentException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toModel({})", (null == restArchive ? "MISSING" : restArchive.getId()));
			
		try {
			modelArchive.setArchiveType(ArchiveType.valueOf(restArchive.getArchiveType()));	
	    } catch (IllegalArgumentException e) {	    	 
	    	throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_TYPE_WRONG));	    	 
	    }
		
		setAvailableProductClasses();
		
		modelArchive.setBaseUri(restArchive.getBaseUri());		
		modelArchive.setClientId(restArchive.getClientId());
		modelArchive.setClientSecret(restArchive.getClientSecret());
		
		modelArchive.setCode(restArchive.getCode());
		modelArchive.setContext(restArchive.getContext());
		modelArchive.setId(restArchive.getId());
		
		modelArchive.setName(restArchive.getName());
		modelArchive.setPassword(restArchive.getPassword());	
		modelArchive.setSendAuthInBody(restArchive.getSendAuthInBody());
		
		modelArchive.setTokenRequired(restArchive.getTokenRequired());
		modelArchive.setTokenUri(restArchive.getTokenUri());
		modelArchive.setUsername(restArchive.getUsername());
			
		return modelArchive;
	}

	/**
	 * Sets Default values of the product archive 
	 * 
	 */
	private void setDefaultValuesIfNull() {
		
		if (null == restArchive.getArchiveType()) {
			restArchive.setArchiveType(ArchiveType.AIP.toString());
		}
		
		if (null == restArchive.getTokenRequired()) {
			restArchive.setTokenRequired(false);
		}
		
		if (null == restArchive.getSendAuthInBody()) {
			restArchive.setSendAuthInBody(false);
		}
	}
	
	/**
	 * Checks mandatory fields in product archive
	 * 
	 */
	private void checkMandatoryFields() {
		
		if (StringUtils.isNullOrBlank(restArchive.getCode())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Code", "product archive model checker"));
		}
		
		if (StringUtils.isNullOrBlank(restArchive.getName())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Name", "product archive model checker"));
		}
		
		if (null == restArchive.getArchiveType()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "ArchiveType", "product archive model checker"));
		}
		
		if (StringUtils.isNullOrBlank(restArchive.getBaseUri())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "BaseUri", "product archive model checker"));
		}
		
		if (StringUtils.isNullOrBlank(restArchive.getContext())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Context", "product archive model checker"));
		}
		
		if (restArchive.getTokenRequired()) {
			if (StringUtils.isNullOrBlank(restArchive.getTokenUri())) { 
				throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "TokenUri", "product archive model checker"));
			}
		}
		
		if (restArchive.getSendAuthInBody()) {
			if (StringUtils.isNullOrBlank(restArchive.getUsername())) {
				throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Username", "product archive model checker"));
			}
			if (null == restArchive.getPassword()) {
				throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Password", "product archive model checker"));
			}
		}
	}
		
	/**
	 * Sets available product classes of the product archive model
	 * 
	 */
	private void setAvailableProductClasses()
	{
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
	}
}
