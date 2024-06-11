/**
 * RestUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.rest.model.JobState;
import de.dlr.proseo.model.rest.model.JobStepState;
import de.dlr.proseo.model.rest.model.RestId;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobGraph;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.RestJobStepGraph;
import de.dlr.proseo.model.rest.model.RestOrbit;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.rest.model.StderrLogLevel;
import de.dlr.proseo.model.rest.model.StdoutLogLevel;
import de.dlr.proseo.model.util.OrderUtil;
import de.dlr.proseo.planner.ProductionPlanner;

/**
 * Utility class to build REST objects.
 * 
 * This class provides static methods to convert domain model entities into their corresponding REST representations.
 * 
 * @author Ernst Melchinger
 */
@Component
public class RestUtil {

	/** A logger instance for this class */
	private static ProseoLogger logger = new ProseoLogger(RestUtil.class);

	/**
	 * Builds a RestOrder object from a ProcessingOrder.
	 * 
	 * @param order The ProcessingOrder
	 * @return RestOrder object representing the ProcessingOrder
	 */
	public static RestOrder createRestOrder(ProcessingOrder order) {
		RestOrder restOrder = new RestOrder();
		if (order != null) {
			restOrder = OrderUtil.toRestOrder(order);
		}
		return restOrder;
	}

	/**
	 * Builds a RestJob object from a Job.
	 * 
	 * @param job  The Job object
	 * @param logs Boolean indicating whether to include logs in the RestJob
	 * @return RestJob object representing the Job
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public static RestJob createRestJob(Job job, Boolean logs) {
		RestJob restJob = new RestJob();

		if (job != null) {
			restJob.setId(Long.valueOf(job.getId()));

			if (job.getJobState() != null) {
				restJob.setJobState(JobState.valueOf(job.getJobState().toString()));
			}

			restJob.setOrbit(RestUtil.createRestOrbit(job.getOrbit()));

			List<RestJobStep> jobSteps = new ArrayList<RestJobStep>();
			for (JobStep js : job.getJobSteps()) {
				jobSteps.add(RestUtil.createRestJobStep(js, logs));
			}
			restJob.setJobSteps(jobSteps);
			if (job.getPriority() != null) {
				restJob.setPriority(Long.valueOf(job.getPriority()));
			}

			if (job.getHasFailedJobSteps() != null) {
				restJob.setHasFailedJobSteps(job.getHasFailedJobSteps());
			}

			if (job.getProcessingFacility() != null) {
				restJob.setProcessingFacilityName(job.getProcessingFacility().getName());
			}

			if (job.getStartTime() != null) {
				restJob.setStartTime(Date.from(job.getStartTime()));
			}

			if (job.getStopTime() != null) {
				restJob.setStopTime(Date.from(job.getStopTime()));
			}

			restJob.setVersion(Long.valueOf(job.getVersion()));

			if (job.getProcessingOrder() != null) {
				restJob.setOrderIdentifier(job.getProcessingOrder().getIdentifier());
			}
		}

		return restJob;
	}

	/**
	 * Builds a RestJobGraph object from a Job.
	 * 
	 * @param job The Job object
	 * @return RestJobGraph object representing the Job
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public static RestJobGraph createRestJobGraph(Job job) {
		RestJobGraph restJobGraph = new RestJobGraph();

		if (job != null) {
			restJobGraph.setId(Long.valueOf(job.getId()));

			if (job.getJobState() != null) {
				restJobGraph.setJobState(JobState.valueOf(job.getJobState().toString()));
			}

			List<RestJobStepGraph> jobSteps = new ArrayList<RestJobStepGraph>();
			for (JobStep js : job.getJobSteps()) {
				jobSteps.add(RestUtil.createRestJobStepGraph(js));
			}
			restJobGraph.setJobSteps(jobSteps);
		}

		return restJobGraph;
	}

	/**
	 * Builds a RestOrbit object from an Orbit.
	 * 
	 * @param orbit The Orbit object
	 * @return RestOrbit object representing the Orbit
	 */
	public static RestOrbit createRestOrbit(Orbit orbit) {
		RestOrbit restOrbit = new RestOrbit();

		if (orbit != null) {
			restOrbit.setId(Long.valueOf(orbit.getId()));

			try {
				restOrbit.setSpacecraftCode(orbit.getSpacecraft().getCode());
				restOrbit.setMissionCode(orbit.getSpacecraft().getMission().getCode());
			} catch (Exception e) {
				logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);

				if (logger.isDebugEnabled())
					logger.debug("... exception stack trace: ", e);
			}

			restOrbit.setOrbitNumber(Long.valueOf(orbit.getOrbitNumber()));

			if (orbit.getStartTime() != null) {
				restOrbit.setStartTime(orbit.getStartTime().toString());
			}
			if (orbit.getStopTime() != null) {
				restOrbit.setStopTime(orbit.getStopTime().toString());
			}

			restOrbit.setVersion(Long.valueOf(orbit.getVersion()));
		}
		return restOrbit;
	}

	/**
	 * Builds a RestJobStep object from a JobStep.
	 * 
	 * @param jobStep The JobStep object
	 * @param logs    Boolean indicating whether to include logs in the RestJobStep
	 * @return RestJobStep object representing the JobStep
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public static RestJobStep createRestJobStep(JobStep jobStep, Boolean logs) {
		RestJobStep restJobStep = new RestJobStep();

		if (jobStep != null) {
			restJobStep.setId(Long.valueOf(jobStep.getId()));
			restJobStep.setName(ProductionPlanner.jobNamePrefix + jobStep.getId());

			if (jobStep.getJobStepState() != null) {
				restJobStep.setJobStepState(JobStepState.valueOf(jobStep.getJobStepState().toString()));
			}

			restJobStep.setVersion((long) jobStep.getVersion());

			restJobStep.setProcessingMode(jobStep.getProcessingMode());

			restJobStep.setOutputParameters(RestUtil.createRestParameterList(jobStep.getOutputParameters()));

			if (jobStep.getProcessingStartTime() != null) {
				restJobStep.setProcessingStartTime(Date.from(jobStep.getProcessingStartTime()));
			}
			if (jobStep.getProcessingCompletionTime() != null) {
				restJobStep.setProcessingCompletionTime(Date.from(jobStep.getProcessingCompletionTime()));
			}

			if (logs && jobStep.getProcessingStdOut() != null) {
				restJobStep.setProcessingStdOut(jobStep.getProcessingStdOut());
			}
			if (logs && jobStep.getProcessingStdErr() != null) {
				restJobStep.setProcessingStdErr(jobStep.getProcessingStdErr());
			}

			if (jobStep.getJobOrderFilename() != null) {
				restJobStep.setJobOrderFilename(jobStep.getJobOrderFilename());
			}

			restJobStep.setStderrLogLevel(StderrLogLevel.fromValue(jobStep.getStderrLogLevel().toString()));
			restJobStep.setStdoutLogLevel(StdoutLogLevel.fromValue(jobStep.getStdoutLogLevel().toString()));

			restJobStep.setIsFailed(jobStep.getIsFailed() == null ? false : jobStep.getIsFailed());

			restJobStep.setJobId(jobStep.getJob() == null ? null : jobStep.getJob().getId());

			if (jobStep.getOutputProduct() != null && jobStep.getOutputProduct().getProductClass() != null
					&& jobStep.getOutputProduct().getProductClass().getProductType() != null) {
				restJobStep.setOutputProductClass(jobStep.getOutputProduct().getProductClass().getProductType());
			}

			for (ProductQuery pq : jobStep.getInputProductQueries()) {
				String pt = pq.getRequestedProductClass().getProductType();
				if (!restJobStep.getInputProductClasses().contains(pt)) {
					restJobStep.getInputProductClasses().add(pt);
				}
			}

			if (jobStep.getOutputProduct() != null) {
				restJobStep.setOutputProduct(jobStep.getOutputProduct().getId());
				if (jobStep.getOutputProduct().getConfiguredProcessor() != null) {
					restJobStep.setConfiguredProcessor(jobStep.getOutputProduct().getConfiguredProcessor().getIdentifier());
				}
			}

			restJobStep.setOrderIdentifier(jobStep.getJob().getProcessingOrder().getIdentifier());
			restJobStep.setOrderId(jobStep.getJob().getProcessingOrder().getId());
		}

		return restJobStep;
	}

	/**
	 * Build RestJobStep of JobStep
	 * 
	 * @param jobStep
	 * @return RestJobStep
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public static RestJobStepGraph createRestJobStepGraph(JobStep jobStep) {
		RestJobStepGraph restJobStepGraph = new RestJobStepGraph();

		if (jobStep != null) {
			restJobStepGraph.setId(Long.valueOf(jobStep.getId()));

			if (jobStep.getJobStepState() != null) {
				restJobStepGraph.setJobStepState(JobStepState.valueOf(jobStep.getJobStepState().toString()));
			}

			if (jobStep.getOutputProduct() != null && jobStep.getOutputProduct().getProductClass() != null
					&& jobStep.getOutputProduct().getProductClass().getProductType() != null) {
				restJobStepGraph.setOutputProductClass(jobStep.getOutputProduct().getProductClass().getProductType());
			}

			List<JobStep> jobStepList = new ArrayList<JobStep>();
			jobStepList.addAll(jobStep.getJob().getJobSteps());

			for (ProductQuery productQuery : jobStep.getInputProductQueries()) {
				String productType = productQuery.getRequestedProductClass().getProductType();

				List<JobStep> pre = new ArrayList<JobStep>();
				for (JobStep preJobStep : jobStepList) {
					if (jobStepProduces(preJobStep.getOutputProduct().getProductClass(), productType)) {
						if (!pre.contains(preJobStep)) {
							RestId restId = new RestId();
							restId.setId(preJobStep.getId());
							restJobStepGraph.getPredecessors().add(restId);
						}
					}
				}
			}
		}

		return restJobStepGraph;
	}

	private static Boolean jobStepProduces(ProductClass productClass, String productType) {
		if (productClass.getProductType().equals(productType)) {
			return true;
		}

		for (ProductClass componentClass : productClass.getComponentClasses()) {
			if (jobStepProduces(componentClass, productType)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Build a List of RestParameters out of a Map of Parameters
	 * 
	 * @param paramMap
	 * @return List of RestParameters
	 */
	public static List<RestParameter> createRestParameterList(Map<String, Parameter> paramMap) {
		List<RestParameter> restParams = new ArrayList<RestParameter>();

		if (paramMap != null) {
			for (Map.Entry<String, Parameter> entry : paramMap.entrySet()) {
				RestParameter restParameter = new RestParameter();
				restParameter.setKey(entry.getKey());
				restParameter.setParameterType(entry.getValue().getParameterType().toString());
				restParameter.setParameterValue(entry.getValue().getParameterValue());
				restParams.add(restParameter);
			}
		}

		return restParams;
	}

}