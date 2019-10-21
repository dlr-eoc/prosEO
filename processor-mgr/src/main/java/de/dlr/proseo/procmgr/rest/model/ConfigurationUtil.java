/**
 * ConfigurationUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfigurationFile;

/**
 * Utility methods for (processor) configurations, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ConfigurationUtil {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);
	
	/**
	 * Convert a prosEO model configuration into a REST configuration
	 * 
	 * @param modelConfiguration the prosEO model configuration
	 * @return an equivalent REST configuration or null, if no model configuration was given
	 */
	public static de.dlr.proseo.procmgr.rest.model.Configuration toRestConfiguration(Configuration modelConfiguration) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestConfiguration({})", (null == modelConfiguration ? "MISSING" : modelConfiguration.getId()));

		if (null == modelConfiguration)
			return null;
		
		de.dlr.proseo.procmgr.rest.model.Configuration restConfiguration = new de.dlr.proseo.procmgr.rest.model.Configuration();
		
		restConfiguration.setId(modelConfiguration.getId());
		restConfiguration.setVersion(Long.valueOf(modelConfiguration.getVersion()));
		restConfiguration.setMissionCode(modelConfiguration.getProcessorClass().getMission().getCode());
		restConfiguration.setProcessorName(modelConfiguration.getProcessorClass().getProcessorName());
		restConfiguration.setConfigurationVersion(modelConfiguration.getConfigurationVersion());
		restConfiguration.setDockerRunParameters(modelConfiguration.getDockerRunParameters());
		
		for (ConfiguredProcessor configuredProcessor: modelConfiguration.getConfiguredProcessors()) {
			restConfiguration.getConfiguredProcessors().add(configuredProcessor.getIdentifier());
		}
		
		for (String paramKey: modelConfiguration.getDynProcParameters().keySet()) {
			restConfiguration.getDynProcParameters().add(
				new de.dlr.proseo.procmgr.rest.model.Parameter(paramKey,
						modelConfiguration.getDynProcParameters().get(paramKey).getParameterType().toString(),
						modelConfiguration.getDynProcParameters().get(paramKey).getParameterValue()));
		}
		
		for (ConfigurationFile configurationFile: modelConfiguration.getConfigurationFiles()) {
			restConfiguration.getConfigurationFiles().add(
				new de.dlr.proseo.procmgr.rest.model.Object(configurationFile.getFileVersion(), configurationFile.getFileName()));
		}
		
		return restConfiguration;
	}
	
	/**
	 * Convert a REST configuration into a prosEO model configuration (scalar and embedded attributes only, no object references)
	 * 
	 * @param restConfiguration the REST configuration
	 * @return a (roughly) equivalent model configuration or null, if no REST configuration was given
	 * @throws IllegalArgumentException if the REST configuration violates syntax rules for date, enum or numeric values
	 */
	public static Configuration toModelConfiguration(de.dlr.proseo.procmgr.rest.model.Configuration restConfiguration) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelConfiguration({})", (null == restConfiguration ? "MISSING" : 
				restConfiguration.getProcessorName() + "/" + restConfiguration.getConfigurationVersion()));

		if (null == restConfiguration)
			return null;
		
		Configuration modelConfiguration = new Configuration();
		
		if (null != restConfiguration.getId() && 0 != restConfiguration.getId()) {
			modelConfiguration.setId(restConfiguration.getId());
			while (modelConfiguration.getVersion() < restConfiguration.getVersion()) {
				modelConfiguration.incrementVersion();
			} 
		}
		modelConfiguration.setConfigurationVersion(restConfiguration.getConfigurationVersion());
		modelConfiguration.setDockerRunParameters(restConfiguration.getDockerRunParameters());
		
		for (de.dlr.proseo.procmgr.rest.model.Object configurationFile: restConfiguration.getConfigurationFiles()) {
			ConfigurationFile modelConfigurationFile = new ConfigurationFile();
			modelConfigurationFile.setFileName(configurationFile.getFileName());
			modelConfigurationFile.setFileVersion(configurationFile.getFileVersion());
			modelConfiguration.getConfigurationFiles().add(modelConfigurationFile);
		}
		
		for (de.dlr.proseo.procmgr.rest.model.Parameter restDynProcParam: restConfiguration.getDynProcParameters()) {
			Parameter modelDynProcParam = new Parameter();
			modelDynProcParam.init(ParameterType.valueOf(restDynProcParam.getParameterType()), restDynProcParam.getParameterValue());
			modelConfiguration.getDynProcParameters().put(restDynProcParam.getKey(), modelDynProcParam);
		}
		
		return modelConfiguration;
	}
}
