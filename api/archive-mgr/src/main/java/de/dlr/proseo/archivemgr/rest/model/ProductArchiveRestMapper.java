/**
 * ProductArchiveUtil.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest.model;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ProductArchiveMgrMessage;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.enums.ArchiveType;

/**
 * Rest - Model Mapper for Product Archive
 * 
 * @author Denys Chaykovskiy
 */

public class ProductArchiveRestMapper {
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveRestMapper.class);
						
	/**
	 * Convert a REST ProductArchive into a prosEO model ProductArchive 
	 * AvailableProductClasses will not be set here, the field will be null
	 * 
	 * @param restArchive the REST ProductArchive
	 * @return model Product Archive
	 * @throws IllegalArgumentException if the REST Product Archive has a wrong archive type
	 */
	public static ProductArchive toModel(RestProductArchive restArchive) throws IllegalArgumentException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toModel({})", (null == restArchive ? "MISSING" : restArchive.getId()));
		
		if (null == restArchive)
			return null;
		
		ProductArchive modelArchive = new ProductArchive();
		
		setDefaultModelValues(modelArchive);
		
		try {
			modelArchive.setArchiveType(ArchiveType.valueOf(restArchive.getArchiveType()));	
	    } catch (IllegalArgumentException e) {
	    	 
	    	throw new IllegalArgumentException(logger.log(ProductArchiveMgrMessage.ARCHIVE_TYPE_WRONG));	    	 
	    }
		
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
	 * @param modelArchive product archive model
	 */
	private static void setDefaultModelValues(ProductArchive modelArchive) {
		
		modelArchive.setArchiveType(ArchiveType.AIP);
		
		modelArchive.setTokenRequired(false);
		
		modelArchive.setSendAuthInBody(false);
	}
}
