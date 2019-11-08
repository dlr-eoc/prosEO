/**
 * ProcessorUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.Task;

/**
 * Utility methods for processors, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ProcessorUtil {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorUtil.class);
	
	/**
	 * Convert a prosEO model processor into a REST processor
	 * 
	 * @param modelProcessor the prosEO model processor
	 * @return an equivalent REST processor or null, if no model processor was given
	 */
	public static RestProcessor toRestProcessor(Processor modelProcessor) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestProcessor({})", (null == modelProcessor ? "MISSING" : modelProcessor.getId()));

		if (null == modelProcessor)
			return null;
		
		RestProcessor restProcessor = new RestProcessor();
		
		restProcessor.setId(modelProcessor.getId());
		restProcessor.setVersion(Long.valueOf(modelProcessor.getVersion()));
		restProcessor.setMissionCode(modelProcessor.getProcessorClass().getMission().getCode());
		restProcessor.setProcessorName(modelProcessor.getProcessorClass().getProcessorName());
		restProcessor.setProcessorVersion(modelProcessor.getProcessorVersion());
		restProcessor.setIsTest(modelProcessor.getIsTest());
		restProcessor.setMinDiskSpace(Long.valueOf(modelProcessor.getMinDiskSpace()));
		restProcessor.setMaxTime(Long.valueOf(modelProcessor.getMaxTime()));
		restProcessor.setSensingTimeFlag(modelProcessor.getSensingTimeFlag());
		restProcessor.setDockerImage(modelProcessor.getDockerImage());
		restProcessor.setDockerRunParameters(modelProcessor.getDockerRunParameters());
		
		for (ConfiguredProcessor configuredProcessor: modelProcessor.getConfiguredProcessors()) {
			restProcessor.getConfiguredProcessors().add(configuredProcessor.getIdentifier());
		}
		
		for (Task task: modelProcessor.getTasks()) {
			restProcessor.getTasks().add(TaskUtil.toRestTask(task));
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
		if (logger.isTraceEnabled()) logger.trace(">>> toModelProcessorClass({})", (null == restProcessor ? "MISSING" : restProcessor.getProcessorName()));

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
		modelProcessor.setIsTest(restProcessor.getIsTest());
		modelProcessor.setMinDiskSpace(restProcessor.getMinDiskSpace().intValue());
		modelProcessor.setMaxTime(restProcessor.getMaxTime().intValue());
		modelProcessor.setSensingTimeFlag(restProcessor.getSensingTimeFlag());
		modelProcessor.setDockerImage(restProcessor.getDockerImage());
		modelProcessor.setDockerRunParameters(restProcessor.getDockerRunParameters());
		
		return modelProcessor;
	}
}
