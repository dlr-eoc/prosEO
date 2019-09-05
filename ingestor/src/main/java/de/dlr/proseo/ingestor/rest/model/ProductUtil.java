/**
 * ProductUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest.model;

import java.time.DateTimeException;
import java.time.Instant;

/**
 * Utility methods for products, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ProductUtil {

	/**
	 * Convert a prosEO model product into a REST product
	 * 
	 * @param modelProduct the prosEO model product
	 * @return an equivalent REST product or null, if no model product was given
	 */
	public static Product toRestProduct(de.dlr.proseo.model.Product modelProduct) {
		if (null == modelProduct)
			return null;
		
		Product restProduct = new Product();
		
		restProduct.setId(modelProduct.getId());
		restProduct.setVersion(Long.valueOf(modelProduct.getVersion()));
		restProduct.setMissionCode(modelProduct.getProductClass().getMission().getCode());
		restProduct.setProductClass(modelProduct.getProductClass().getProductType());
		restProduct.setMode(modelProduct.getMode());
		restProduct.setSensingStartTime(de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelProduct.getSensingStartTime()));
		restProduct.setSensingStopTime(de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelProduct.getSensingStopTime()));
		for (de.dlr.proseo.model.Product componentProduct: modelProduct.getComponentProducts()) {
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
			de.dlr.proseo.ingestor.rest.model.Parameter restParameter = new de.dlr.proseo.ingestor.rest.model.Parameter(
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
	 * @return a (roughly) equivalent model product
	 * @throws IllegalArgumentException if the REST product violates syntax rules for date, enum or numeric values
	 */
	public static de.dlr.proseo.model.Product toModelProduct(Product restProduct) throws IllegalArgumentException {
		de.dlr.proseo.model.Product modelProduct = new de.dlr.proseo.model.Product();
		
		modelProduct.setId(restProduct.getId());
		while (modelProduct.getVersion() < restProduct.getVersion()) {
			modelProduct.incrementVersion();
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
		for (Parameter restParameter: restProduct.getParameters()) {
			de.dlr.proseo.model.Parameter modelParameter = new de.dlr.proseo.model.Parameter();
			try {
				modelParameter.seParametertType(de.dlr.proseo.model.Product.ParameterType.valueOf(restParameter.getParameterType()));
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
