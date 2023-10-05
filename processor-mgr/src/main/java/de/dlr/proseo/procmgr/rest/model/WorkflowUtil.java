/**
 * WorkflowUtil.java
 *
 * (c) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest.model;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.UUID;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProcessorMgrMessage;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.WorkflowOption.WorkflowOptionType;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility methods for workflows, i.e. for conversion between prosEO model and REST workflows
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
	 * @return an equivalent REST workflow or null, if no model processor workflow was given
	 */
	public static RestWorkflow toRestWorkflow(Workflow modelWorkflow) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestWorkflow({})", (null == modelWorkflow ? "MISSING" : modelWorkflow.getId()));

		if (null == modelWorkflow)
			return null;

		RestWorkflow restWorkflow = new RestWorkflow();

		if (0 != modelWorkflow.getId()) {
			restWorkflow.setId(modelWorkflow.getId());
		}
		if (0 != modelWorkflow.getVersion()) {
			restWorkflow.setVersion(Long.valueOf(modelWorkflow.getVersion()));
		}
		if (null != modelWorkflow.getMission()) {
			restWorkflow.setMissionCode(modelWorkflow.getMission().getCode());
		}
		if (null != modelWorkflow.getName()) {
			restWorkflow.setName(modelWorkflow.getName());
		}
		if (null != modelWorkflow.getWorkflowVersion()) {
			restWorkflow.setWorkflowVersion(modelWorkflow.getWorkflowVersion());
		}
		if (null != modelWorkflow.getDescription()) {
			restWorkflow.setDescription(modelWorkflow.getDescription());
		}
		if (null != modelWorkflow.getUuid()) {
			restWorkflow.setUuid(modelWorkflow.getUuid().toString());
		}
		if (null != modelWorkflow.getEnabled()) {
			restWorkflow.setEnabled(modelWorkflow.getEnabled());
		}
		if (null != modelWorkflow.getConfiguredProcessor()) {
			restWorkflow.setConfiguredProcessor(modelWorkflow.getConfiguredProcessor().getIdentifier());
			restWorkflow
				.setMissionCode(modelWorkflow.getConfiguredProcessor().getProcessor().getProcessorClass().getMission().getCode());
		}
		if (null != modelWorkflow.getInputProductClass()) {
			restWorkflow.setInputProductClass(modelWorkflow.getInputProductClass().getProductType());
		}
		if (null != modelWorkflow.getOutputProductClass()) {
			restWorkflow.setOutputProductClass(modelWorkflow.getOutputProductClass().getProductType());
		}
		if (null != modelWorkflow.getOutputFileClass()) {
			restWorkflow.setOutputFileClass(modelWorkflow.getOutputFileClass());
		}
		if (null != modelWorkflow.getProcessingMode()) {
			restWorkflow.setProcessingMode(modelWorkflow.getProcessingMode());
		}
		if (null != modelWorkflow.getSliceDuration()) {
			restWorkflow.setSliceDuration(modelWorkflow.getSliceDuration().toSeconds());
		}
		if (null != modelWorkflow.getSliceOverlap()) {
			restWorkflow.setSliceOverlap(modelWorkflow.getSliceOverlap().toSeconds());
		}
		if (null != modelWorkflow.getSlicingType()) {
			restWorkflow.setSlicingType(modelWorkflow.getSlicingType().toString());
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
					restOption.setValueRange(new ArrayList<String>());
					for (String str : option.getValueRange()) {
						restOption.getValueRange().add(str);
					}
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
					restOption.setMissionCode(
							modelWorkflow.getConfiguredProcessor().getProcessor().getProcessorClass().getMission().getCode());
				}
				if (null != option.getDescription()) {
					restOption.setDescription(option.getDescription());
				}

				restOption.setWorkflowName(restWorkflow.getName());
				restWorkflow.getWorkflowOptions().add(restOption);
			}
		}

		if (null != modelWorkflow.getClassOutputParameters() && !modelWorkflow.getClassOutputParameters().isEmpty()) {
			modelWorkflow.getClassOutputParameters().forEach((productClass, parameter) -> {
				RestClassOutputParameter restClassParam = new RestClassOutputParameter();

				if (null != productClass) {
					restClassParam.setProductClass(productClass.getProductType());
				}
				if (null != parameter) {
					parameter.getOutputParameters().forEach((key, param) -> {
						RestParameter restParam = new RestParameter();
						restParam.setKey(key);
						restParam.setParameterType(param.getParameterType().toString());
						restParam.setParameterValue(param.getParameterValue());
						restClassParam.getOutputParameters().add(restParam);
					});
				}
				restWorkflow.getClassOutputParameters().add(restClassParam);
			});
		}

		if (null != modelWorkflow.getOutputParameters() && !modelWorkflow.getOutputParameters().isEmpty()) {
			modelWorkflow.getOutputParameters().forEach((key, param) -> {
				RestParameter restParam = new RestParameter();
				restParam.setKey(key);
				restParam.setParameterType(param.getParameterType().toString());
				restParam.setParameterValue(param.getParameterValue());
				restWorkflow.getOutputParameters().add(restParam);
			});
		}

		if (null != modelWorkflow.getInputFilters() && !modelWorkflow.getInputFilters().isEmpty()) {
			modelWorkflow.getInputFilters().forEach((productClass, filter) -> {
				RestInputFilter restFilter = new RestInputFilter();
				restFilter.setProductClass(productClass.getProductType());

				filter.getFilterConditions().forEach((key, param) -> {
					RestParameter restParam = new RestParameter();
					restParam.setKey(key);
					restParam.setParameterType(param.getParameterType().toString());
					restParam.setParameterValue(param.getParameterValue());
					restFilter.getFilterConditions().add(restParam);
				});
				restWorkflow.getInputFilters().add(restFilter);
			});
		}

		return restWorkflow;
	}

	/**
	 * Convert a REST workflow into a prosEO model workflow (scalar and embedded attributes only, no object references)
	 *
	 * @param restWorkflow the REST workflow
	 * @return a (roughly) equivalent model workflow or null, if no REST workflow was given
	 *
	 */
	public static Workflow toModelWorkflow(RestWorkflow restWorkflow) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelWorkflow({})", (null == restWorkflow ? "MISSING" : restWorkflow.getName()));

		if (null == restWorkflow)
			return null;

		Workflow modelWorkflow = new Workflow();

		if (null != restWorkflow.getId()) {
			modelWorkflow.setId(restWorkflow.getId());
		}
		if (null != restWorkflow.getVersion() && 0 != restWorkflow.getVersion()) {
			while (modelWorkflow.getVersion() < restWorkflow.getVersion()) {
				modelWorkflow.incrementVersion();
			}
		}
		if (null != restWorkflow.getName()) {
			modelWorkflow.setName(restWorkflow.getName());
		}
		if (null != restWorkflow.getWorkflowVersion()) {
			modelWorkflow.setWorkflowVersion(restWorkflow.getWorkflowVersion());
		}
		if (null != restWorkflow.getDescription()) {
			modelWorkflow.setDescription(restWorkflow.getDescription());
		}
		if (null != restWorkflow.getUuid()) {
			modelWorkflow.setUuid(UUID.fromString(restWorkflow.getUuid()));
		}
		if (null != restWorkflow.getEnabled()) {
			modelWorkflow.setEnabled(restWorkflow.getEnabled());
		}
		if (null != restWorkflow.getOutputFileClass()) {
			modelWorkflow.setOutputFileClass(restWorkflow.getOutputFileClass());
		}
		if (null != restWorkflow.getProcessingMode()) {
			modelWorkflow.setProcessingMode(restWorkflow.getProcessingMode());
		}
		if (null != restWorkflow.getSliceDuration()) {
			modelWorkflow.setSliceDuration(Duration.ofSeconds(restWorkflow.getSliceDuration()));
		}
		if (null != restWorkflow.getSliceOverlap()) {
			modelWorkflow.setSliceOverlap(Duration.ofSeconds(restWorkflow.getSliceOverlap()));
		}
		if (null != restWorkflow.getSlicingType()) {
			modelWorkflow.setSlicingType(OrderSlicingType.valueOf(restWorkflow.getSlicingType()));
		}

		if (null != restWorkflow.getWorkflowOptions() && !restWorkflow.getWorkflowOptions().isEmpty()) {
			for (RestWorkflowOption option : restWorkflow.getWorkflowOptions()) {
				WorkflowOption modelOption = new WorkflowOption();

				if (null == option.getMissionCode()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In workflow option: missionCode"));
				}
				if (null == option.getWorkflowName()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In workflow option: workflowName"));
				}
				if (null == option.getName()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In workflow option: name"));
				}

				if ((null != option.getMissionCode() && !option.getMissionCode().equals(restWorkflow.getMissionCode()))
						|| (null != option.getWorkflowName() && !option.getWorkflowName().equals(restWorkflow.getName()))) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.WORKFLOW_OPTION_MISMATCH, option.getMissionCode(),
									option.getWorkflowName(), restWorkflow.getMissionCode(), restWorkflow.getName()));
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
				if (null != option.getId() && 0 != option.getId()) {
					modelOption.setId(option.getId());
				}
				if (null != option.getDefaultValue()) {
					modelOption.setDefaultValue(option.getDefaultValue());
				}
				if (null != option.getDescription()) {
					modelOption.setDescription(option.getDescription());
				}

				modelOption.setWorkflow(modelWorkflow);
				modelWorkflow.getWorkflowOptions().add(modelOption);
			}
		}

		if (null != restWorkflow.getOutputParameters() && !restWorkflow.getOutputParameters().isEmpty()) {
			for (RestParameter restParam : restWorkflow.getOutputParameters()) {
				Parameter modelParam = new Parameter();

				restParam.setParameterType(restParam.getParameterType().toUpperCase());
				modelParam.setParameterType(ParameterType.valueOf(restParam.getParameterType()));

				if (null == restParam.getParameterValue()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For classOutputParameter creation, output parameter value"));
				}

				switch (restParam.getParameterType()) {
				case "BOOLEAN":
					if (!restParam.getParameterValue().equalsIgnoreCase("true")
							&& !restParam.getParameterValue().equalsIgnoreCase("false"))
						throw new IllegalArgumentException(
								logger.log(GeneralMessage.INVALID_PARAMETER_FORMAT, restParam.getParameterValue(), "BOOLEAN"));
					modelParam.setBooleanValue(Boolean.valueOf(restParam.getParameterValue()));
					modelParam.setParameterValue(restParam.getParameterValue());
					break;
				case "DOUBLE":
					try {
						modelParam.setDoubleValue(Double.valueOf(restParam.getParameterValue()));
						modelParam.setParameterValue(restParam.getParameterValue());
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								logger.log(GeneralMessage.INVALID_PARAMETER_FORMAT, restParam.getParameterValue(), "DOUBLE"));
					}
					break;
				case "INSTANT":
					try {
						modelParam.setInstantValue(OrbitTimeFormatter.parseDateTime(restParam.getParameterValue()));
						modelParam.setParameterValue(restParam.getParameterValue());
					} catch (DateTimeParseException e) {
						throw new IllegalArgumentException(
								logger.log(GeneralMessage.INVALID_PARAMETER_FORMAT, restParam.getParameterValue(), "INSTANT"));
					}
					break;
				case "INTEGER":
					try {
						modelParam.setIntegerValue(Integer.valueOf(restParam.getParameterValue()));
						modelParam.setParameterValue(restParam.getParameterValue());
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								logger.log(GeneralMessage.INVALID_PARAMETER_FORMAT, restParam.getParameterValue(), "DOUBLE"));
					}
					break;
				case "STRING":
					modelParam.setStringValue(restParam.getParameterValue());
					modelParam.setParameterValue(restParam.getParameterValue());
					break;
				default:
					throw new IllegalArgumentException(
							logger.log(GeneralMessage.INVALID_PARAMETER_FORMAT, restParam.getParameterValue(), "(any)"));
				}

				modelWorkflow.getOutputParameters().put(restParam.getKey(), modelParam);
			}
		}

		return modelWorkflow;
	}
}
