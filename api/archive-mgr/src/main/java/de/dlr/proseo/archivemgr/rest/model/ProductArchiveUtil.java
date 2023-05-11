/**
 * ProductArchiveUtil.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest.model;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ProductArchiveMgrMessage;
import de.dlr.proseo.model.ProductArchive;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.enums.ArchiveType;

import java.util.List;
import java.util.ArrayList;

import java.util.Set;

/**
 * Model - Rest and Rest - Model Mapper for Product Archive
 * 
 * @author Denys Chaykovskiy
 */

// TODO: Think about data model, should it be inside or not, maybe mapper class or even separate classes
public class ProductArchiveUtil {
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveUtil.class);
	
	/**
	 * Converts a prosEO model ProductArchive into a REST ProductArchive
	 * 
	 * @param modelArchive the prosEO model ProductArchive
	 * @return an equivalent REST ProductArchive or null, if no model ProductArchive was given
	 */
	public static RestProductArchive toRestProductArchive(ProductArchive modelArchive) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toRestProductArchive({})", (null == modelArchive ? "MISSING" : modelArchive.getId()));
	
		if (null == modelArchive)
			return null;
		
		RestProductArchive restArchive = new RestProductArchive();
		
		setDefaultRestValues(restArchive);
		
		
		Set<ProductClass> modelProductClasses = modelArchive.getAvailableProductClasses();
		List<String> restProductClasses = new ArrayList<String>();
		
		for (ProductClass modelProductClass : modelProductClasses) {
			
			String productType = modelProductClass.getProductType();
			restProductClasses.add(productType);
		}
		restArchive.setAvailableProductClasses(restProductClasses);
		
		
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
	 * Convert a REST ProductArchive into a prosEO model ProductArchive 
	 * AvailableProductClasses will not be set here, the field will be null
	 * 
	 * @param restArchive the REST ProductArchive
	 * @return model Product Archive
	 * @throws IllegalArgumentException if the REST Product Archive has a wrong archive type
	 */
	public static ProductArchive toModelProductArchive(RestProductArchive restArchive) throws IllegalArgumentException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toModelProductArchive({})", (null == restArchive ? "MISSING" : restArchive.getId()));
		
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
	 * @param modelArchive
	 */
	public static void setDefaultModelValues(ProductArchive modelArchive) {
		
		modelArchive.setArchiveType(ArchiveType.AIP);
		
		modelArchive.setTokenRequired(false);
		
		modelArchive.setSendAuthInBody(false);
	}
		
	/**
	 * @param restArchive
	 */
	public static void setDefaultRestValues(RestProductArchive restArchive) {
		
		restArchive.setArchiveType(ArchiveType.AIP.toString());
		
		restArchive.setTokenRequired(false);
		
		restArchive.setSendAuthInBody(false);
	}
}
