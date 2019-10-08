/**
 * ProductUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest.model;

import java.time.DateTimeException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Product;

/**
 * Utility methods for products, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ProductUtil {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductUtil.class);
	
	/**
	 * Convert a prosEO model product into a REST product
	 * 
	 * @param modelProduct the prosEO model product
	 * @return an equivalent REST product or null, if no model product was given
	 */
	public static RestProduct toRestProduct(Product modelProduct) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestProduct({})", (null == modelProduct ? "MISSING" : modelProduct.getId()));

		if (null == modelProduct)
			return null;
		
		RestProduct restProduct = new RestProduct();
		
		restProduct.setId(modelProduct.getId());
		restProduct.setVersion(Long.valueOf(modelProduct.getVersion()));
		if (null != modelProduct.getProductClass()) {
			if (null != modelProduct.getProductClass().getMission()) {
				restProduct.setMissionCode(modelProduct.getProductClass().getMission().getCode());
			}
			restProduct.setProductClass(modelProduct.getProductClass().getProductType());
		}
		restProduct.setMode(modelProduct.getMode());
		if (null != modelProduct.getSensingStartTime()) {
			restProduct.setSensingStartTime(
					de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelProduct.getSensingStartTime()));
		}
		if (null != modelProduct.getSensingStopTime()) {
			restProduct.setSensingStopTime(
					de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelProduct.getSensingStopTime()));
		}
		if (null != modelProduct.getGenerationTime()) {
			restProduct.setGenerationTime(
					de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelProduct.getGenerationTime()));
		}
		for (Product componentProduct: modelProduct.getComponentProducts()) {
			restProduct.getComponentProductIds().add(componentProduct.getId());
		}
		if (null != modelProduct.getEnclosingProduct()) {
			restProduct.setEnclosingProductId(modelProduct.getEnclosingProduct().getId());
		}
		if (null != modelProduct.getOrbit()) {
			Orbit restOrbit = new Orbit();
			de.dlr.proseo.model.Orbit modelOrbit = modelProduct.getOrbit();
			restOrbit.setOrbitNumber(Long.valueOf(modelOrbit.getOrbitNumber()));
			restOrbit.setSpacecraftCode(modelOrbit.getSpacecraft().getCode());
			restProduct.setOrbit(restOrbit);
		}
		for (de.dlr.proseo.model.ProductFile modelFile: modelProduct.getProductFile()) {
			ProductFile restFile = new ProductFile();
			restFile.setId(modelFile.getId());
			restFile.setVersion(Long.valueOf(modelFile.getVersion()));
			restFile.setProcessingFacilityName(modelFile.getProcessingFacility().getName());
			restFile.setProductFileName(modelFile.getProductFileName());
			restFile.setFilePath(modelFile.getFilePath());
			restFile.setStorageType(modelFile.getStorageType().toString());
			for (String modelAuxFileName: modelFile.getAuxFileNames()) {
				restFile.getAuxFileNames().add(modelAuxFileName);
			}
			restProduct.getProductFile().add(restFile);
		}
		for (String productParameterKey: modelProduct.getParameters().keySet()) {
			Parameter restParameter = new Parameter(
					productParameterKey,
					modelProduct.getParameters().get(productParameterKey).getParameterType().toString(),
					modelProduct.getParameters().get(productParameterKey).getParameterValue().toString());
			restProduct.getParameters().add(restParameter);
		}
		
		return restProduct;
	}
	
	/**
	 * Convert a REST product into a prosEO model product (scalar and embedded attributes only, no product references)
	 * 
	 * @param restProduct the REST product
	 * @return a (roughly) equivalent model product or null, if no REST product was given
	 * @throws IllegalArgumentException if the REST product violates syntax rules for date, enum or numeric values
	 */
	public static Product toModelProduct(RestProduct restProduct) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelProduct({})", (null == restProduct ? "MISSING" : restProduct.getProductClass()));

		if (null == restProduct)
			return null;
		
		Product modelProduct = new Product();
		
		if (null != restProduct.getId() && 0 != restProduct.getId()) {
			modelProduct.setId(restProduct.getId());
			while (modelProduct.getVersion() < restProduct.getVersion()) {
				modelProduct.incrementVersion();
			} 
		}
		modelProduct.setMode(restProduct.getMode());
		try {
			modelProduct.setSensingStartTime(
					Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(restProduct.getSensingStartTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing start time '%s'", restProduct.getSensingStartTime()));
		}
		try {
			modelProduct.setSensingStopTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(restProduct.getSensingStopTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing stop time '%s'", restProduct.getSensingStartTime()));
		}
		try {
			modelProduct.setGenerationTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(restProduct.getGenerationTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid product generation time '%s'", restProduct.getGenerationTime()));
		}
		for (Parameter restParameter: restProduct.getParameters()) {
			de.dlr.proseo.model.Parameter modelParameter = new de.dlr.proseo.model.Parameter();
			try {
				modelParameter.setParameterType(de.dlr.proseo.model.Parameter.ParameterType.valueOf(restParameter.getParameterType()));
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("Invalid parameter type '%s'", restParameter.getParameterType()));
			}
			try {
				switch (modelParameter.getParameterType()) {
				case INTEGER: 	modelParameter.setIntegerValue(Integer.parseInt(restParameter.getParameterValue())); break;
				case STRING:	modelParameter.setStringValue(restParameter.getParameterValue()); break;
				case BOOLEAN:	modelParameter.setBooleanValue(Boolean.parseBoolean(restParameter.getParameterValue())); break;
				case DOUBLE:	modelParameter.setDoubleValue(Double.parseDouble(restParameter.getParameterValue())); break;
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("Invalid parameter value '%s' for type '%s'",
						restParameter.getParameterValue(), restParameter.getParameterType()));
			}
			modelProduct.getParameters().put(restParameter.getKey(), modelParameter);
		}
		
		return modelProduct;
	}
}
