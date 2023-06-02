/**
 * ProductArchiveUtil.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest.model;

import de.dlr.proseo.logging.logger.ProseoLogger;
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

		this.modelArchive = modelArchive; 
	}
	
	/**
	 * Converts a prosEO model ProductArchive into a REST ProductArchive
	 * 
	 * @return an equivalent REST ProductArchive or null, if no model ProductArchive was given
	 */
	public RestProductArchive toRest() {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toRest({})", (null == modelArchive ? "MISSING" : modelArchive.getId()));
	
		if (null == modelArchive)
			return null;
				
		setDefaultRestValues();
		
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
	 * Sets default values in rest product archive
	 * 
	 * @param restArchive rest product archive
	 */
	private void setDefaultRestValues() {
		
		if (null == restArchive.getArchiveType()) {
			restArchive.setArchiveType(ArchiveType.AIP.toString());
		}
		
		if (null == restArchive.getTokenRequired()) {
			restArchive.setTokenRequired(false);
		}
		
		if (null == restArchive.getArchiveType()) {
			restArchive.setSendAuthInBody(false);
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
