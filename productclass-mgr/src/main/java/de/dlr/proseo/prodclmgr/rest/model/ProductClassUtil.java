/**
 * ProductClassUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest.model;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.ProductVisibility;

/**
 * Utility methods for product classes, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ProductClassUtil {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassUtil.class);
	
	/**
	 * Convert a prosEO model product class into a REST product class
	 * 
	 * @param modelProductClass the prosEO model product class
	 * @return an equivalent REST product class or null, if no model product class was given
	 */
	public static RestProductClass toRestProductClass(ProductClass modelProductClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestProductClass({})", (null == modelProductClass ? "MISSING" : modelProductClass.getProductType()));

		if (null == modelProductClass) {
			return null;
		}
		
		RestProductClass restProductClass = new RestProductClass();
		
		restProductClass.setId(modelProductClass.getId());
		restProductClass.setVersion(Long.valueOf(modelProductClass.getVersion()));
		if (null != modelProductClass.getMission()) {
			restProductClass.setMissionCode(modelProductClass.getMission().getCode());
		}
		restProductClass.setProductType(modelProductClass.getProductType());
		restProductClass.setTypeDescription(modelProductClass.getDescription());
		if (null != modelProductClass.getVisibility()) {
			restProductClass.setVisibility(modelProductClass.getVisibility().toString());
		}
		if (null != modelProductClass.getDefaultSlicingType()) {
			restProductClass.setDefaultSlicingType(modelProductClass.getDefaultSlicingType().toString());
		}
		if (null != modelProductClass.getDefaultSliceDuration()) {
			restProductClass.setDefaultSliceDuration(modelProductClass.getDefaultSliceDuration().getSeconds());
		}
		if (null != modelProductClass.getEnclosingClass()) {
			restProductClass.setEnclosingClass(modelProductClass.getEnclosingClass().getProductType());
		}
		if (null != modelProductClass.getProcessorClass()) {
			restProductClass.setProcessorClass(modelProductClass.getProcessorClass().getProcessorName());
		}
		for (ProductClass componentClass: modelProductClass.getComponentClasses()) {
			restProductClass.getComponentClasses().add(componentClass.getProductType());
		}
		for (SimpleSelectionRule simpleSelectionRule: modelProductClass.getRequiredSelectionRules()) {
			RestSimpleSelectionRule restSimpleSelectionRule = new RestSimpleSelectionRule();
			restSimpleSelectionRule.setId(simpleSelectionRule.getId());
			restSimpleSelectionRule.setVersion(Long.valueOf(simpleSelectionRule.getVersion()));
			restSimpleSelectionRule.setMode(simpleSelectionRule.getMode());
			restSimpleSelectionRule.setIsMandatory(simpleSelectionRule.getIsMandatory());
			restSimpleSelectionRule.setTargetProductClass(modelProductClass.getProductType());
			if (null != simpleSelectionRule.getSourceProductClass()) {
				restSimpleSelectionRule.setSourceProductClass(simpleSelectionRule.getSourceProductClass().getProductType());
			}
			for (ConfiguredProcessor configuredProcessor: simpleSelectionRule.getApplicableConfiguredProcessors()) {
				restSimpleSelectionRule.getApplicableConfiguredProcessors().add(configuredProcessor.getIdentifier());
			}
			for (String filterConditionKey: simpleSelectionRule.getFilterConditions().keySet()) {
				restSimpleSelectionRule.getFilterConditions().add(
						new RestParameter(filterConditionKey,
								simpleSelectionRule.getFilterConditions().get(filterConditionKey).getParameterType().toString(),
								simpleSelectionRule.getFilterConditions().get(filterConditionKey).getParameterValue().toString()));
			}
			for (SimplePolicy simplePolicy: simpleSelectionRule.getSimplePolicies()) {
				RestSimplePolicy restSimplePolicy = new RestSimplePolicy();
				restSimplePolicy.setId(simplePolicy.getId());
				restSimplePolicy.setVersion(Long.valueOf(simplePolicy.getVersion()));
				restSimplePolicy.setPolicyType(simplePolicy.getPolicyType().toString());
				restSimplePolicy.setDeltaTimeT0(new DeltaTimeT0(simplePolicy.getDeltaTimeT0().duration, simplePolicy.getDeltaTimeT0().unit.toString()));
				restSimplePolicy.setDeltaTimeT1(new DeltaTimeT1(simplePolicy.getDeltaTimeT1().duration, simplePolicy.getDeltaTimeT1().unit.toString()));
				restSimpleSelectionRule.getSimplePolicies().add(restSimplePolicy);
			}
			restProductClass.getSelectionRule().add(restSimpleSelectionRule);
		}
		
		return restProductClass;
	}
	
	/**
	 * Convert a REST product class into a prosEO model product class (scalar and embedded attributes only, no object references)
	 * 
	 * @param restProductClass the REST product class
	 * @return a (roughly) equivalent model product class
	 * @throws IllegalArgumentException if the REST product violates syntax rules for date, enum or numeric values
	 */
	public static ProductClass toModelProductClass(RestProductClass restProductClass) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelProductClass({})", (null == restProductClass ? "MISSING" : restProductClass.getProductType()));

		if (null == restProductClass) {
			return null;
		}
		
		ProductClass modelProductClass = new ProductClass();
		
		if (null != restProductClass.getId() && 0 != restProductClass.getId()) {
			modelProductClass.setId(restProductClass.getId());
			while (modelProductClass.getVersion() < restProductClass.getVersion()) {
				modelProductClass.incrementVersion();
			}
		}
		
		modelProductClass.setProductType(restProductClass.getProductType());
		modelProductClass.setDescription(restProductClass.getTypeDescription());
		if (null != restProductClass.getVisibility()) {
			modelProductClass.setVisibility(ProductVisibility.valueOf(restProductClass.getVisibility()));
		}
		if (null != restProductClass.getDefaultSlicingType()) {
			modelProductClass.setDefaultSlicingType(OrderSlicingType.valueOf(restProductClass.getDefaultSlicingType()));
		}
		if (null != restProductClass.getDefaultSliceDuration()) {
			modelProductClass.setDefaultSliceDuration(Duration.ofSeconds(restProductClass.getDefaultSliceDuration()));
		}
		
		return modelProductClass;
	}
}
