/**
 * ProductArchiveUtil.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest.model;

import java.util.HashSet;
import java.util.Set;

import de.dlr.proseo.logging.logger.ProseoLogger;
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

		this.restArchive = restArchive; 
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
		
		if (null == restArchive)
			return null;
				
		setDefaultModelValues();
		
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
	 * Sets Default values of the product archive model
	 * 
	 */
	private void setDefaultModelValues() {
		
		if (null == modelArchive.getArchiveType()) {
			modelArchive.setArchiveType(ArchiveType.AIP);
		}
		
		if (null == modelArchive.getTokenRequired()) {
			modelArchive.setTokenRequired(false);
		}
		
		if (null == modelArchive.getSendAuthInBody()) {
			modelArchive.setSendAuthInBody(false);
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
