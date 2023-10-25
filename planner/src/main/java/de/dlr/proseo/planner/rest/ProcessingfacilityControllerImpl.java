/**
 * ProcessingfacilityControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.rest.ProcessingfacilityController;
import de.dlr.proseo.model.rest.model.PlannerPod;
import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.util.UtilService;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
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
	private static ProseoLogger logger = new ProseoLogger(ProcessingfacilityController.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PLANNER);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
    /**
     * Get attached processing facilities
     * 
     * @return a list of JSON objects describing the processing facilities
     */
	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public ResponseEntity<List<RestProcessingFacility>> getRestProcessingFacilities(HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProcessingFacilities()");
		
		try {
			productionPlanner.updateKubeConfigs();
			Collection<KubeConfig> kubeConfigs = productionPlanner.getKubeConfigs();
			
			if (null == kubeConfigs) {
				String message = logger.log(PlannerMessage.FACILITY_NOT_DEFINED);

		    	return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}

			List<RestProcessingFacility> l = new ArrayList<RestProcessingFacility>();
			
			for (de.dlr.proseo.planner.kubernetes.KubeConfig kc: kubeConfigs) {
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
						kc.getExternalStorageManagerUrl(),
						kc.getLocalStorageManagerUrl(),
						kc.getStorageManagerUser(),
						kc.getStorageManagerPassword(),
						kc.getStorageType().toString()));
			}

			return new ResponseEntity<>(l, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Get production planner processing facility by name
     * 
     * @param name the processing facility name
     * @return a JSON object describing the processing facility
     */
	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public ResponseEntity<RestProcessingFacility> getRestProcessingFacilityByName(String name, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProcessingFacilityByName({})", name);
		
		de.dlr.proseo.planner.kubernetes.KubeConfig kc = productionPlanner.getKubeConfig(name);
		
		if (null == kc) {
			String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, name);

	    	return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		}
		
		try {
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
					kc.getExternalStorageManagerUrl(),
					kc.getLocalStorageManagerUrl(),
					kc.getStorageManagerUser(),
					kc.getStorageManagerPassword(),
					kc.getStorageType().toString());

			return new ResponseEntity<>(pf, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    /**
     * Synchronize and run job steps of processing facility identified by name
     * 
     * @param name the processing facility name
     * @return a JSON object describing the processing facility that was synchronized
     */
	@Override
	public ResponseEntity<RestProcessingFacility> synchronizeFacility(String name, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> synchronizeFacility({})", name);
		
		productionPlanner.updateKubeConfig(name);
		de.dlr.proseo.planner.kubernetes.KubeConfig kc = productionPlanner.getKubeConfig(name);
		
		if (null == kc) {
			String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, name);

	    	return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		}
		
		try {
			kc.sync();
			try {
				productionPlanner.acquireThreadSemaphore("synchronizeFacility");
				UtilService.getJobStepUtil().checkForJobStepsToRun(kc, 0, false, true);
				productionPlanner.releaseThreadSemaphore("run");		
			} catch (Exception e) {
				productionPlanner.releaseThreadSemaphore("synchronizeFacility");		
				logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			}
			
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
					kc.getExternalStorageManagerUrl(),
					kc.getLocalStorageManagerUrl(),
					kc.getStorageManagerUser(),
					kc.getStorageManagerPassword(),
					kc.getStorageType().toString());

			return new ResponseEntity<>(pf, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
    /**
     * Kubernetes pod and job has finished with state
     * 
     * @param podname name of the Kubernetes pod
     * @param name the Kubernetes job name
     * @param status finish status
     * @return a JSON object describing the finished pod (currently not implemented)
     */
	@Override
    public ResponseEntity<PlannerPod> finishKubeJob(String podname, String name, String status, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> finishKubeJob({}, {}, {})", podname, name, status);
		
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = productionPlanner.getKubeConfig(name);
		
		if (null == aKubeConfig) {
			String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, "for job " + name, "Not connected");
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		}
		
		try {
			// TODO check for existing pod, jobstep, ... 
			// set jobstep and pod status,
			// finish jobstep, collect log and data, ...
			
			// delete pod
			KubeJob kj = aKubeConfig.getKubeJob(podname);
			if (kj != null) {
				kj.finish(aKubeConfig, podname);
			}
			// TODO delete finished jobs asynchron
			// aKubeConfig.deleteJob(podname);
			
			// TODO Correctly fill return entity PlannerPod
			
			String message = logger.log(PlannerMessage.KUBEJOB_FINISH_TRIGGERED, aKubeConfig.getId(), kj.getJobName());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
}
