/**
 * ProductArchiveUtil.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest.model;

import de.dlr.proseo.archivemgr.utils.StringUtils;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProductArchiveMgrMessage;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.enums.ArchiveType;

import java.util.List;
import java.util.ArrayList;

import java.util.Set;

/**
 * Model - Rest Mapper for Product Archive
 * 
 * @author Denys Chaykovskiy
 */

public class ProductArchiveModelMapper {
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveModelMapper.class);
	
	/** Product Archive as input parameter */
	private ProductArchive modelArchive;
	
	/** Rest Archive as output parameter */
	private RestProductArchive restArchive = new RestProductArchive();  
	
	/**
	 * Constructor with modelArchive parameter
	 * 
	 * @param modelArchive model archive
	 */
	public ProductArchiveModelMapper(ProductArchive modelArchive) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> Constructor({})", (null == modelArchive ? "MISSING" : modelArchive.getId()));

		if (null == modelArchive) {
			throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_MISSING));
		}
		
		this.modelArchive = modelArchive; 
		
		setDefaultValuesIfNull();
		
		checkMandatoryFields();
	}
	
	/**
	 * Gets checked in constructor product archive
	 * 
	 * @return product archive
	 */
	public ProductArchive get() {
		
		return modelArchive;
	}
	
	/**
	 * Converts a prosEO model ProductArchive into a REST ProductArchive
	 * 
	 * @return an equivalent REST ProductArchive or null, if no model ProductArchive was given
	 */
	public RestProductArchive toRest() {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toRest({})", (null == modelArchive ? "MISSING" : modelArchive.getId()));
		
		setAvailableProductClasses();
					
		restArchive.setArchiveType(modelArchive.getArchiveType().toString());		
		restArchive.setBaseUri(modelArchive.getBaseUri());		
		restArchive.setClientId(modelArchive.getClientId());
		
		restArchive.setClientSecret(modelArchive.getClientSecret());		
		restArchive.setCode(modelArchive.getCode());
		restArchive.setContext(modelArchive.getContext());
		
		restArchive.setId(modelArchive.getId());		
		restArchive.setName(modelArchive.getName());
		
		restArchive.setPassword(modelArchive.getPassword());		
		restArchive.setSendAuthInBody(modelArchive.getSendAuthInBody());		
		restArchive.setTokenRequired(modelArchive.getTokenRequired());
		
		restArchive.setTokenUri(modelArchive.getTokenUri());		
		restArchive.setUsername(modelArchive.getUsername());	
		restArchive.setVersion((long) modelArchive.getVersion());
		
		return restArchive;
	}
					
	/**
	 * Sets default values in product archive
	 * 
	 */
	private void setDefaultValuesIfNull() {
		
		if (null == modelArchive.getArchiveType()) {
			modelArchive.setArchiveType(ArchiveType.AIP);
		}
		
		if (null == modelArchive.getTokenRequired()) {
			modelArchive.setTokenRequired(false);
		}
		
		if (null == modelArchive.getArchiveType()) {
			modelArchive.setSendAuthInBody(false);
		}
	}
	
	/**
	 * Checks mandatory fields in product archive
	 * 
	 */
	private void checkMandatoryFields() {
		
		if (StringUtils.isNullOrBlank(modelArchive.getCode())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Code", "product archive model checker"));
		}
		
		if (StringUtils.isNullOrBlank(modelArchive.getName())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Name", "product archive model checker"));
		}
		
		if (null == modelArchive.getArchiveType()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "ArchiveType", "product archive model checker"));
		}
		
		if (StringUtils.isNullOrBlank(modelArchive.getBaseUri())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "BaseUri", "product archive model checker"));
		}
		
		if (StringUtils.isNullOrBlank(modelArchive.getContext())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Context", "product archive model checker"));
		}
		
		if (modelArchive.getTokenRequired()) {
			if (StringUtils.isNullOrBlank(modelArchive.getTokenUri())) { 
				throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "TokenUri", "product archive model checker"));
			}
		}
		
		if (modelArchive.getSendAuthInBody()) {
			if (StringUtils.isNullOrBlank(modelArchive.getUsername())) {
				throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Username", "product archive model checker"));
			}
			if (null == modelArchive.getPassword()) {
				throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "Password", "product archive model checker"));
			}
		}
	}
	
	/**
	 * Sets available product classes of the rest product archive 
	 * 
	 */
	private void setAvailableProductClasses()
	{
		Set<ProductClass> modelProductClasses = modelArchive.getAvailableProductClasses();
		List<String> restProductClasses = new ArrayList<String>();
		
		for (ProductClass modelProductClass : modelProductClasses) {
			
			String productType = modelProductClass.getProductType();
			restProductClasses.add(productType);
		}
		
		restArchive.setAvailableProductClasses(restProductClasses);
	}	
}
