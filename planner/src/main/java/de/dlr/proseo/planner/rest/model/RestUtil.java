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
import de.dlr.proseo.planner.ProductionPlanner;

@Component
public class RestUtil {


	public static RestOrder createRestOrder(ProcessingOrder order) {
		RestOrder ro = new RestOrder();
		if (order != null) {
			List<String> configuredProcessors = new ArrayList<String>();
			if (order.getRequestedConfiguredProcessors() != null) {
				for (ConfiguredProcessor cp : order.getRequestedConfiguredProcessors()) {
					configuredProcessors.add(cp.getIdentifier());
				}
			}
			ro.setConfiguredProcessors(configuredProcessors);
			if (order.getExecutionTime() != null) {
				ro.setExecutionTime(Date.from(order.getExecutionTime()));
			}
			ro.setFilterConditions(RestUtil.createRestParameterList(order.getFilterConditions()));
			ro.setId(Long.valueOf(order.getId()));
			ro.setIdentifier(order.getIdentifier());
			List<String> inputProductClasses = new ArrayList<String>();
			if (order.getInputProductClasses() != null) {
				for (ProductClass cp : order.getInputProductClasses()) {
					configuredProcessors.add(cp.getProductType());
				}
			}
			ro.setInputProductClasses(inputProductClasses);
			if (order.getMission() != null) {
				ro.setMissionCode(order.getMission().getCode());
			}
			ro.setOrderState(order.getOrderState().toString());
			ro.setOutputFileClass(order.getOutputFileClass());
			ro.setOutputParameters(RestUtil.createRestParameterList(order.getOutputParameters()));
			ro.setProcessingMode(order.getProcessingMode());
			if (order.getSliceDuration() != null) {
			ro.setSliceDuration(order.getSliceDuration().toMillis());
			}
			if (order.getSliceOverlap() != null) {
				ro.setSliceOverlap(order.getSliceOverlap().toMillis());
			}
			if (order.getSlicingType() != null) {
				ro.setSlicingType(order.getSlicingType().toString());
			}
			if (order.getStartTime() != null) {
				ro.setStartTime(Date.from(order.getStartTime()));
			}
			if (order.getStopTime() != null) {
				ro.setStopTime(Date.from(order.getStopTime()));
			}
			ro.setVersion(Long.valueOf(order.getVersion()));
			List<String> requestedProductClasses = new ArrayList<String>();
			if (order.getRequestedProductClasses() != null) {
				for (ProductClass cp : order.getRequestedProductClasses()) {
					requestedProductClasses.add(cp.getProductType());
				}
			}
			ro.setRequestedProductClasses(requestedProductClasses);
			
			ro.setOrbits(RestUtil.createRestOrbitQueries(order.getRequestedOrbits()));
		}
		return ro;
	}
	
	public static List<RestOrbitQuery> createRestOrbitQueries(List<Orbit> orbits) {
		List<RestOrbitQuery> restOrbits = new ArrayList<RestOrbitQuery>();
		if (orbits != null) {
			List<Long> orbNumber = new ArrayList<Long>();
			for (Orbit orbit : orbits) {
				orbNumber.add(orbit.getOrbitNumber().longValue());			
			}
			for (Orbit o : orbits) {
				RestOrbitQuery ro = new RestOrbitQuery();
				ro.setSpacecraftCode(o.getSpacecraft().getCode());
				ro.setOrbitNumberFrom(Collections.min(orbNumber));
				ro.setOrbitNumberTo(Collections.max(orbNumber));
				restOrbits.add(ro);
			}
		}
		return restOrbits;
	}
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
			rj.setOutputParameters(RestUtil.createRestParameterList(job.getOutputParameters()));
			if (job.getProcessingOrder() != null) {
				rj.setOrderIdentifier(job.getProcessingOrder().getIdentifier());
			}
		}
		return rj;
	}

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

@Transactional
	public static RestJobStep createRestJobStep(JobStep js) {
		RestJobStep pjs = new RestJobStep();
		if (js != null) {
			pjs = new RestJobStep();
			pjs.setId(Long.valueOf(js.getId()));
			pjs.setName(ProductionPlanner.jobNamePrefix + js.getId());
			if (js.getJobStepState() != null) {
				pjs.setJobStepState(de.dlr.proseo.planner.rest.model.JobStepState.valueOf(js.getJobStepState().toString()));
			}
			pjs.setVersion((long) js.getVersion());
			pjs.setProcessingMode(js.getProcessingMode());
			if (js.getProcessingStartTime() != null) { 
				pjs.setProcessingStartTime(Date.from(js.getProcessingStartTime()));
			}
			if (js.getProcessingCompletionTime() != null) { 
				pjs.setProcessingCompletionTime(Date.from(js.getProcessingCompletionTime()));
			}
			pjs.setProcessingStdOut(js.getProcessingStdOut());
			pjs.setProcessingStdErr(js.getProcessingStdErr());
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
