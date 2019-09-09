package de.dlr.proseo.planner.rest;


import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.rest.model.PlannerJobstep;
import de.dlr.proseo.planner.rest.model.PlannerPod;
import de.dlr.proseo.planner.rest.model.PlannerProcessingFacility;
import de.dlr.proseo.planner.rest.model.PodKube;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;

@Component
public class ProcessingfacilityControllerImpl implements ProcessingfacilityController{

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);

    /**
     * Get attached processing facilities
     * 
     */
	@Override
    public ResponseEntity<List<PlannerProcessingFacility>> getPlannerProcessingFacilities() {
		ProductionPlanner.updateKubeConfigs();
		if (ProductionPlanner.getKubeConfigs() != null) {
			List<PlannerProcessingFacility> l = new ArrayList<PlannerProcessingFacility>();
			for (de.dlr.proseo.planner.kubernetes.KubeConfig kc: ProductionPlanner.getKubeConfigs()) {
				l.add(new PlannerProcessingFacility(kc.getId(),
						kc.getDescription(),
						kc.getUrl()));
			}
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_SUCCESS, "");
			return new ResponseEntity<>(l, responseHeaders, HttpStatus.FOUND);
		}
		String message = String.format(MSG_PREFIX + "Processing facility is not connected)", 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

    /**
     * Get production planner processingfacilitiy by name
     * 
     */
	@Override
	public ResponseEntity<PlannerProcessingFacility> getPlannerProcessingFacilityByName(String name) {
		// todo handle name
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = ProductionPlanner.getKubeConfig(name);
		if (aKubeConfig != null) {
			PlannerProcessingFacility pf = new PlannerProcessingFacility(aKubeConfig.getId(),
					aKubeConfig.getDescription(),
					aKubeConfig.getUrl());
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_SUCCESS, "");
			return new ResponseEntity<>(pf, responseHeaders, HttpStatus.FOUND);
		} else {
			String message = String.format(MSG_PREFIX + "Processing Facility %s not found (%d)", name, 2000);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
	}

    /**
     * Get pods on processing facility
     * 
     */
	@Override
    public  ResponseEntity<List<PlannerPod>> getPlannerPodsByName(String name) {
		return getPlannerPods(null, name);
	}
	/**
	 * Get pods on processing facility
	 * 
	 */
	@Override
	public  ResponseEntity<List<PlannerPod>> getPlannerPods(String status, String name) {
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = ProductionPlanner.getKubeConfig(name);
		if (aKubeConfig != null) {
			if (aKubeConfig.getId().equalsIgnoreCase(name)) {
				// todo handle name
				if (aKubeConfig.isConnected()) {			 
					V1JobList list = aKubeConfig.getJobList();
					List<PlannerPod> jobList = new ArrayList<PlannerPod>();
					if (list != null) {
						for (V1Job item : list.getItems()) {
							PodKube pk = new PodKube(item);
							if (pk != null) {
								if (status == null || pk.hasStatus(status)) {
									jobList.add(pk);
								}
							}
						}
						HttpHeaders responseHeaders = new HttpHeaders();
						responseHeaders.set(HTTP_HEADER_SUCCESS, "");
						return new ResponseEntity<>(jobList, responseHeaders, HttpStatus.FOUND);
					}
				}
			} else {
		    	String message = String.format(MSG_PREFIX + "Processing Facility %s not found (%d)", name, 2000);
		    	logger.error(message);
		    	HttpHeaders responseHeaders = new HttpHeaders();
		    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}			
		} 
		String message = String.format(MSG_PREFIX + "Processing facility is not connected)", 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

    /**
     * Delete completed pods on processing facility
     * 
     */
	@Override
    public ResponseEntity<?> deletePod(String status, String name) {
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = ProductionPlanner.getKubeConfig(name);
		if (aKubeConfig != null) {
			if (aKubeConfig.isConnected()) {	
				aKubeConfig.deletePodsStatus(status);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "");
				return new ResponseEntity<>(responseHeaders, HttpStatus.FOUND);
			} else {
				String message = String.format(MSG_PREFIX + "Processing facility is not connected)", 2000);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
		}
    	String message = String.format(MSG_PREFIX + "Processing Facility %s not found (%d)", name, 2000);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

    /**
     * Pod of name has finished with state
     * 
     */
	@Override
    public ResponseEntity<PlannerPod> modifyProcessingfacilities(String podname, String name, String status) {
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = ProductionPlanner.getKubeConfig(name);
		if (aKubeConfig != null) {
			// todo check for existing pod, jobstep, ... 
			// set jobstep and pod status,
			// finish jobstep, collect log and data, ...
			
			// delete pod
			aKubeConfig.deleteJob(podname);
			
			
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_SUCCESS, "");
			return new ResponseEntity<>(responseHeaders, HttpStatus.FOUND);
		}
		String message = String.format(MSG_PREFIX + "Processing facility is not connected)", 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

    /**
     * Get jobstep/pod for test purpose
     * 
     */
	public ResponseEntity<PlannerPod> getPlannerPod(String podname, String name) {
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = ProductionPlanner.getKubeConfig(name);
		if (aKubeConfig != null) {
			V1Job aJob = aKubeConfig.getV1Job(podname);
			if (aJob != null) {
				PodKube aPlan = new PodKube(aJob);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "");
				return new ResponseEntity<>(aPlan, responseHeaders, HttpStatus.FOUND);
			}
		}
    	String message = String.format(MSG_PREFIX + "CREATE not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
    
    /**
     * Create jobstep/pod for test purpose
     * 
     */
	@Override
    public ResponseEntity<PlannerJobstep> updateProcessingfacilities(String podname, String name) {
    	KubeJob aJob = ProductionPlanner.getKubeConfig(name).createJob(podname);
    	if (aJob != null) {
    		PlannerJobstep aPlan = new PlannerJobstep();
    		aPlan.setId(((Integer)aJob.getJobId()).toString());
    		aPlan.setName(aJob.getJobName());
    		HttpHeaders responseHeaders = new HttpHeaders();
    		responseHeaders.set(HTTP_HEADER_SUCCESS, "");
    		return new ResponseEntity<>(aPlan, responseHeaders, HttpStatus.FOUND);
    	}
    	String message = String.format(MSG_PREFIX + "CREATE not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
}
