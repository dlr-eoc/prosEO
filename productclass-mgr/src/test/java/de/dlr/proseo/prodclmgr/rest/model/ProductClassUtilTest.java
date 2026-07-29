/**
 * ProductClassUtilTest.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.SimplePolicy.DeltaTime;
import de.dlr.proseo.model.SimplePolicy.PolicyType;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.enums.ParameterType;

/**
 * Test class for ProductClassUtil
 *
 * @author Dr. Thomas Bassler
 */
public class ProductClassUtilTest {

	private static final int TEST_PRODUCT_CLASS_VERSION = 42;
	private static final Long TEST_PRODUCT_CLASS_ID = 4711L;
	private static final String TEST_COMPONENT_PRODUCT_TYPE = "NPP_BD3";
	private static final String TEST_ENCLOSING_PRODUCT_TYPE = "L2FULL";
	/* Static test data */
	private static final String TEST_MISSION_CODE = "S5P";
	private static final String TEST_BASE_PRODUCT_TYPE = "L1B";
	private static final String TEST_PROCESSOR_NAME = "RAL L2";
	private static final String TEST_PROCESSOR_VERSION = "01.02.03";
	private static final String TEST_CONFIGURATION_VERSION = "2019-09-27";
	private static final String TEST_CONFIGURED_PROCESSOR = TEST_PROCESSOR_NAME + "/" + TEST_PROCESSOR_VERSION + "/"
			+ TEST_CONFIGURATION_VERSION;
	private static final DeltaTime TEST_DELTA_TIME_T1 = new DeltaTime(180, TimeUnit.MINUTES);
	private static final DeltaTime TEST_DELTA_TIME_T0 = new DeltaTime(4, TimeUnit.HOURS);
	private static final String TEST_PARAMETER_VALUE = "01";
	private static final String TEST_PARAMETER_KEY = "revision";
	private static final String TEST_MODE = "OFFL";
	private static final String TEST_PRODUCT_CLASS_DESCRIPTION = "Suomi NPP cloud cover";
	private static final String TEST_PRODUCT_TYPE = "L2__NPP___";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassUtilTest.class);

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.model.ProductClassUtil#toRestProductClass(de.dlr.proseo.model.ProductClass)}.
	 */
	@Test
	public final void testToRestProductClass() {
		// Create a mission
		Mission mission = new Mission();
		mission.setCode(TEST_MISSION_CODE);

		// Create base, enclosing and component product classes
		ProductClass baseProductClass = new ProductClass();
		baseProductClass.setMission(mission);
		baseProductClass.setProductType(TEST_BASE_PRODUCT_TYPE);
		ProductClass enclosingProductClass = new ProductClass();
		enclosingProductClass.setMission(mission);
		enclosingProductClass.setProductType(TEST_ENCLOSING_PRODUCT_TYPE);
		ProductClass componentProductClass = new ProductClass();
		componentProductClass.setMission(mission);
		componentProductClass.setProductType(TEST_COMPONENT_PRODUCT_TYPE);

		// Create a processor class
		ProcessorClass processorClass = new ProcessorClass();
		processorClass.setMission(mission);
		processorClass.setProcessorName(TEST_PROCESSOR_NAME);

		// Create a configured processor
		Processor processor = new Processor();
		processor.setProcessorClass(processorClass);
		processor.setProcessorVersion(TEST_PROCESSOR_VERSION);
		Configuration configuration = new Configuration();
		configuration.setConfigurationVersion(TEST_CONFIGURATION_VERSION);
		configuration.setProcessorClass(processorClass);
		ConfiguredProcessor configuredProcessor = new ConfiguredProcessor();
		configuredProcessor.setIdentifier(TEST_CONFIGURED_PROCESSOR);
		configuredProcessor.setProcessor(processor);
		configuredProcessor.setConfiguration(configuration);
		processor.getConfiguredProcessors().add(configuredProcessor);
		configuration.getConfiguredProcessors().add(configuredProcessor);

		// Create the new product class
		ProductClass productClass = new ProductClass();
		productClass.setId(TEST_PRODUCT_CLASS_ID);
		while (productClass.getVersion() < TEST_PRODUCT_CLASS_VERSION) {
			productClass.incrementVersion();
		}
		productClass.setMission(mission);
		productClass.setProductType(TEST_PRODUCT_TYPE);
		productClass.setDescription(TEST_PRODUCT_CLASS_DESCRIPTION);
		productClass.setProcessorClass(processorClass);
		productClass.setEnclosingClass(enclosingProductClass);
		productClass.getComponentClasses().add(componentProductClass);

		SimpleSelectionRule simpleSelectionRule = new SimpleSelectionRule();
		simpleSelectionRule.setTargetProductClass(productClass);
		simpleSelectionRule.setSourceProductClass(baseProductClass);
		simpleSelectionRule.setMode(TEST_MODE);
		simpleSelectionRule.setIsMandatory(true);
		simpleSelectionRule.getConfiguredProcessors().add(configuredProcessor);
		simpleSelectionRule.getFilterConditions()
			.put(TEST_PARAMETER_KEY, (new Parameter()).init(ParameterType.STRING, TEST_PARAMETER_VALUE));

		SimplePolicy simplePolicy = new SimplePolicy();
		simplePolicy.setPolicyType(PolicyType.LatestValCover);
		simplePolicy.setDeltaTimes(Arrays.asList(TEST_DELTA_TIME_T0, TEST_DELTA_TIME_T1));

		simpleSelectionRule.getSimplePolicies().add(simplePolicy);
		productClass.getRequiredSelectionRules().add(simpleSelectionRule);

		// Copy the model product class to a REST product class
		RestProductClass restProductClass = ProductClassUtil.toRestProductClass(productClass);
		logger.info("Created REST product class: " + restProductClass);

		// Check the result
		assertNotNull(restProductClass, "REST product class missing");
		assertEquals(TEST_PRODUCT_CLASS_ID, restProductClass.getId(), "Unexpected id:");
		assertEquals(TEST_PRODUCT_CLASS_VERSION, restProductClass.getVersion().intValue(), "Unexpected version:");
		assertEquals(TEST_MISSION_CODE, restProductClass.getMissionCode(), "Unexpected mission code:");
		assertEquals(TEST_PRODUCT_TYPE, restProductClass.getProductType(), "Unexpected product type:");
		assertEquals(TEST_PRODUCT_CLASS_DESCRIPTION, restProductClass.getTypeDescription(), "Unexpected description:");
		assertEquals(TEST_PROCESSOR_NAME, restProductClass.getProcessorClass(), "Unexpected processor class:");
		assertEquals(TEST_ENCLOSING_PRODUCT_TYPE, restProductClass.getEnclosingClass(), "Unexpected enclosing type:");
		assertNotNull(restProductClass.getComponentClasses(), "Component classes missing");
		assertEquals(productClass.getComponentClasses().size(), restProductClass.getComponentClasses().size(),
				"Unexpected number of component classes:");
		assertEquals(TEST_COMPONENT_PRODUCT_TYPE, restProductClass.getComponentClasses().get(0), "Unexpected component class:");
		assertNotNull(restProductClass.getSelectionRule(), "Selection rule missing");
		assertEquals(productClass.getRequiredSelectionRules().size(), restProductClass.getSelectionRule().size(),
				"Unexpected number of simple selection rules");

		RestSimpleSelectionRule restSelectionRule = restProductClass.getSelectionRule().get(0);
		assertEquals(TEST_PRODUCT_TYPE, restSelectionRule.getTargetProductClass(), "Unexpected target product class:");
		assertEquals(TEST_BASE_PRODUCT_TYPE, restSelectionRule.getSourceProductClass(), "Unexpected source product class:");
		assertEquals(TEST_MODE, restSelectionRule.getMode(), "Unexpected mode:");
		assertEquals(true, restSelectionRule.getIsMandatory(), "Unexpected 'mandatory' value:");

		assertNotNull(restSelectionRule.getConfiguredProcessors(), "Applicable processors missing");
		assertEquals(simpleSelectionRule.getConfiguredProcessors().size(), restSelectionRule.getConfiguredProcessors().size(),
				"Unexpected number of applicable processors:");
		assertEquals(TEST_CONFIGURED_PROCESSOR, restSelectionRule.getConfiguredProcessors().get(0),
				"Unexpected applicable configured processor:");

		assertNotNull(restSelectionRule.getFilterConditions(), "Filter conditions missing");
		assertEquals(simpleSelectionRule.getFilterConditions().size(), restSelectionRule.getFilterConditions().size(),
				"Unexpected number of filter conditions:");
		Parameter modelParameter = simpleSelectionRule.getFilterConditions().get(TEST_PARAMETER_KEY);
		RestParameter restParameter = restSelectionRule.getFilterConditions().get(0);
		assertEquals(TEST_PARAMETER_KEY, restParameter.getKey(), "Unexpected filter condition key:");
		assertEquals(modelParameter.getParameterType().toString(), restParameter.getParameterType(),
				"Unexpected filter condition type:");
		assertEquals(TEST_PARAMETER_VALUE, restParameter.getParameterValue(), "Unexpected filter condition value:");

		assertNotNull(restSelectionRule.getSimplePolicies(), "Simple policies missing");
		assertEquals(simpleSelectionRule.getSimplePolicies().size(), restSelectionRule.getSimplePolicies().size(),
				"Unexpected number of simple policies:");
		RestSimplePolicy restPolicy = restSelectionRule.getSimplePolicies().get(0);
		assertEquals(simplePolicy.getPolicyType().toString(), restPolicy.getPolicyType(), "Unexpected policy type:");
		assertEquals(simplePolicy.getDeltaTimeT0().duration, restPolicy.getDeltaTimeT0().duration.longValue(),
				"Unexpected delta time T0 duration:");
		assertEquals(simplePolicy.getDeltaTimeT0().unit.toString(), restPolicy.getDeltaTimeT0().unit,
				"Unexpected delta time T0 unit:");
		assertEquals(simplePolicy.getDeltaTimeT1().duration, restPolicy.getDeltaTimeT1().duration.longValue(),
				"Unexpected delta time T1 duration:");
		assertEquals(simplePolicy.getDeltaTimeT1().unit.toString(), restPolicy.getDeltaTimeT1().unit,
				"Unexpected delta time T1 unit:");

		logger.info("Test copy model to REST OK");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.model.ProductClassUtil#toModelProductClass(de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}.
	 */
	@Test
	public final void testToModelProductClass() {

		// Create a REST product class (scalar values and embedded objects only)
		RestProductClass restProductClass = new RestProductClass();
		restProductClass.setId(TEST_PRODUCT_CLASS_ID);
		restProductClass.setVersion(Long.valueOf(TEST_PRODUCT_CLASS_VERSION));
		restProductClass.setMissionCode(TEST_MISSION_CODE);
		restProductClass.setProductType(TEST_PRODUCT_TYPE);
		restProductClass.setTypeDescription(TEST_PRODUCT_CLASS_DESCRIPTION);

		// Copy to model product class
		ProductClass productClass = ProductClassUtil.toModelProductClass(restProductClass);
		logger.info("Created model product class:" + productClass);

		// Check the result
		assertNotNull(productClass, "Model product class missing");
		assertEquals(TEST_PRODUCT_CLASS_ID, productClass.getId(), "Unexpected id:");
		assertEquals(TEST_PRODUCT_CLASS_VERSION, productClass.getVersion(), "Unexpected version:");
		assertEquals(TEST_PRODUCT_TYPE, productClass.getProductType(), "Unexpected product type:");
		assertEquals(TEST_PRODUCT_CLASS_DESCRIPTION, productClass.getDescription(), "Unexpected description:");
		logger.info("Test copy REST to model OK");
	}

}
