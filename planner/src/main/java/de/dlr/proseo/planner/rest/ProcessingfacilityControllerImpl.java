/**
 * ProcessingfacilityControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	private static Logger logger = LoggerFactory.getLogger(ProcessingfacilityController.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
    /**
     * Get attached processing facilities
     * 
     * @return a list of JSON objects describing the processing facilities
     */
	@Override
	@Transactional
    public ResponseEntity<List<RestProcessingFacility>> getRestProcessingFacilities() {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProcessingFacilities()");
		
		try {
			productionPlanner.updateKubeConfigs();
			Collection<KubeConfig> kubeConfigs = productionPlanner.getKubeConfigs();
			
			if (null == kubeConfigs) {
				String message = Messages.FACILITY_NOT_DEFINED.log(logger);

		    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
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
						kc.getLocalStorageManagerUrl(),
						kc.getStorageManagerUser(),
						kc.getStorageManagerPassword(),
						kc.getStorageType().toString()));
			}

			return new ResponseEntity<>(l, HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Get production planner processing facility by name
     * 
     * @param name the processing facility name
     * @return a JSON object describing the processing facility
     */
	@Override
	@Transactional
	public ResponseEntity<RestProcessingFacility> getRestProcessingFacilityByName(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProcessingFacilityByName({})", name);
		
		de.dlr.proseo.planner.kubernetes.KubeConfig kc = productionPlanner.getKubeConfig(name);
		
		if (null == kc) {
			String message = Messages.FACILITY_NOT_EXIST.log(logger, name);

	    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
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
					kc.getLocalStorageManagerUrl(),
					kc.getStorageManagerUser(),
					kc.getStorageManagerPassword(),
					kc.getStorageType().toString());

			return new ResponseEntity<>(pf, HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    /**
     * Synchronize and run job steps of processing facility identified by name
     * 
     * @param name the processing facility name
     * @return a JSON object describing the processing facility that was synchronized
     */
	@Override
	@Transactional
	public ResponseEntity<RestProcessingFacility> synchronizeFacility(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> synchronizeFacility({})", name);
		
		productionPlanner.updateKubeConfig(name);
		de.dlr.proseo.planner.kubernetes.KubeConfig kc = productionPlanner.getKubeConfig(name);
		
		if (null == kc) {
			String message = Messages.FACILITY_NOT_EXIST.log(logger, name);

	    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		}
		
		try {
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

			return new ResponseEntity<>(pf, HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
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
	@Transactional
    public ResponseEntity<PlannerPod> finishKubeJob(String podname, String name, String status) {
		if (logger.isTraceEnabled()) logger.trace(">>> finishKubeJob({}, {}, {})", podname, name, status);
		
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = productionPlanner.getKubeConfig(name);
		
		if (null == aKubeConfig) {
			String message = Messages.FACILITY_NOT_AVAILABLE.log(logger, "for job " + name, "Not connected");
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
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
			
			String message = Messages.KUBEJOB_FINISH_TRIGGERED.log(logger, aKubeConfig.getId(), kj.getJobName());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
}
