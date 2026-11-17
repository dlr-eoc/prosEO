/**
 * SlurmConfig.java
 *
 * © 2026 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.engine.slurm;

import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.engine.EngineConfig;
import de.dlr.proseo.planner.engine.EngineJob;
import de.dlr.proseo.planner.service.ServiceConnection.SlurmException;
import de.dlr.proseo.planner.util.UtilService;
import tools.jackson.databind.ObjectMapper;

/**
 * Represents the connection to the Slurm Rest API
 *
 * @author Ernst Melchinger
 */
public class SlurmConfig  extends EngineConfig{
	
	/** Dummy Job Order file name (only used in development environment) **/
	private static final String DUMMY_JOF_FILENAME = "/testdata/test1.pl";

	public static final String CALL_SLURM_0045 = "/slurm/v0.0.45";
	public static final String CALL_SLURMDB_0045 = "/slurmdb/v0.0.45";

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(SlurmConfig.class);

	public SlurmConfig(ProcessingFacility processingFacility, ProductionPlanner planner) {
		super(processingFacility, planner);
	}

	@Override
	public boolean connect() {
		if (logger.isTraceEnabled())
			logger.trace(">>> connect()");
		engineJobList = new HashMap<>();
		try {
			sync();
		} catch (Exception e) {
			// Exception already reported
			return false;
		}
		return isConnected();
	}

	@Override
	public boolean isConnected() {
		if (logger.isTraceEnabled())
			logger.trace(">>> isConnected()");
		ObjectMapper mapper = new ObjectMapper();
		String result = "";
		Boolean isConnected = false;
		for (int ii = 0; ii < ProseoUtil.DB_MAX_RETRY; ii++) {
			try {
				result = getProductionPlanner().getServiceConnection().getFromSlurm(getProcessingFacility().getProcessingEngineUrl(), 
						CALL_SLURM_0045 + "/ping", String.class, getProcessingFacility().getProcessingEngineToken());
				break;
			} catch (SlurmException e) {
				// Get current token and retry
				if ((ii + 1) < ProseoUtil.DB_MAX_RETRY) {
					// get new token
					try {
						refreshToken();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		Map<?,?> m = mapper.readValue(result, Map.class);
		if (m.get("pings") != null && m.get("pings") instanceof List) {
			for (Object lo : (List<?>)(m.get("pings"))) {
				if (lo instanceof Map) {		
					Map<?,?> jo = (Map<?,?>)lo;
					if (jo.get("responding") != null && jo.get("responding") instanceof Boolean && (Boolean)(jo.get("responding"))) {
						isConnected = true;
						break;
					}
				}
			}
		}
		if (logger.isTraceEnabled())
			logger.trace("<<< isConnected() {}", isConnected);
		return isConnected;
	}
	
	// Retrieve the number of nodes being able to run.
	public Boolean getNodeInfo() {
		if (logger.isTraceEnabled())
			logger.trace(">>> getNodeInfo()");
		ObjectMapper mapper = new ObjectMapper();
		String result = "";
		for (int ii = 0; ii < ProseoUtil.DB_MAX_RETRY; ii++) {
			try {
				result = getProductionPlanner().getServiceConnection().getFromSlurm(getProcessingFacility().getProcessingEngineUrl(), 
						CALL_SLURM_0045 + "/nodes", String.class, getProcessingFacility().getProcessingEngineToken());
				break;
			} catch (SlurmException e) {
				// Get current token and retry
				if ((ii + 1) < ProseoUtil.DB_MAX_RETRY) {
					// get new token
					try {
						refreshToken();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		
		// Known node states are:
		//
		//ALLOCATED
		//    The node has been allocated to one or more jobs. 
		//
		//ALLOCATED+
		//    The node is allocated to one or more active jobs plus one or more jobs are in the process of COMPLETING. 
		//
		//BLOCKED
		//    The node has been blocked by exclusive topo job. 
		//
		//COMPLETING
		//    All jobs associated with this node are in the process of COMPLETING. This node state will be removed when all of the job's processes have terminated and the Slurm epilog program (if any) has terminated. While a node is in the COMPLETING state, it is not eligible for new job placement and no jobs will be scheduled on it. See the Epilog parameter description in the slurm.conf(5) man page for more information. 
		//
		//DOWN
		//    The node is unavailable for use. Slurm can automatically place nodes in this state if some failure occurs. System administrators may also explicitly place nodes in this state. If a node resumes normal operation, Slurm can automatically return it to service. See the ReturnToService and SlurmdTimeout parameter descriptions in the slurm.conf(5) man page for more information. 
		//
		//DRAINED
		//    The node is unavailable for use per system administrator request. See the update node command in the scontrol(1) man page or the slurm.conf(5) man page for more information. 
		//
		//DRAINING
		//    The node is currently allocated a job, but will not be allocated additional jobs. The node state will be changed to state DRAINED when the last job on it completes. Nodes enter this state per system administrator request. See the update node command in the scontrol(1) man page or the slurm.conf(5) man page for more information. 
		//
		//FAIL
		//    The node is expected to fail soon and is unavailable for use per system administrator request. See the update node command in the scontrol(1) man page or the slurm.conf(5) man page for more information. 
		//
		//FAILING
		//    The node is currently executing a job, but is expected to fail soon and is unavailable for use per system administrator request. See the update node command in the scontrol(1) man page or the slurm.conf(5) man page for more information. 
		//
		//FUTURE
		//    The node is currently not fully configured, but expected to be available at some point in the indefinite future for use. 
		//
		//IDLE
		//    The node is not allocated to any jobs and is available for use. 
		//
		//INVAL
		//    The node did not register correctly with the controller. This happens when a node registers with less resources than configured in the slurm.conf file. The node will clear from this state with a valid registration (i.e. a slurmd restart is required). 
		//
		//MAINT
		//    The node is currently in a reservation with a flag value of "maintenance". 
		//
		//REBOOT_ISSUED
		//    A reboot request has been sent to the agent configured to handle this request. 
		//
		//REBOOT_REQUESTED
		//    A request to reboot this node has been made, but hasn't been handled yet. 
		//
		//MIXED
		//    The node has some of its CPUs ALLOCATED while others are IDLE. Or the node has a suspended job allocated to some of its TRES (e.g. memory). 
		//
		//PLANNED
		//    The node is planned by the backfill scheduler for a higher priority job. 
		//
		//POWER_DOWN
		//    The node is pending power down. 
		//
		//POWERED_DOWN
		//    The node is currently powered down and not capable of running any jobs. 
		//
		//POWERING_DOWN
		//    The node is in the process of powering down and not capable of running any jobs. 
		//
		//POWERING_UP
		//    The node is in the process of being powered up. 
		//
		//RESERVED
		//    The node is in an advanced reservation and not generally available. 
		//
		//UNKNOWN
		//    The Slurm controller has just started and the node's state has not yet been determined. 
		
		Map<?,?> m = mapper.readValue(result, Map.class);
		workerCnt = 0;
		if (m.get("nodes") != null && m.get("nodes") instanceof List) {
			for (Object lo : (List<?>)(m.get("nodes"))) {
				if (lo instanceof Map) {		
					Map<?,?> jo = (Map<?,?>)lo;
					if (jo.get("state") != null && jo.get("state") instanceof List) {
						Long w = 0L;
						for (Object state : (List<?>)(jo.get("state"))) {
							if (state instanceof String && "IDLE".equalsIgnoreCase((String)state)) {
								w = 1L;
								continue;
							}
							if (state instanceof String && "ALLOCATED".equalsIgnoreCase((String)state)) {
								w = 1L;
								continue;
							}
							if (state instanceof String && "ALLOCATED+".equalsIgnoreCase((String)state)) {
								w = 1L;
								continue;
							}
							if (state instanceof String && "COMPLETING".equalsIgnoreCase((String)state)) {
								w = 1L;
								continue;
							}
							if (state instanceof String && "PLANNED".equalsIgnoreCase((String)state)) {
								w = 1L;
								continue;
							}
							if (state instanceof String && "MIXED".equalsIgnoreCase((String)state)) {
								w = 1L;
								continue;
							}
							workerCnt += w;
						}
					}
				}
			}
		}
		return true;
	}

	/** Synchronizes the Slurm jobs and the internal SlurmJob list with the Planner. */
	@Override
	public void sync() {
		if (logger.isTraceEnabled())
			logger.trace(">>> sync()");

		// Step 1: Check facility state
		if (getFacilityState(null) == FacilityState.DISABLED || getFacilityState(null) == FacilityState.STOPPED
				|| getFacilityState(null) == FacilityState.STARTING) {
			// Nothing to do if facility state is disabled, stopped, or starting
			return;
		}
		try {
			getNodeInfo(); // TODO Should this info be processed?
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			return;
		}
		
		// Step 2: Rebuild runtime data by retrieving all jobs 
		ObjectMapper mapper = new ObjectMapper();
		String result = "";
		for (int ii = 0; ii < ProseoUtil.DB_MAX_RETRY; ii++) {
			try {
				result = getProductionPlanner().getServiceConnection().getFromSlurm(getProcessingFacility().getProcessingEngineUrl(), 
						CALL_SLURM_0045 + "/jobs", String.class, getProcessingFacility().getProcessingEngineToken());
				break;
			} catch (SlurmException e) {
				// Get current token and retry
				if ((ii + 1) < ProseoUtil.DB_MAX_RETRY) {
					// get new token
					try {
						refreshToken();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}		
		// get database jobs
		// TODO? currently not used
		//String dbResult = "";
		//for (int ii = 0; ii < ProseoUtil.DB_MAX_RETRY; ii++) {
		//	try {
		//		dbResult = getProductionPlanner().getServiceConnection().getFromSlurm(getProcessingFacility().getProcessingEngineUrl(), 
		//				CALL_SLURMDB_0045 + "/jobs", String.class, getProcessingFacility().getProcessingEngineToken());
		//		break;
		//	} catch (SlurmException e) {
		//		// Get current token and retry
		//		if ((ii + 1) < ProseoUtil.DB_MAX_RETRY) {
		//			// get new token
		//			try {
		//				refreshToken();
		//			} catch (Exception e1) {
		//				// TODO Auto-generated catch block
		//				e1.printStackTrace();
		//			}
		//		} else {
		//			if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
		//			throw e;
		//		}
		//	}
		//}
		
		// Look for the id, name and job states:
		//  
		//	BOOT_FAIL	terminated due to node boot failure
		//	CANCELLED	cancelled by user or administrator
		//	COMPLETED	completed execution successfully; finished with an exit code of zero on all nodes
		//	DEADLINE	terminated due to reaching the latest start time that allows the job to reach its deadline given its TimeLimit
		//	FAILED	completed execution unsuccessfully; non-zero exit code or other failure condition
		//	NODE_FAIL	terminated due to node failure
		//	OUT_OF_MEMORY	experienced out of memory error
		//	PENDING	queued and waiting for initiation; will typically have a reason code specifying why it has not yet started
		//	PREEMPTED	terminated due to preemption; may transition to another state based on the configured PreemptMode and job characteristics
		//	RUNNING	allocated resources and executing
		//	SUSPENDED	allocated resources but execution suspended, such as from preemption or a direct request from an authorized user
		//	TIMEOUT	terminated due to reaching the time limit, such as those configured in slurm.conf or specified for the individual job
		// 

		// Step 3: Update engineJob list
		Map<String, JobStepState> slurmJobs = new HashMap<>();
		JobStepState jobState;
		String jobName;
		Integer slurmId;
		Map<?,?> m = mapper.readValue(result, Map.class);
		if (m.get("jobs") != null && m.get("jobs") instanceof List) {
			for (Object lo : (List<?>)(m.get("jobs"))) {
				if (lo instanceof Map) {	
					jobState = null;
					jobName = null;
					slurmId = null;							
					Map<?,?> jo = (Map<?,?>)lo;
					if (jo.get("job_state") != null && jo.get("job_state") instanceof List) {
						for (Object state : (List<?>)(jo.get("job_state"))) {
							if (state instanceof String && "COMPLETED".equalsIgnoreCase((String)state)) {
								jobState = JobStepState.COMPLETED;
							} else if (state instanceof String && "RUNNING".equalsIgnoreCase((String)state)) {
								jobState = JobStepState.RUNNING;
							} else if (state instanceof String && "PENDING".equalsIgnoreCase((String)state)) {
								jobState = JobStepState.RUNNING;
							} else if (state instanceof String && "FAILED".equalsIgnoreCase((String)state)) {
								jobState = JobStepState.FAILED;
							} else {
								jobState = JobStepState.FAILED;
							}
						}
					}
					if (jo.get("name") != null && jo.get("name") instanceof String) {
						jobName = (String)(jo.get("name"));
					}
					if (jo.get("job_id") != null && jo.get("job_id") instanceof Integer) {
						slurmId = (Integer)(jo.get("job_id"));
					}
					slurmJobs.put(jobName, jobState);
					if (jobState != null && jobName != null && slurmId != null) {
						// Build a slurm job for "active" jobs
						// Ensure that the slurm job is contained in the configuration's map of slurm jobs
						if (getEngineJob(jobName) == null) {
							if (!getEngineJobList().containsKey(jobName) && jobState.equals(JobStepState.RUNNING)) {
								Long jobId = Long.valueOf(jobName.substring(ProductionPlanner.jobNamePrefix.length()));
								SlurmJob newJob = new SlurmJob(jobId, DUMMY_JOF_FILENAME);
								newJob.setSlurmJobId(slurmId);
								if (newJob != null) {
									getEngineJobList().put(newJob.getJobName(), newJob);
								}
							}
						}
					}
				}
			}
		}
		// Step 4: Remove elements from engineJob list without existing Slurm jobs
		List<EngineJob> sjList = new ArrayList<>();
		sjList.addAll(engineJobList.values());
		for (EngineJob sj : sjList) {
			if (!slurmJobs.containsKey(sj.getJobName())) {
				// TODO Did we not just add missing jobs to engineJobList in step 3? If this is about non-prosEO jobs, we could have
				// removed them before.
				engineJobList.remove(sj.getJobName());
			}
		}
		// Step 5: Check if any slurm job has finished without triggering a message event to the Planner
		for (String aJobName : slurmJobs.keySet()) {
			EngineJob slurmJob = engineJobList.get(aJobName);
			if (slurmJob != null && ! slurmJobs.get(aJobName).equals(JobStepState.RUNNING) 
					&& !getProductionPlanner().getFinishThreads().containsKey(aJobName)) {
				slurmJob.updateFinishInfoAndDelete(aJobName);
			}
		}
		// Step 6: Update the state of job steps in the database
		List<Long> jobStepIds = new ArrayList<Long>();
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.execute((status) -> {

			List<de.dlr.proseo.model.JobStep.JobStepState> jobStepStates = new ArrayList<>();
			jobStepStates.add(de.dlr.proseo.model.JobStep.JobStepState.RUNNING);
			List<JobStep> runningJobSteps = RepositoryService.getJobStepRepository()
				.findAllByProcessingFacilityAndJobStepStateIn(processingFacility.getId(), jobStepStates);
			for (JobStep jobStep : runningJobSteps) {
				jobStepIds.add(jobStep.getId());
			}
			return null;
		});

		// These job steps have to be in the slurm job list. If not, there was a problem. Set it to failed.
		for (Long jobStepId : jobStepIds) {
			for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
				try {

					transactionTemplate.execute((status) -> {
						String locJobName = ProductionPlanner.jobNamePrefix + jobStepId;
						Optional<JobStep> jobStepOpt = RepositoryService.getJobStepRepository().findById(jobStepId);
						JobStep jobStep = null;
						if (jobStepOpt.isPresent()) {
							jobStep = jobStepOpt.get();
						} else {
							return null;
						}
						// If no job with this name is running, change state to FAILED and set info in log
						if (!slurmJobs.containsKey(locJobName)) {
							Product outputProduct = jobStep.getOutputProduct();
							boolean wasFailed = true;

							if (outputProduct != null) {
								// Check whether the expected products were already generated
								List<Product> collectedProducts = new ArrayList<>();
								UtilService.getJobStepUtil().collectProducts(outputProduct, collectedProducts);
								if (UtilService.getJobStepUtil()
										.checkProducts(collectedProducts, jobStep.getJob().getProcessingFacility())) {
									jobStep.setJobStepState(JobStepState.COMPLETED);
									wasFailed = false;
								} else {
									jobStep.setJobStepState(JobStepState.FAILED);
								}
							} else {
								jobStep.setJobStepState(JobStepState.FAILED);
							}

							jobStep.incrementVersion();

							// Save the information on what happened
							String stdout = jobStep.getProcessingStdOut();
							if (stdout == null) {
								stdout = "";
							}
							if (wasFailed) {
								jobStep.setProcessingStdOut(
										"Job on Processing Facility was deleted/canceled by others (e.g. operator) or crashed\n\n"
												+ stdout);
							}
							jobStep = RepositoryService.getJobStepRepository().save(jobStep);

							// Check whether the job step is now finished, potentially do something accordingly
							UtilService.getJobStepUtil().checkFinish(jobStep);
						} else if (slurmJobs.get(locJobName).equals(JobStepState.COMPLETED)) {
							jobStep.setJobStepState(JobStepState.COMPLETED);
							jobStep.incrementVersion();
							jobStep = RepositoryService.getJobStepRepository().save(jobStep);
							// Check whether the job step is now finished, potentially do something accordingly
							UtilService.getJobStepUtil().checkFinish(jobStep);
						} else if (slurmJobs.get(locJobName).equals(JobStepState.FAILED)) {
							jobStep.setJobStepState(JobStepState.FAILED);
							jobStep.incrementVersion();
							jobStep = RepositoryService.getJobStepRepository().save(jobStep);
							// Check whether the job step is now finished, potentially do something accordingly
							UtilService.getJobStepUtil().checkFinish(jobStep);
						}

						return null;
					});
					break;
				} catch (CannotAcquireLockException e) {
					if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

					if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
						ProseoUtil.dbWait();
					} else {
						if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
						throw e;
					}
				}
			}

		}
	}

	/**
	 * Creates a new job on the cluster with the specified name and log levels.
	 *
	 * @param name           the name of the new job
	 * @param stdoutLogLevel the log level for stdout
	 * @param stderrLogLevel the log level for stderr
	 * @return the created job or null
	 */
	@Override
	public SlurmJob createJob(String name, String stdoutLogLevel, String stderrLogLevel) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJob({}, {}, {})", name, stdoutLogLevel, stderrLogLevel);

		SlurmJob newJob = new SlurmJob(Long.parseLong(name), DUMMY_JOF_FILENAME);

		try {
			newJob = newJob.createJob(this, stdoutLogLevel, stderrLogLevel);
		} catch (RuntimeException e) {
			logger.log(PlannerMessage.JOB_CREATION_FAILED, e.getClass() + " - " + e.getMessage());

			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			newJob = null;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			newJob = null;
		}

		if (newJob != null) {
			engineJobList.put(newJob.getJobName(), newJob);
		}

		return newJob;
	}

	/**
	 * Creates a new job on the cluster with the specified ID and log levels.
	 *
	 * @param id             the ID of the job
	 * @param stdoutLogLevel the log level for stdout
	 * @param stderrLogLevel the log level for stderr
	 * @return the created job or null
	 */
	@Override
	public SlurmJob createJob(long id, String stdoutLogLevel, String stderrLogLevel) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJob({}, {}, {})", id, stdoutLogLevel, stderrLogLevel);

		SlurmJob newJob = new SlurmJob(id, DUMMY_JOF_FILENAME);

		try {
			newJob = newJob.createJob(this, stdoutLogLevel, stderrLogLevel);
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			newJob = null;
		}

		if (newJob != null) {
			engineJobList.put(newJob.getJobName(), newJob);
		}

		return newJob;
	
	}

	/**
	 * Deletes a named job from the cluster.
	 *
	 * @param name the name of the job to delete
	 * @return true if the job was successfully deleted, otherwise false
	 */
	@Override
	public boolean deleteJob(String name) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteJob({})", name);
		
		EngineJob ej = getEngineJob(name);
		SlurmJob slurmJob = null;
		if (ej instanceof SlurmJob) {
			slurmJob = (SlurmJob)ej;
		} else {
			return false;
		}
		String result = "";
		for (int ii = 0; ii < ProseoUtil.DB_MAX_RETRY; ii++) {
			try {
				result = getProductionPlanner().getServiceConnection().deleteFromSlurm(getProcessingFacility().getProcessingEngineUrl(), 
						SlurmConfig.CALL_SLURM_0045 + "/job/" + slurmJob.getSlurmJobId() + "?signal=SIGKILL", String.class, getProcessingFacility().getProcessingEngineToken());
				break;
			} catch (SlurmException e) {
				// Get current token and retry
				if ((ii + 1) < ProseoUtil.DB_MAX_RETRY) {
					// get new token
					try {
						refreshToken();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		ObjectMapper mapper = new ObjectMapper();
		Map<?,?> m = mapper.readValue(result, Map.class);
		if (m.get("job_id") != null && m.get("job_id") instanceof Integer) {
//			this.slurmJobId = (Integer)(m.get("job_id"));
			List<?> a = (List<?>) m.get("errors");
			for (Object o : a) {
				if (o instanceof Map) {
					if (((Map<?,?>)o).get("error_number").equals(1007)) {
						// Protocol authentication error
						// get new token
					}
				}
			}
		}

		TransactionTemplate transactionTemplate = new TransactionTemplate(this.getProductionPlanner().getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		final SlurmJob slurmJobLoc = slurmJob;
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {
				transactionTemplate.execute((status) -> {
					Long jobStepId = slurmJobLoc.getJobId();
					Optional<JobStep> jobStep = RepositoryService.getJobStepRepository().findById(jobStepId);
					if (jobStep.isPresent()) {

						slurmJobLoc.updateJobLog(jobStep.get());
					}
					return null;
				});
			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait();
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		engineJobList.remove(name);
		return true;
	}

	// Get new token for authentication
	public void refreshToken() throws Exception {

		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.redirectErrorStream(true);

		processBuilder.command(ProductionPlanner.config.getTokenCommand().split(" "));
		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(Redirect.PIPE);
		int exitCode = 0; // Failure
		try {
			Process process;
			exitCode = (process = processBuilder.start()).waitFor();
			String output = process.getOutputStream().toString();
			output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			if (exitCode == 0 && output != null && output.startsWith("SLURM_JWT=")) {
				processingFacility.setProcessingEngineToken(output.substring("SLURM_JWT=".length()));
			}
		} catch (Exception e) {
			throw e;
		}
	}
}
