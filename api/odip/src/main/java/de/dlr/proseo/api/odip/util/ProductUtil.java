/**
 * ProductUtil.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.util;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.ProductQuality;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.rest.model.RestProduct;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility methods for products, e. g. for conversion between prosEO model and REST model
 *
 * @author Dr. Thomas Bassler
 */
public class ProductUtil {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductUtil.class);

	/**
	 * Convert a REST product into a prosEO model product (scalar and embedded attributes only, no object references); does not
	 * create or update the download history.
	 *
	 * @param restProduct the REST product
	 * @return a (roughly) equivalent model product or null, if no REST product was given
	 * @throws IllegalArgumentException if the REST product violates syntax rules for date, enum or numeric values
	 */
	public static Product toModelProduct(RestProduct restProduct) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelProduct({})", (null == restProduct ? "MISSING" : restProduct.getProductClass()));

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
				throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_UUID_INVALID, restProduct.getUuid()));
			}
		}

		modelProduct.setFileClass(restProduct.getFileClass());
		modelProduct.setMode(restProduct.getMode());

		if (null != restProduct.getProductQuality()) {
			modelProduct.setProductQuality(ProductQuality.valueOf(restProduct.getProductQuality()));
		}

		try {
			modelProduct.setSensingStartTime(Instant.from(OrbitTimeFormatter.parse(restProduct.getSensingStartTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(
					logger.log(IngestorMessage.INVALID_SENSING_START_TIME, restProduct.getSensingStartTime()));
		}

		try {
			modelProduct.setSensingStopTime(Instant.from(OrbitTimeFormatter.parse(restProduct.getSensingStopTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(
					logger.log(IngestorMessage.INVALID_SENSING_STOP_TIME, restProduct.getSensingStartTime()));
		}

		if (null == restProduct.getRawDataAvailabilityTime()) {
			modelProduct.setRawDataAvailabilityTime(null);
		} else {
			try {
				modelProduct
					.setRawDataAvailabilityTime(Instant.from(OrbitTimeFormatter.parse(restProduct.getRawDataAvailabilityTime())));
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_RAW_DATA_AVAILABILITY_TIME, restProduct.getRawDataAvailabilityTime()));
			}
		}

		try {
			modelProduct.setGenerationTime(Instant.from(OrbitTimeFormatter.parse(restProduct.getGenerationTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(
					logger.log(IngestorMessage.INVALID_PRODUCT_GENERATION_TIME, restProduct.getGenerationTime()));
		}

		if (null == restProduct.getPublicationTime()) {
			modelProduct.setPublicationTime(null);
		} else {
			try {
				modelProduct.setPublicationTime(Instant.from(OrbitTimeFormatter.parse(restProduct.getPublicationTime())));
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_PUBLICATION_TIME, restProduct.getPublicationTime()));
			}
		}

		if (null == restProduct.getEvictionTime()) {
			modelProduct.setEvictionTime(null);
		} else {
			try {
				modelProduct.setEvictionTime(Instant.from(OrbitTimeFormatter.parse(restProduct.getEvictionTime())));
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_EVICTION_TIME, restProduct.getEvictionTime()));
			}
		}

		if (null == restProduct.getProductionType()) {
			modelProduct.setProductionType(null);
		} else {
			try {
				modelProduct.setProductionType(ProductionType.valueOf(restProduct.getProductionType()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_PRODUCTION_TYPE, restProduct.getProductionType()));
			}
		}

		for (RestParameter restParameter : restProduct.getParameters()) {
			de.dlr.proseo.model.Parameter modelParameter = new de.dlr.proseo.model.Parameter();
			if (restParameter.getParameterType() != null && !restParameter.getParameterType().equals("null")) {
				try {
					modelParameter.setParameterType(ParameterType.valueOf(restParameter.getParameterType()));
				} catch (Exception e) {
					throw new IllegalArgumentException(
							logger.log(IngestorMessage.INVALID_PARAMETER_TYPE, restParameter.getParameterType()));
				}
			} else {
				modelParameter.setParameterType(ParameterType.STRING);
			}
			try {
				switch (modelParameter.getParameterType()) {
				case INTEGER:
					modelParameter.setIntegerValue(Integer.parseInt(restParameter.getParameterValue()));
					break;
				case STRING:
					modelParameter.setStringValue(restParameter.getParameterValue());
					break;
				case BOOLEAN:
					modelParameter.setBooleanValue(Boolean.parseBoolean(restParameter.getParameterValue()));
					break;
				case DOUBLE:
					modelParameter.setDoubleValue(Double.parseDouble(restParameter.getParameterValue()));
					break;
				case INSTANT:
					modelParameter.setInstantValue(OrbitTimeFormatter.parseDateTime(restParameter.getParameterValue()));
					break;
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.INVALID_PARAMETER_VALUE,
						restParameter.getParameterValue(), restParameter.getParameterType()));
			}
			modelProduct.getParameters().put(restParameter.getKey(), modelParameter);
		}

		return modelProduct;
	}

	/**
	 * Convert a REST product represented as map into a prosEO model product (scalar and embedded attributes only, no object references); does not
	 * create or update the download history.
	 *
	 * @param restProductMap the REST product map
	 * @return a (roughly) equivalent model product or null, if no REST product map was given
	 * @throws IllegalArgumentException if the REST product map violates syntax rules for date, enum or numeric values
	 */
	@SuppressWarnings("unchecked")
	public static Product toModelProduct(Map<String, Object> restProductMap) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> restProductMap({})", (null == restProductMap ? "MISSING" : restProductMap));

		if (logger.isTraceEnabled())
			logger.trace(">>> toModelProduct({})", (null == restProductMap ? "MISSING" : restProductMap.get("productClass")));

		if (null == restProductMap)
			return null;

		Product modelProduct = new Product();
		Long id = 0L;
		if (null != restProductMap.get("id")) {
			id = ((Integer)(restProductMap.get("id"))).longValue();
		}
		if (0 != id) {
			modelProduct.setId(id);
			while (modelProduct.getVersion() < (Integer)(restProductMap.get("version"))) {
				modelProduct.incrementVersion();
			}
		}

		if (null != restProductMap.get("uuid")) {
			try {
				modelProduct.setUuid(UUID.fromString((String)(restProductMap.get("uuid"))));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_UUID_INVALID, (String)(restProductMap.get("uuid"))));
			}
		}

		modelProduct.setFileClass((String)(restProductMap.get("fileClass")));
		modelProduct.setMode((String)(restProductMap.get("mode")));

		if (null != restProductMap.get("productQuality")) {
			modelProduct.setProductQuality(ProductQuality.valueOf((String)(restProductMap.get("productQuality"))));
		}

		try {
			modelProduct.setSensingStartTime(Instant.from(OrbitTimeFormatter.parse((String)(restProductMap.get("sensingStartTime")))));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(
					logger.log(IngestorMessage.INVALID_SENSING_START_TIME, (String)(restProductMap.get("sensingStartTime"))));
		}

		try {
			modelProduct.setSensingStopTime(Instant.from(OrbitTimeFormatter.parse((String)(restProductMap.get("sensingStopTime")))));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(
					logger.log(IngestorMessage.INVALID_SENSING_STOP_TIME, (String)(restProductMap.get("sensingStopTime"))));
		}

		if (null == restProductMap.get("rawDataAvailabilityTime")) {
			modelProduct.setRawDataAvailabilityTime(null);
		} else {
			try {
				modelProduct
					.setRawDataAvailabilityTime(Instant.from(OrbitTimeFormatter.parse((String)(restProductMap.get("rawDataAvailabilityTime")))));
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_RAW_DATA_AVAILABILITY_TIME, (String)(restProductMap.get("rawDataAvailabilityTime"))));
			}
		}

		try {
			modelProduct.setGenerationTime(Instant.from(OrbitTimeFormatter.parse((String)(restProductMap.get("generationTime")))));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(
					logger.log(IngestorMessage.INVALID_PRODUCT_GENERATION_TIME, (String)(restProductMap.get("generationTime"))));
		}

		if (null == restProductMap.get("publicationTime")) {
			modelProduct.setPublicationTime(null);
		} else {
			try {
				modelProduct.setPublicationTime(Instant.from(OrbitTimeFormatter.parse((String)(restProductMap.get("publicationTime")))));
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_PUBLICATION_TIME, (String)(restProductMap.get("publicationTime"))));
			}
		}

		if (null == restProductMap.get("evictionTime")) {
			modelProduct.setEvictionTime(null);
		} else {
			try {
				modelProduct.setEvictionTime(Instant.from(OrbitTimeFormatter.parse((String)(restProductMap.get("evictionTime")))));
			} catch (DateTimeException e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_EVICTION_TIME, (String)(restProductMap.get("evictionTime"))));
			}
		}

		if (null == restProductMap.get("productionType")) {
			modelProduct.setProductionType(null);
		} else {
			try {
				modelProduct.setProductionType(ProductionType.valueOf((String)(restProductMap.get("productionType"))));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
						logger.log(IngestorMessage.INVALID_PRODUCTION_TYPE, (String)(restProductMap.get("productionType"))));
			}
		}
		List<Map<String, String>> restParameterMapList = null;
		if (restProductMap.get("parameters") != null) {
			restParameterMapList = (List<Map<String, String>>)(restProductMap.get("parameters"));
		}
		if (restParameterMapList != null) {
			for (Map<String, String> restParameterMap : restParameterMapList) {
				de.dlr.proseo.model.Parameter modelParameter = new de.dlr.proseo.model.Parameter();
				if (restParameterMap.get("parameterType") != null && !((String)(restParameterMap.get("parameterType"))).equals("null")) {
					try {
						modelParameter.setParameterType(ParameterType.valueOf(((String)(restParameterMap.get("parameterType")))));
					} catch (Exception e) {
						throw new IllegalArgumentException(
								logger.log(IngestorMessage.INVALID_PARAMETER_TYPE, ((String)(restParameterMap.get("parameterType")))));
					}
				} else {
					modelParameter.setParameterType(ParameterType.STRING);
				}
				try {
					switch (modelParameter.getParameterType()) {
					case INTEGER:
						modelParameter.setIntegerValue(Integer.parseInt((String)(restParameterMap.get("parameterValue"))));
						break;
					case STRING:
						modelParameter.setStringValue((String)(restParameterMap.get("parameterValue")));
						break;
					case BOOLEAN:
						modelParameter.setBooleanValue(Boolean.parseBoolean((String)(restParameterMap.get("parameterValue"))));
						break;
					case DOUBLE:
						modelParameter.setDoubleValue(Double.parseDouble((String)(restParameterMap.get("parameterValue"))));
						break;
					case INSTANT:
						modelParameter.setInstantValue(OrbitTimeFormatter.parseDateTime((String)(restParameterMap.get("parameterValue"))));
						break;
					}
				} catch (Exception e) {
					throw new IllegalArgumentException(logger.log(IngestorMessage.INVALID_PARAMETER_VALUE,
							(String)(restParameterMap.get("parameterValue")), (String)(restParameterMap.get("parameterType"))));
				}
				modelProduct.getParameters().put((String)(restParameterMap.get("key")), modelParameter);
			}
		}

		return modelProduct;
	}

}