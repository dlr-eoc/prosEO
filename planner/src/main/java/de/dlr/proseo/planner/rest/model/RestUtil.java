/**
 * RestUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.rest.model.JobState;
import de.dlr.proseo.model.rest.model.JobStepState;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.RestOrbit;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.rest.model.StderrLogLevel;
import de.dlr.proseo.model.rest.model.StdoutLogLevel;
import de.dlr.proseo.model.util.OrderUtil;
import de.dlr.proseo.planner.ProductionPlanner;
/**
 * Build REST objects
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class RestUtil {


	/**
	 * Build RestOrder of ProcessingOrder
	 * 
	 * @param order Processing order
	 * @return RestOrder
	 */
	public static RestOrder createRestOrder(ProcessingOrder order) {
		RestOrder ro = new RestOrder();
		if (order != null) {
			ro = OrderUtil.toRestOrder(order);
		}
		return ro;
	}


	/**
	 * Build RestJob of Job
	 * @param job
	 * @return RestJob
	 */
	@Transactional
	public static RestJob createRestJob(Job job) {
		RestJob rj = new RestJob();
		if (job != null) {
			rj.setId(Long.valueOf(job.getId()));
			if (job.getJobState() != null) {
				rj.setJobState(JobState.valueOf(job.getJobState().toString()));
			}
			rj.setOrbit(RestUtil.createRestOrbit(job.getOrbit()));
			List<RestJobStep> jobSteps = new ArrayList<RestJobStep>();
			for (JobStep js : job.getJobSteps()) {
				jobSteps.add(RestUtil.createRestJobStep(js));
			}
			rj.setJobSteps(jobSteps);
			if (job.getPriority() != null) {
				rj.setPriority(Long.valueOf(job.getPriority()));
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
			rj.setFilterConditions(RestUtil.createRestParameterList(job.getFilterConditions()));
			rj.setOutputParameters(RestUtil.createRestParameterList(job.getOutputParameters()));
			if (job.getProcessingOrder() != null) {
				rj.setOrderIdentifier(job.getProcessingOrder().getIdentifier());
			}
		}
		return rj;
	}

	/**
	 * Build RestOrbit of Orbit
	 * 
	 * @param orbit
	 * @return RestOrbit
	 */
	public static RestOrbit createRestOrbit(Orbit orbit) {
		RestOrbit ro = new RestOrbit();
		if (orbit != null) {
			ro.setId(Long.valueOf(orbit.getId()));
			try {
				ro.setSpacecraftCode(orbit.getSpacecraft().getCode());
				ro.setMissionCode(orbit.getSpacecraft().getMission().getCode());
			} catch (Exception ex) {
				
			}
			ro.setOrbitNumber(Long.valueOf(orbit.getOrbitNumber()));
			if (orbit.getStartTime() != null) {
				ro.setStartTime(orbit.getStartTime().toString());
			}
			if (orbit.getStopTime() != null) {
				ro.setStopTime(orbit.getStopTime().toString());
			}
			ro.setVersion(Long.valueOf(orbit.getVersion()));
		}
		return ro;
	}

	/**
	 * Build RestJobStep of JobStep
	 * 
	 * @param js
	 * @return RestJobStep
	 */
	@Transactional
	public static RestJobStep createRestJobStep(JobStep js) {
		RestJobStep pjs = new RestJobStep();
		if (js != null) {
			pjs = new RestJobStep();
			pjs.setId(Long.valueOf(js.getId()));
			pjs.setName(ProductionPlanner.jobNamePrefix + js.getId());
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

			if (js.getProcessingStdOut() != null) {
				pjs.setProcessingStdOut(js.getProcessingStdOut());
			}
			if (js.getProcessingStdErr() != null) {
				pjs.setProcessingStdErr(js.getProcessingStdErr());
			}
			pjs.setStderrLogLevel(StderrLogLevel.fromValue(js.getStderrLogLevel().toString()));
			pjs.setStdoutLogLevel(StdoutLogLevel.fromValue(js.getStdoutLogLevel().toString()));
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
