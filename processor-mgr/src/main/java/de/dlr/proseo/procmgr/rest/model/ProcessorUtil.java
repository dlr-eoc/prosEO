/**
 * ProcessorUtil.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest.model;

import java.util.Map.Entry;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.enums.JobOrderVersion;

/**
 * Utility methods for processors, e. g. for conversion between prosEO model and REST model
 *
 * @author Dr. Thomas Bassler
 */
public class ProcessorUtil {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorUtil.class);

	/**
	 * Convert a prosEO model processor into a REST processor
	 *
	 * @param modelProcessor the prosEO model processor
	 * @return an equivalent REST processor or null, if no model processor was given
	 */
	public static RestProcessor toRestProcessor(Processor modelProcessor) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestProcessor({})", (null == modelProcessor ? "MISSING" : modelProcessor.getId()));

		if (null == modelProcessor)
			return null;

		RestProcessor restProcessor = new RestProcessor();

		restProcessor.setId(modelProcessor.getId());
		restProcessor.setVersion(Long.valueOf(modelProcessor.getVersion()));
		restProcessor.setMissionCode(modelProcessor.getProcessorClass().getMission().getCode());
		restProcessor.setProcessorName(modelProcessor.getProcessorClass().getProcessorName());
		restProcessor.setProcessorVersion(modelProcessor.getProcessorVersion());
		restProcessor.setJobOrderVersion(modelProcessor.getJobOrderVersion().toString());
		restProcessor.setUseInputFileTimeIntervals(modelProcessor.getUseInputFileTimeIntervals());
		restProcessor.setIsTest(modelProcessor.getIsTest());
		restProcessor.setMinDiskSpace(Long.valueOf(modelProcessor.getMinDiskSpace()));
		restProcessor.setMaxTime(Long.valueOf(modelProcessor.getMaxTime()));
		restProcessor.setSensingTimeFlag(modelProcessor.getSensingTimeFlag());
		restProcessor.setDockerImage(modelProcessor.getDockerImage());

		for (ConfiguredProcessor configuredProcessor : modelProcessor.getConfiguredProcessors()) {
			restProcessor.getConfiguredProcessors().add(configuredProcessor.getIdentifier());
		}

		for (Task task : modelProcessor.getTasks()) {
			restProcessor.getTasks().add(TaskUtil.toRestTask(task));
		}

		for (Entry<String, String> dockerRunParam : modelProcessor.getDockerRunParameters().entrySet()) {
			restProcessor.getDockerRunParameters().add(new RestStringParameter(dockerRunParam.getKey(), dockerRunParam.getValue()));
		}

		return restProcessor;
	}

	/**
	 * Convert a REST processor into a prosEO model processor (scalar and embedded attributes only, no object references)
	 *
	 * @param restProcessor the REST product
	 * @return a (roughly) equivalent model product or null, if no REST product was given
	 * @throws IllegalArgumentException if the REST product violates syntax rules for date, enum or numeric values
	 */
	public static Processor toModelProcessor(RestProcessor restProcessor) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelProcessorClass({})", (null == restProcessor ? "MISSING" : restProcessor.getProcessorName()));

		if (null == restProcessor)
			return null;

		Processor modelProcessor = new Processor();

		if (null != restProcessor.getId() && 0 != restProcessor.getId()) {
			modelProcessor.setId(restProcessor.getId());
			while (modelProcessor.getVersion() < restProcessor.getVersion()) {
				modelProcessor.incrementVersion();
			}
		}
		modelProcessor.setProcessorVersion(restProcessor.getProcessorVersion());
		if (null != restProcessor.getJobOrderVersion()) {
			modelProcessor.setJobOrderVersion(JobOrderVersion.valueOf(restProcessor.getJobOrderVersion()));
		}
		if (null != restProcessor.getUseInputFileTimeIntervals()) {
			modelProcessor.setUseInputFileTimeIntervals(restProcessor.getUseInputFileTimeIntervals());
		}
		modelProcessor.setIsTest(restProcessor.getIsTest());
		modelProcessor.setMinDiskSpace(restProcessor.getMinDiskSpace().intValue());
		modelProcessor.setMaxTime(restProcessor.getMaxTime().intValue());
		modelProcessor.setSensingTimeFlag(restProcessor.getSensingTimeFlag());
		modelProcessor.setDockerImage(restProcessor.getDockerImage());

		for (RestStringParameter restDockerParam : restProcessor.getDockerRunParameters()) {
			modelProcessor.getDockerRunParameters().put(restDockerParam.getKey(), restDockerParam.getValue());
		}

		return modelProcessor;
	}
}
