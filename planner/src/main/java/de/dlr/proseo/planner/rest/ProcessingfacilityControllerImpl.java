/**
 * ProcessingfacilityControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.rest.ProcessingfacilityController;
import de.dlr.proseo.model.rest.model.PlannerPod;
import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.util.UtilService;
import de.dlr.proseo.planner.kubernetes.KubeJob;
/**
 * Spring MVC controller for the prosEO planner; implements the services required to handle
 * processing facilities.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class ProcessingfacilityControllerImpl implements ProcessingfacilityController{
	
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(ProcessingfacilityController.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
    /**
     * Get attached processing facilities
     * 
     */
	@Override
	@Transactional
    public ResponseEntity<List<RestProcessingFacility>> getRestProcessingFacilities() {
		productionPlanner.updateKubeConfigs();
		if (productionPlanner.getKubeConfigs() != null) {
			List<RestProcessingFacility> l = new ArrayList<RestProcessingFacility>();
			for (de.dlr.proseo.planner.kubernetes.KubeConfig kc: productionPlanner.getKubeConfigs()) {
				l.add(new RestProcessingFacility(
						kc.getLongId(),
						kc.getVersion(),
						kc.getId(),
						kc.getFacilityState().toString(),
						kc.getDescription(),
						kc.getProcessingEngineUrl(),
						kc.getProcessingEngineToken(),
						kc.getMaxJobsPerNode().longValue(),
						kc.getStorageManagerUrl(),
						kc.getLocalStorageManagerUrl(),
						kc.getStorageManagerUser(),
						kc.getStorageManagerPassword(),
						kc.getStorageType().toString()));
			}
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
			return new ResponseEntity<>(l, responseHeaders, HttpStatus.OK);
		}
		Messages.FACILITY_NOT_DEFINED.log(logger);
    	String message = Messages.FACILITY_NOT_DEFINED.formatWithPrefix();
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

    /**
     * Get production planner processing facility by name
     * 
     */
	@Override
	@Transactional
	public ResponseEntity<RestProcessingFacility> getRestProcessingFacilityByName(String name) {
		de.dlr.proseo.planner.kubernetes.KubeConfig kc = productionPlanner.getKubeConfig(name);
		if (kc != null) {
			RestProcessingFacility pf = new RestProcessingFacility(
					null,
					null,
					kc.getId(),
					kc.getFacilityState().toString(),
					kc.getDescription(),
					kc.getProcessingEngineUrl(),
					kc.getProcessingEngineToken(),
					kc.getMaxJobsPerNode().longValue(),
					kc.getStorageManagerUrl(),
					kc.getLocalStorageManagerUrl(),
					kc.getStorageManagerUser(),
					kc.getStorageManagerPassword(),
					kc.getStorageType().toString());
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
			return new ResponseEntity<>(pf, responseHeaders, HttpStatus.OK);
		} else {
			Messages.FACILITY_NOT_EXIST.log(logger, name);
	    	String message = Messages.FACILITY_NOT_EXIST.formatWithPrefix(name);
	    	HttpHeaders responseHeaders = new HttpHeaders();
	    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
	    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
	}
    /**
     * Synchronize and run job steps of processing facility identified by name
     * 
     */
	@Override
	@Transactional
	public ResponseEntity<RestProcessingFacility> synchronizeFacility(String name) {
		productionPlanner.updateKubeConfig(name);
		de.dlr.proseo.planner.kubernetes.KubeConfig kc = productionPlanner.getKubeConfig(name);
		if (kc != null) {
			kc.sync();
			UtilService.getJobStepUtil().checkForJobStepsToRun(kc, null, false);
			RestProcessingFacility pf = new RestProcessingFacility(
					kc.getLongId(),
					null,
					kc.getId(),
					kc.getFacilityState().toString(),
					kc.getDescription(),
					kc.getProcessingEngineUrl(),
					kc.getProcessingEngineToken(),
					null,
					kc.getStorageManagerUrl(),
					kc.getLocalStorageManagerUrl(),
					kc.getStorageManagerUser(),
					kc.getStorageManagerPassword(),
					kc.getStorageType().toString());
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
			return new ResponseEntity<>(pf, responseHeaders, HttpStatus.OK);
		} else {
			Messages.FACILITY_NOT_EXIST.log(logger, name);
	    	String message = Messages.FACILITY_NOT_EXIST.formatWithPrefix(name);
	    	HttpHeaders responseHeaders = new HttpHeaders();
	    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
	    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
	}
	
    /**
     * Kubernetes pod (identified by podname) and job (identified by name)  has finished with state
     * 
     */
	@Override
	@Transactional
    public ResponseEntity<PlannerPod> finishKubeJob(String podname, String name, String status) {
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = productionPlanner.getKubeConfig(name);
		if (aKubeConfig != null) {
			// todo check for existing pod, jobstep, ... 
			// set jobstep and pod status,
			// finish jobstep, collect log and data, ...
			
			// delete pod
			KubeJob kj = aKubeConfig.getKubeJob(podname);
			if (kj != null) {
				kj.finish(aKubeConfig, podname);
			}
			// todo delete finished jobs asynchron
			// aKubeConfig.deleteJob(podname);
			
			
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), "");
			return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
		}
		String message = String.format("Processing facility is not connected)", 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
}
