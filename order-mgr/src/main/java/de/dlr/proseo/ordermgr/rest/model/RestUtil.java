/**
 * OrderManager.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.rest.model.JobState;
import de.dlr.proseo.model.rest.model.JobStepState;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.rest.model.StderrLogLevel;
import de.dlr.proseo.model.rest.model.StdoutLogLevel;
import de.dlr.proseo.ordermgr.OrderManager;

/**
 * A utility class for converting REST jobs, job steps and parameter lists from their model equivalent.
 *
 *
 * @author Ernst Melchinger
 */
@Component
public class RestUtil {

	/**
	 * Convert a model job to a REST job
	 *
	 * @param modelJob the model job
	 * @param logs     whether or not to include logs in the REST job step
	 * @return the REST job
	 */
	public static RestJob createRestJob(Job modelJob, Boolean logs) {

		RestJob rj = new RestJob();

		if (modelJob != null) {
			rj.setId(Long.valueOf(modelJob.getId()));

			if (modelJob.getJobState() != null) {
				rj.setJobState(JobState.valueOf(modelJob.getJobState().toString()));
			}
			if (modelJob.getOrbit() == null) {
				rj.setOrbit(null);
			} else {
				rj.setOrbit(OrbitUtil.toRestOrbit(modelJob.getOrbit()));
			}

			List<RestJobStep> jobSteps = new ArrayList<>();
			for (JobStep js : modelJob.getJobSteps()) {
				jobSteps.add(RestUtil.createRestJobStep(js, logs));
			}
			
			Collections.sort(jobSteps, Comparator.comparing(RestJobStep::getId));
			rj.setJobSteps(jobSteps);

			if (modelJob.getPriority() != null) {
				rj.setPriority(Long.valueOf(modelJob.getPriority()));
			}
			if (modelJob.getHasFailedJobSteps() != null) {
				rj.setHasFailedJobSteps(modelJob.getHasFailedJobSteps());
			}
			if (modelJob.getProcessingFacility() != null) {
				rj.setProcessingFacilityName(modelJob.getProcessingFacility().getName());
			}
			if (modelJob.getStartTime() != null) {
				rj.setStartTime(Date.from(modelJob.getStartTime()));
			}
			if (modelJob.getStopTime() != null) {
				rj.setStopTime(Date.from(modelJob.getStopTime()));
			}
			rj.setVersion(Long.valueOf(modelJob.getVersion()));

			if (modelJob.getProcessingOrder() != null) {
				rj.setOrderIdentifier(modelJob.getProcessingOrder().getIdentifier());
			}

		}

		return rj;
	}

	/**
	 * Creates a REST job step based on the provided model job step.
	 *
	 * @param modelJobStep The model job step to convert.
	 * @param logs         Determines whether to include logs in the REST job step.
	 * @return The REST job step.
	 */
	public static RestJobStep createRestJobStep(JobStep modelJobStep, Boolean logs) {
		RestJobStep pjs = new RestJobStep();
		if (modelJobStep != null) {
			pjs.setId(Long.valueOf(modelJobStep.getId()));
			pjs.setName(OrderManager.jobNamePrefix + modelJobStep.getId());

			if (modelJobStep.getJobStepState() != null) {
				pjs.setJobStepState(JobStepState.valueOf(modelJobStep.getJobStepState().toString()));
			}

			pjs.setVersion((long) modelJobStep.getVersion());
			pjs.setProcessingMode(modelJobStep.getProcessingMode());
			pjs.setOutputParameters(RestUtil.createRestParameterList(modelJobStep.getOutputParameters()));

			if (modelJobStep.getProcessingStartTime() != null) {
				pjs.setProcessingStartTime(Date.from(modelJobStep.getProcessingStartTime()));
			}

			if (modelJobStep.getProcessingCompletionTime() != null) {
				pjs.setProcessingCompletionTime(Date.from(modelJobStep.getProcessingCompletionTime()));
			}

			if (logs && modelJobStep.getProcessingStdOut() != null) {
				pjs.setProcessingStdOut(modelJobStep.getProcessingStdOut());
			}

			if (logs && modelJobStep.getProcessingStdErr() != null) {
				pjs.setProcessingStdErr(modelJobStep.getProcessingStdErr());
			}

			if (modelJobStep.getJobOrderFilename() != null) {
				pjs.setJobOrderFilename(modelJobStep.getJobOrderFilename());
			}

			pjs.setStderrLogLevel(StderrLogLevel.fromValue(modelJobStep.getStderrLogLevel().toString()));
			pjs.setStdoutLogLevel(StdoutLogLevel.fromValue(modelJobStep.getStdoutLogLevel().toString()));
			pjs.setIsFailed(modelJobStep.getIsFailed() == null ? false : modelJobStep.getIsFailed());
			pjs.setJobId(modelJobStep.getJob() == null ? null : modelJobStep.getJob().getId());

			if (modelJobStep.getOutputProduct() != null && modelJobStep.getOutputProduct().getProductClass() != null
					&& modelJobStep.getOutputProduct().getProductClass().getProductType() != null) {
				pjs.setOutputProductClass(modelJobStep.getOutputProduct().getProductClass().getProductType());
			}

			for (ProductQuery pq : modelJobStep.getInputProductQueries()) {
				String pt = pq.getRequestedProductClass().getProductType();
				if (!pjs.getInputProductClasses().contains(pt)) {
					pjs.getInputProductClasses().add(pt);
				}
			}
			Collections.sort(pjs.getInputProductClasses(), (o1, o2) -> {
				return o1.compareTo(o2);
			});

			if (modelJobStep.getOutputProduct() != null) {
				pjs.setOutputProduct(modelJobStep.getOutputProduct().getId());
				if (modelJobStep.getOutputProduct().getConfiguredProcessor() != null) {
					pjs.setConfiguredProcessor(modelJobStep.getOutputProduct().getConfiguredProcessor().getIdentifier());
				}
			}

			pjs.setOrderIdentifier(modelJobStep.getJob().getProcessingOrder().getIdentifier());
			pjs.setOrderId(modelJobStep.getJob().getProcessingOrder().getId());
		}

		return pjs;
	}

	/**
	 * Convert a model parameter list to list of REST parameters
	 *
	 * @param paramMap a list of model parameters
	 * @return a list of REST parameters
	 */
	public static List<RestParameter> createRestParameterList(Map<String, Parameter> paramMap) {
		List<RestParameter> restParams = new ArrayList<>();
		if (paramMap != null) {
			for (Map.Entry<String, Parameter> entry : paramMap.entrySet()) {
				RestParameter rp = new RestParameter();
				rp.setKey(entry.getKey());
				rp.setParameterType(entry.getValue().getParameterType().toString());
				rp.setParameterValue(entry.getValue().getParameterValue());
				restParams.add(rp);
			}
		}
		return restParams;
	}

}