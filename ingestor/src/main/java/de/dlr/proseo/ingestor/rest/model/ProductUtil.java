/**
 * ProductUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest.model;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.ProductQuality;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility methods for products, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ProductUtil {

	/* Message ID constants */
	private static final int MSG_ID_PRODUCT_UUID_INVALID = 2021;
	private static final int MSG_ID_INVALID_PARAMETER_VALUE = 2049;
	private static final int MSG_ID_INVALID_PARAMETER_TYPE = 2048;
	private static final int MSG_ID_INVALID_PRODUCT_GENERATION_TIME = 2047;
	private static final int MSG_ID_INVALID_SENSING_STOP_TIME = 2046;
	private static final int MSG_ID_INVALID_SENSING_START_TIME = 2045;
	private static final int MSG_ID_INVALID_PRODUCTION_TYPE = 2050;

	/* Message string constants */
	private static final String MSG_PRODUCT_UUID_INVALID = "(E%d) Product UUID %s invalid";
	private static final String MSG_INVALID_PARAMETER_VALUE = "(E%d) Invalid parameter value '%s' for type '%s'";
	private static final String MSG_INVALID_PARAMETER_TYPE = "(E%d) Invalid parameter type '%s'";
	private static final String MSG_INVALID_PRODUCT_GENERATION_TIME = "(E%d) Invalid product generation time '%s'";
	private static final String MSG_INVALID_SENSING_STOP_TIME = "(E%d) Invalid sensing stop time '%s'";
	private static final String MSG_INVALID_SENSING_START_TIME = "(E%d) Invalid sensing start time '%s'";
	private static final String MSG_INVALID_PRODUCTION_TYPE = "(E%d) Invalid production type '%s'";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductUtil.class);
	
	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private static String logError(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);
		
		return message;
	}
	
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
		if (null != modelProduct.getUuid()) {
			restProduct.setUuid(modelProduct.getUuid().toString());
		}
		if (null != modelProduct.getProductClass()) {
			if (null != modelProduct.getProductClass().getMission()) {
				restProduct.setMissionCode(modelProduct.getProductClass().getMission().getCode());
			}
			restProduct.setProductClass(modelProduct.getProductClass().getProductType());
		}
		restProduct.setFileClass(modelProduct.getFileClass());
		restProduct.setMode(modelProduct.getMode());
		if (null != modelProduct.getProductQuality()) {
			restProduct.setProductQuality(modelProduct.getProductQuality().toString());
		}
		if (null != modelProduct.getSensingStartTime()) {
			restProduct.setSensingStartTime(OrbitTimeFormatter.format(modelProduct.getSensingStartTime()));
		}
		if (null != modelProduct.getSensingStopTime()) {
			restProduct.setSensingStopTime(OrbitTimeFormatter.format(modelProduct.getSensingStopTime()));
		}
		if (null != modelProduct.getGenerationTime()) {
			restProduct.setGenerationTime(OrbitTimeFormatter.format(modelProduct.getGenerationTime()));
		}
		if (null != modelProduct.getProductionType()) {
			restProduct.setProductionType(modelProduct.getProductionType().toString());
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
		for (ProductFile modelFile: modelProduct.getProductFile()) {
			restProduct.getProductFile().add(ProductFileUtil.toRestProductFile(modelFile));
		}
		if (null != modelProduct.getConfiguredProcessor()) {
			ConfiguredProcessor modelConfiguredProcessor = modelProduct.getConfiguredProcessor();
			RestConfiguredProcessor restConfiguredProcessor = new RestConfiguredProcessor();
			restConfiguredProcessor.setId(modelConfiguredProcessor.getId());
			restConfiguredProcessor.setVersion(Long.valueOf(modelConfiguredProcessor.getVersion()));
			restConfiguredProcessor.setIdentifier(modelConfiguredProcessor.getIdentifier());
			restConfiguredProcessor.setProcessorName(modelConfiguredProcessor.getProcessor().getProcessorClass().getProcessorName());
			restConfiguredProcessor.setProcessorVersion(modelConfiguredProcessor.getProcessor().getProcessorVersion());
			restConfiguredProcessor.setConfigurationVersion(modelConfiguredProcessor.getConfiguration().getConfigurationVersion());
			restProduct.setConfiguredProcessor(restConfiguredProcessor);
		}
		for (String productParameterKey: modelProduct.getParameters().keySet()) {
			RestParameter restParameter = new RestParameter(
					productParameterKey,
					modelProduct.getParameters().get(productParameterKey).getParameterType().toString(),
					modelProduct.getParameters().get(productParameterKey).getParameterValue().toString());
			restProduct.getParameters().add(restParameter);
		}
		
		return restProduct;
	}
	
	/**
	 * Convert a REST product into a prosEO model product (scalar and embedded attributes only, no object references)
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
		if (null != restProduct.getUuid()) {
			try {
				modelProduct.setUuid(UUID.fromString(restProduct.getUuid()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
						logError(MSG_PRODUCT_UUID_INVALID, MSG_ID_PRODUCT_UUID_INVALID, restProduct.getUuid()));
			} 
		}
		modelProduct.setFileClass(restProduct.getFileClass());
		modelProduct.setMode(restProduct.getMode());
		if (null != restProduct.getProductQuality()) {
			modelProduct.setProductQuality(ProductQuality.valueOf(restProduct.getProductQuality()));
		}
		try {
			modelProduct.setSensingStartTime(
					Instant.from(OrbitTimeFormatter.parse(restProduct.getSensingStartTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(logError(MSG_INVALID_SENSING_START_TIME, MSG_ID_INVALID_SENSING_START_TIME,
					restProduct.getSensingStartTime()));
		}
		try {
			modelProduct.setSensingStopTime(Instant.from(OrbitTimeFormatter.parse(restProduct.getSensingStopTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(logError(MSG_INVALID_SENSING_STOP_TIME, MSG_ID_INVALID_SENSING_STOP_TIME,
					restProduct.getSensingStartTime()));
		}
		try {
			modelProduct.setGenerationTime(Instant.from(OrbitTimeFormatter.parse(restProduct.getGenerationTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(logError(MSG_INVALID_PRODUCT_GENERATION_TIME, MSG_ID_INVALID_PRODUCT_GENERATION_TIME,
					restProduct.getGenerationTime()));
		}
		if (null != restProduct.getProductionType()) {
			try {
				modelProduct.setProductionType(ProductionType.valueOf(restProduct.getProductionType()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(logError(MSG_INVALID_PRODUCTION_TYPE, MSG_ID_INVALID_PRODUCTION_TYPE,
						restProduct.getProductionType()));
			}
		}
		for (RestParameter restParameter: restProduct.getParameters()) {
			de.dlr.proseo.model.Parameter modelParameter = new de.dlr.proseo.model.Parameter();
			try {
				modelParameter.setParameterType(de.dlr.proseo.model.Parameter.ParameterType.valueOf(restParameter.getParameterType()));
			} catch (Exception e) {
				throw new IllegalArgumentException(logError(MSG_INVALID_PARAMETER_TYPE, MSG_ID_INVALID_PARAMETER_TYPE,
						restParameter.getParameterType()));
			}
			try {
				switch (modelParameter.getParameterType()) {
				case INTEGER: 	modelParameter.setIntegerValue(Integer.parseInt(restParameter.getParameterValue())); break;
				case STRING:	modelParameter.setStringValue(restParameter.getParameterValue()); break;
				case BOOLEAN:	modelParameter.setBooleanValue(Boolean.parseBoolean(restParameter.getParameterValue())); break;
				case DOUBLE:	modelParameter.setDoubleValue(Double.parseDouble(restParameter.getParameterValue())); break;
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(logError(MSG_INVALID_PARAMETER_VALUE, MSG_ID_INVALID_PARAMETER_VALUE,
						restParameter.getParameterValue(), restParameter.getParameterType()));
			}
			modelProduct.getParameters().put(restParameter.getKey(), modelParameter);
		}
		
		return modelProduct;
	}
}
