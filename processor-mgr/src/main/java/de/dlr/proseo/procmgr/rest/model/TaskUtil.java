/**
 * TaskUtil.java
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
 * Utility methods for processor tasks, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class TaskUtil {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(TaskUtil.class);
	
	/**
	 * Convert a prosEO model processor task into a REST processor task
	 * 
	 * @param modelTask the prosEO model processor task
	 * @return an equivalent REST processor task or null, if no model processor task was given
	 */
	public static RestTask toRestTask(Task modelTask) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestTask({})", (null == modelTask ? "MISSING" : modelTask.getId()));

		if (null == modelTask)
			return null;
		
		RestTask restTask = new RestTask();
		
		restTask.setId(modelTask.getId());
		restTask.setVersion(Long.valueOf(modelTask.getVersion()));
		restTask.setMissionCode(modelTask.getProcessor().getProcessorClass().getMission().getCode());
		restTask.setProcessorName(modelTask.getProcessor().getProcessorClass().getProcessorName());
		restTask.setProcessorVersion(modelTask.getProcessor().getProcessorVersion());
		restTask.setTaskName(modelTask.getTaskName());
		restTask.setTaskVersion(modelTask.getTaskVersion());
		restTask.setIsCritical(modelTask.getIsCritical());
		restTask.setCriticalityLevel(Long.valueOf(modelTask.getCriticalityLevel()));
		restTask.setNumberOfCpus(Long.valueOf(modelTask.getNumberOfCpus()));
		
		for (String breakpointFileName: modelTask.getBreakpointFileNames()) {
			restTask.getBreakpointFileNames().add(breakpointFileName);
		}
		
		return restTask;
	}
	
	/**
	 * Convert a REST processor task into a prosEO model processor task (scalar and embedded attributes only, no object references)
	 * 
	 * @param restTask the REST processor task
	 * @return a (roughly) equivalent model processor task or null, if no REST processor task was given
	 * @throws IllegalArgumentException if the REST processor task violates syntax rules for date, enum or numeric values
	 */
	public static Task toModelTask(RestTask restTask) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelTask({})", (null == restTask ? "MISSING" : restTask.getTaskName()));

		if (null == restTask)
			return null;
		
		Task modelTask = new Task();
		
		if (null != restTask.getId() && 0 != restTask.getId()) {
			modelTask.setId(restTask.getId());
			while (modelTask.getVersion() < restTask.getVersion()) {
				modelTask.incrementVersion();
			} 
		}
		modelTask.setTaskName(restTask.getTaskName());
		modelTask.setTaskVersion(restTask.getTaskVersion());
		modelTask.setIsCritical(restTask.getIsCritical());
		modelTask.setCriticalityLevel(restTask.getCriticalityLevel().intValue());
		modelTask.setNumberOfCpus(restTask.getNumberOfCpus().intValue());
		
		for (String breakpointFileName: restTask.getBreakpointFileNames()) {
			modelTask.getBreakpointFileNames().add(breakpointFileName);
		}
		
		return modelTask;
	}
}
