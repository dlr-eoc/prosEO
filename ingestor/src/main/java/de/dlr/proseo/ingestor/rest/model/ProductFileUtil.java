/**
 * ProductFileUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest.model;

import java.time.DateTimeException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ProductFile;

/**
 * Utility methods for product files, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ProductFileUtil {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductFileUtil.class);
	
	/**
	 * Convert a prosEO model product file into a REST product file
	 * 
	 * @param modelProductFile the prosEO model product
	 * @return an equivalent REST product or null, if no model product was given
	 */
	public static RestProductFile toRestProductFile(ProductFile modelProductFile) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestProductFile({})", (null == modelProductFile ? "MISSING" : modelProductFile.getId()));

		if (null == modelProductFile)
			return null;
		
		RestProductFile restProductFile = new RestProductFile();
		
		restProductFile.setId(modelProductFile.getId());
		restProductFile.setVersion(Long.valueOf(modelProductFile.getVersion()));
		restProductFile.setProcessingFacilityName(modelProductFile.getProcessingFacility().getName());
		restProductFile.setProductFileName(modelProductFile.getProductFileName());
		restProductFile.setFilePath(modelProductFile.getFilePath());
		restProductFile.setStorageType(modelProductFile.getStorageType().toString());
		restProductFile.getAuxFileNames().addAll(modelProductFile.getAuxFileNames());
		
		return restProductFile;
	}
	
	/**
	 * Convert a REST product file into a prosEO model product file (scalar and embedded attributes only, no product references)
	 * 
	 * @param restProductFile the REST product
	 * @return a (roughly) equivalent model product or null, if no REST product was given
	 * @throws IllegalArgumentException if the REST product violates syntax rules for date, enum or numeric values
	 */
	public static ProductFile toModelProductFile(RestProductFile restProductFile) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelProductFile({})", (null == restProductFile ? "MISSING" : restProductFile.getId()));

		if (null == restProductFile)
			return null;
		
		ProductFile modelProductFile = new ProductFile();
		
		if (null != restProductFile.getId() && 0 != restProductFile.getId()) {
			modelProductFile.setId(restProductFile.getId());
			while (modelProductFile.getVersion() < restProductFile.getVersion()) {
				modelProductFile.incrementVersion();
			} 
		}
		
		modelProductFile.setProductFileName(restProductFile.getProductFileName());
		modelProductFile.getAuxFileNames().addAll(restProductFile.getAuxFileNames());
		modelProductFile.setFilePath(restProductFile.getFilePath());
		
		return modelProductFile;
	}
}