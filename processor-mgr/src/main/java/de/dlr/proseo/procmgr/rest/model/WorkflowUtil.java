/**
 * WorkflowUtil.java
 * 
 * (c) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest.model;

import java.util.UUID;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ProcessorMgrMessage;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.WorkflowOption.WorkflowOptionType;

/**
 * Utility methods for workflows, i.e. for conversion between prosEO model and
 * REST workflows
 * 
 * @author Katharina Bassler
 */
public class WorkflowUtil {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(WorkflowUtil.class);

	/**
	 * Convert a prosEO model workflow into a REST workflow
	 * 
	 * @param modelWorkflow the prosEO model workflow
	 * @return an equivalent REST workflow or null, if no model processor workflow
	 *         was given
	 */
	public static RestWorkflow toRestWorkflow(Workflow modelWorkflow) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestWorkflow({})", (null == modelWorkflow ? "MISSING" : modelWorkflow.getId()));

		if (null == modelWorkflow)
			return null;

		RestWorkflow restWorkflow = new RestWorkflow();

		if (null != modelWorkflow.getName()) {
			restWorkflow.setName(modelWorkflow.getName());
		}
		if (0 != modelWorkflow.getId()) {
			restWorkflow.setId(modelWorkflow.getId());
		}
		if (0 != modelWorkflow.getVersion()) {
			restWorkflow.setVersion(Long.valueOf(modelWorkflow.getVersion()));
		}
		if (null != modelWorkflow.getWorkflowVersion()) {
			restWorkflow.setWorkflowVersion(modelWorkflow.getWorkflowVersion());
		}
		if (null != modelWorkflow.getUuid()) {
			restWorkflow.setUuid(modelWorkflow.getUuid().toString());
		}
		if (null != modelWorkflow.getConfiguredProcessor()) {
			restWorkflow.setConfiguredProcessor(modelWorkflow.getConfiguredProcessor().getIdentifier());
			restWorkflow.setMissionCode(
					modelWorkflow.getConfiguredProcessor().getProcessor().getProcessorClass().getMission().getCode());
		}
		if (null != modelWorkflow.getInputProductClass()) {
			restWorkflow.setInputProductClass(modelWorkflow.getInputProductClass().getProductType());
		}
		if (null != modelWorkflow.getOutputProductClass()) {
			restWorkflow.setOutputProductClass(modelWorkflow.getOutputProductClass().getProductType());
		}

		if (null != modelWorkflow.getWorkflowOptions() && !modelWorkflow.getWorkflowOptions().isEmpty()) {
			for (WorkflowOption option : modelWorkflow.getWorkflowOptions()) {
				RestWorkflowOption restOption = new RestWorkflowOption();

				if (null != option.getName()) {
					restOption.setName(option.getName());
				}
				if (null != option.getType()) {
					restOption.setOptionType(option.getType().toString());
				}
				if (null != option.getValueRange()) {
					restOption.setValueRange(option.getValueRange());
				}
				if (0 != option.getId()) {
					restOption.setId(option.getId());
				}
				if (0 != option.getVersion()) {
					restOption.setVersion((long) option.getVersion());
				}
				if (null != option.getDefaultValue()) {
					restOption.setDefaultValue(option.getDefaultValue());
				}
				if (null != modelWorkflow.getConfiguredProcessor()) {
					restOption.setMissionCode(modelWorkflow.getConfiguredProcessor().getProcessor().getProcessorClass()
							.getMission().getCode());
				}

				restOption.setWorkflowName(restWorkflow.getName());
				restWorkflow.getWorkflowOptions().add(restOption);
			}
		}

		return restWorkflow;
	}

	/**
	 * Convert a REST workflow into a prosEO model workflow (scalar and embedded
	 * attributes only, no object references)
	 * 
	 * @param restWorkflow the REST workflow
	 * @return a (roughly) equivalent model workflow or null, if no REST workflow
	 *         was given
	 * 
	 */
	public static Workflow toModelWorkflow(RestWorkflow restWorkflow) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelWorkflow({})", (null == restWorkflow ? "MISSING" : restWorkflow.getName()));

		if (null == restWorkflow)
			return null;

		Workflow modelWorkflow = new Workflow();

		if (null != restWorkflow.getName()) {
			modelWorkflow.setName(restWorkflow.getName());
		}
		if (null != restWorkflow.getId()) {
			modelWorkflow.setId(restWorkflow.getId());
		}
		if (0 != restWorkflow.getVersion()) {
			while (modelWorkflow.getVersion() < restWorkflow.getVersion()) {
				modelWorkflow.incrementVersion();
			}
		}
		if (null != restWorkflow.getWorkflowVersion()) {
			modelWorkflow.setWorkflowVersion(restWorkflow.getWorkflowVersion());
		}
		if (null != restWorkflow.getUuid()) {
			modelWorkflow.setUuid(UUID.fromString(restWorkflow.getUuid()));
		}

		if (null != restWorkflow.getWorkflowOptions() && !restWorkflow.getWorkflowOptions().isEmpty()) {
			for (RestWorkflowOption option : restWorkflow.getWorkflowOptions()) {
				WorkflowOption modelOption = new WorkflowOption();

				if ((null != option.getMissionCode() && option.getMissionCode() != restWorkflow.getMissionCode())
						|| option.getWorkflowName() != restWorkflow.getName()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.WORKFLOW_OPTION_MISMATCH));
				}
				if (null != option.getName()) {
					modelOption.setName(option.getName());
				}
				if (null != option.getOptionType()) {
					modelOption.setType(WorkflowOptionType.get(option.getOptionType().toLowerCase()));
				}
				if (null != option.getValueRange()) {
					modelOption.setValueRange(option.getValueRange());
				}
				if (0 != option.getId()) {
					modelOption.setId(option.getId());
				}
				if (null != option.getDefaultValue()) {
					modelOption.setDefaultValue(option.getDefaultValue());
				}

				modelOption.setWorkflow(modelWorkflow);
				modelWorkflow.getWorkflowOptions().add(modelOption);
			}
		}

		return modelWorkflow;
	}
}
