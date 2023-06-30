/**
 * ConfiguredProcessorUtil.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest.model;

import java.util.UUID;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ConfiguredProcessor;

/**
 * Utility methods for configured processors, e. g. for conversion between prosEO model and REST model
 *
 * @author Dr. Thomas Bassler
 */
public class ConfiguredProcessorUtil {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfiguredProcessorUtil.class);

	/**
	 * Convert a prosEO model configured processor into a REST configured processor
	 *
	 * @param modelConfiguredProcessor the prosEO model configured processor
	 * @return an equivalent REST configured processor or null, if no model configured processor was given
	 */
	public static RestConfiguredProcessor toRestConfiguredProcessor(ConfiguredProcessor modelConfiguredProcessor) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestConfiguredProcessor({})",
					(null == modelConfiguredProcessor ? "MISSING" : modelConfiguredProcessor.getId()));

		if (null == modelConfiguredProcessor)
			return null;

		RestConfiguredProcessor restConfiguredProcessor = new RestConfiguredProcessor();

		restConfiguredProcessor.setId(modelConfiguredProcessor.getId());
		restConfiguredProcessor.setVersion(Long.valueOf(modelConfiguredProcessor.getVersion()));
		restConfiguredProcessor.setMissionCode(modelConfiguredProcessor.getProcessor().getProcessorClass().getMission().getCode());
		restConfiguredProcessor.setProcessorName(modelConfiguredProcessor.getProcessor().getProcessorClass().getProcessorName());
		restConfiguredProcessor.setProcessorVersion(modelConfiguredProcessor.getProcessor().getProcessorVersion());
		restConfiguredProcessor.setConfigurationVersion(modelConfiguredProcessor.getConfiguration().getConfigurationVersion());
		restConfiguredProcessor.setIdentifier(modelConfiguredProcessor.getIdentifier());
		restConfiguredProcessor.setEnabled(modelConfiguredProcessor.getEnabled());
		if (null != modelConfiguredProcessor.getUuid()) {
			restConfiguredProcessor.setUuid(modelConfiguredProcessor.getUuid().toString());
		}
		return restConfiguredProcessor;
	}

	/**
	 * Convert a REST configured processor into a prosEO model configured processor (scalar and embedded attributes only, no object
	 * references)
	 *
	 * @param restConfiguredProcessor the REST configured processor
	 * @return a (roughly) equivalent model configured processor or null, if no REST processor task was given
	 * @throws IllegalArgumentException if the REST configured processor violates syntax rules for date, enum or numeric values
	 */
	public static ConfiguredProcessor toModelConfiguredProcessor(RestConfiguredProcessor restConfiguredProcessor)
			throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelConfiguredProcessor({})",
					(null == restConfiguredProcessor ? "MISSING" : restConfiguredProcessor.getIdentifier()));

		if (null == restConfiguredProcessor)
			return null;

		ConfiguredProcessor modelConfiguredProcessor = new ConfiguredProcessor();

		if (null != restConfiguredProcessor.getId() && 0 != restConfiguredProcessor.getId()) {
			modelConfiguredProcessor.setId(restConfiguredProcessor.getId());
			while (modelConfiguredProcessor.getVersion() < restConfiguredProcessor.getVersion()) {
				modelConfiguredProcessor.incrementVersion();
			}
		}
		modelConfiguredProcessor.setIdentifier(restConfiguredProcessor.getIdentifier());
		if (null != restConfiguredProcessor.getEnabled()) {
			modelConfiguredProcessor.setEnabled(restConfiguredProcessor.getEnabled());
		}
		if (null != restConfiguredProcessor.getUuid()) {
			modelConfiguredProcessor.setUuid(UUID.fromString(restConfiguredProcessor.getUuid()));
		}

		return modelConfiguredProcessor;
	}
}
