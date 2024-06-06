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
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.rest.ProcessingfacilityController;
import de.dlr.proseo.model.rest.model.PlannerPod;
import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.util.UtilService;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;

/**
 * Spring MVC controller for the prosEO planner; implements the services required to handle processing facilities.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class ProcessingfacilityControllerImpl implements ProcessingfacilityController {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessingfacilityController.class);

	/** Utility class for handling HTTP headers */
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
		if (logger.isTraceEnabled())
			logger.trace(">>> getRestProcessingFacilities()");

		try {
			// Update and retrieve the Kubernetes configuration
			productionPlanner.updateKubeConfigs();
			Collection<KubeConfig> kubeConfigs = productionPlanner.getKubeConfigs();

			if (null == kubeConfigs) {
				String message = logger.log(PlannerMessage.FACILITY_NOT_DEFINED);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}

			// Iterate through Kubernetes configurations to create RestProcessingFacility objects
			List<RestProcessingFacility> processingFacilities = new ArrayList<RestProcessingFacility>();
			for (de.dlr.proseo.planner.kubernetes.KubeConfig kubeConfig : kubeConfigs) {
				processingFacilities.add(new RestProcessingFacility(kubeConfig.getLongId(), kubeConfig.getVersion(), kubeConfig.getId(), kubeConfig.getFacilityState(null).toString(),
						kubeConfig.getDescription(), kubeConfig.getProcessingEngineUrl(), kubeConfig.getProcessingEngineToken(),
						kubeConfig.getMaxJobsPerNode().longValue(), kubeConfig.getStorageManagerUrl(), kubeConfig.getExternalStorageManagerUrl(),
						kubeConfig.getLocalStorageManagerUrl(), kubeConfig.getStorageManagerUser(), kubeConfig.getStorageManagerPassword(),
						kubeConfig.getStorageType().toString()));
			}

			return new ResponseEntity<>(processingFacilities, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

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
		if (logger.isTraceEnabled())
			logger.trace(">>> getRestProcessingFacilityByName({})", name);

		// Retrieve Kubernetes configuration by name
		de.dlr.proseo.planner.kubernetes.KubeConfig kubeConfig = productionPlanner.getKubeConfig(name);

		if (null == kubeConfig) {
			String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, name);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		}

		try {
			// Create a RestProcessingFacility object using Kubernetes configuration details
			RestProcessingFacility processingFacility = new RestProcessingFacility(null, null, kubeConfig.getId(), kubeConfig.getFacilityState(null).toString(),
					kubeConfig.getDescription(), kubeConfig.getProcessingEngineUrl(), kubeConfig.getProcessingEngineToken(),
					kubeConfig.getMaxJobsPerNode().longValue(), kubeConfig.getStorageManagerUrl(), kubeConfig.getExternalStorageManagerUrl(),
					kubeConfig.getLocalStorageManagerUrl(), kubeConfig.getStorageManagerUser(), kubeConfig.getStorageManagerPassword(),
					kubeConfig.getStorageType().toString());

			return new ResponseEntity<>(processingFacility, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

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
		if (logger.isTraceEnabled())
			logger.trace(">>> synchronizeFacility({})", name);

		// Update and retrieve Kubernetes configuration
		productionPlanner.updateKubeConfig(name);
		de.dlr.proseo.planner.kubernetes.KubeConfig kubeConfig = productionPlanner.getKubeConfig(name);

		if (null == kubeConfig) {
			String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, name);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		}

		// Get the processing facility from Kubernetes configuration and ensure it is not in RUNNING or STARTING state
		ProcessingFacility processingFacility = kubeConfig.getProcessingFacility();
		if (processingFacility.getFacilityState() != FacilityState.RUNNING && processingFacility.getFacilityState() != FacilityState.STARTING) {
			String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, name, processingFacility.getFacilityState().toString());

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
		} else {
			try {
				// Synchronize Kubernetes configuration
				kubeConfig.sync();
				try {
					// Check for job steps to run
					UtilService.getJobStepUtil().checkForJobStepsToRun(kubeConfig, 0, false, true);
				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				}

				// Create a RestProcessingFacility object using Kubernetes configuration details
				RestProcessingFacility restProcessingFacility = new RestProcessingFacility(kubeConfig.getLongId(), null, kubeConfig.getId(),
						kubeConfig.getFacilityState(null).toString(), kubeConfig.getDescription(), kubeConfig.getProcessingEngineUrl(),
						kubeConfig.getProcessingEngineToken(), null, kubeConfig.getStorageManagerUrl(), kubeConfig.getExternalStorageManagerUrl(),
						kubeConfig.getLocalStorageManagerUrl(), kubeConfig.getStorageManagerUser(), kubeConfig.getStorageManagerPassword(),
						kubeConfig.getStorageType().toString());

				return new ResponseEntity<>(restProcessingFacility, HttpStatus.OK);
			} catch (Exception e) {
				String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

				if (logger.isDebugEnabled())
					logger.debug("... exception stack trace: ", e);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}

	/**
	 * Kubernetes pod and job has finished with state
	 * 
	 * @param podname name of the Kubernetes pod
	 * @param name    the Kubernetes job name
	 * @param status  finish status
	 * @return a JSON object describing the finished pod (currently not implemented)
	 */
	@Override
	public ResponseEntity<PlannerPod> finishKubeJob(String podname, String name, String status, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> finishKubeJob({}, {}, {})", podname, name, status);

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
			KubeJob kubeJob = aKubeConfig.getKubeJob(podname);
			if (null == kubeJob) {
				return new ResponseEntity<>(http.errorHeaders(logger.log(PlannerMessage.KUBECONFIG_JOB_NOT_FOUND, podname)),
						HttpStatus.OK);
			}

			kubeJob.finish(aKubeConfig, podname);

			// TODO delete finished jobs asynchron
			// aKubeConfig.deleteJob(podname);

			// TODO Correctly fill return entity PlannerPod
			PlannerPod plannerPod = new PlannerPod();
			plannerPod.setId(String.valueOf(kubeJob.getJobId()));
			plannerPod.setName(podname);
			plannerPod.setStatus(status);
			plannerPod.setSucceded(Boolean.valueOf(status == "SUCCESS").toString());

			String message = logger.log(PlannerMessage.KUBEJOB_FINISH_TRIGGERED, aKubeConfig.getId(), kubeJob.getJobName());

			return new ResponseEntity<>(plannerPod, http.errorHeaders(message), HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}
}