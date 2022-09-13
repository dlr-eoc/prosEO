package de.dlr.proseo.ordermgr.rest.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
 * Build REST objects
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class RestUtil {


	/**
	 * Build RestJob of Job
	 * @param job
	 * @return RestJob
	 */
	@Transactional
	public static RestJob createRestJob(Job job, Boolean logs) {
		RestJob rj = new RestJob();
		if (job != null) {
			rj.setId(Long.valueOf(job.getId()));
			if (job.getJobState() != null) {
				rj.setJobState(JobState.valueOf(job.getJobState().toString()));
			}
			if (job.getOrbit() == null) {
				rj.setOrbit(null);
			} else {
				rj.setOrbit(OrbitUtil.toRestOrbit(job.getOrbit()));
			}
			List<RestJobStep> jobSteps = new ArrayList<RestJobStep>();
			for (JobStep js : job.getJobSteps()) {
				jobSteps.add(RestUtil.createRestJobStep(js, logs));
			}
			rj.setJobSteps(jobSteps);
			if (job.getPriority() != null) {
				rj.setPriority(Long.valueOf(job.getPriority()));
			}
			if (job.getHasFailedJobSteps() != null) {
				rj.setHasFailedJobSteps(job.getHasFailedJobSteps());
			}
			if (job.getProcessingFacility() != null) {
				rj.setProcessingFacilityName(job.getProcessingFacility().getName());
			}
			if (job.getStartTime() != null) {
				rj.setStartTime(Date.from(job.getStartTime()));
			}
			if (job.getStopTime() != null) {
				rj.setStopTime(Date.from(job.getStopTime()));
			}
			rj.setVersion(Long.valueOf(job.getVersion()));

			if (job.getProcessingOrder() != null) {
				rj.setOrderIdentifier(job.getProcessingOrder().getIdentifier());
			}
		}
		return rj;
	}

	/**
	 * Build RestJobStep of JobStep
	 * 
	 * @param js
	 * @return RestJobStep
	 */
	@Transactional
	public static RestJobStep createRestJobStep(JobStep js, Boolean logs) {
		RestJobStep pjs = new RestJobStep();
		if (js != null) {
			pjs.setId(Long.valueOf(js.getId()));
			pjs.setName(OrderManager.jobNamePrefix + js.getId());
			if (js.getJobStepState() != null) {
				pjs.setJobStepState(JobStepState.valueOf(js.getJobStepState().toString()));
			}
			pjs.setVersion((long) js.getVersion());
			pjs.setProcessingMode(js.getProcessingMode());
			pjs.setOutputParameters(RestUtil.createRestParameterList(js.getOutputParameters()));
			if (js.getProcessingStartTime() != null) { 
				pjs.setProcessingStartTime(Date.from(js.getProcessingStartTime()));
			}
			if (js.getProcessingCompletionTime() != null) { 
				pjs.setProcessingCompletionTime(Date.from(js.getProcessingCompletionTime()));
			}

			if (logs && js.getProcessingStdOut() != null) {
				pjs.setProcessingStdOut(js.getProcessingStdOut());
			}
			if (logs && js.getProcessingStdErr() != null) {
				pjs.setProcessingStdErr(js.getProcessingStdErr());
			}
			if (js.getJobOrderFilename() != null) {
				pjs.setJobOrderFilename(js.getJobOrderFilename());
			}
			pjs.setStderrLogLevel(StderrLogLevel.fromValue(js.getStderrLogLevel().toString()));
			pjs.setStdoutLogLevel(StdoutLogLevel.fromValue(js.getStdoutLogLevel().toString()));
			pjs.setIsFailed(js.getIsFailed() == null ? false : js.getIsFailed());
			pjs.setJobId(js.getJob() == null ? null : js.getJob().getId());
			if (js.getOutputProduct() != null && js.getOutputProduct().getProductClass() != null && js.getOutputProduct().getProductClass().getProductType() != null) {
				pjs.setOutputProductClass(js.getOutputProduct().getProductClass().getProductType());
			}
			for (ProductQuery pq : js.getInputProductQueries()) {
				String pt = pq.getRequestedProductClass().getProductType();
				if (!pjs.getInputProductClasses().contains(pt)) {
					pjs.getInputProductClasses().add(pt);
				}
			}
			if (js.getOutputProduct() != null) {
				pjs.setOutputProduct(js.getOutputProduct().getId());
				if (js.getOutputProduct().getConfiguredProcessor() != null) {
					pjs.setConfiguredProcessor(js.getOutputProduct().getConfiguredProcessor().getIdentifier());
				}
			}
			pjs.setOrderIdentifier(js.getJob().getProcessingOrder().getIdentifier());
			pjs.setOrderId(js.getJob().getProcessingOrder().getId());
		} 
		return pjs;
	}

	/**
	 * Build a List of RestParameters out of a Map of Parameters
	 * @param paramMap
	 * @return List of RestParameters
	 */
	public static List<RestParameter> createRestParameterList(Map<String, Parameter> paramMap) {
		List<RestParameter> restParams = new ArrayList<RestParameter>();
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