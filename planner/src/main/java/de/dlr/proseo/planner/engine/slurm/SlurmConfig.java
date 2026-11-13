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
	public static final String CALL_SLURMDB_0045 = "/slurm/v0.0.45";

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(SlurmConfig.class);

	public SlurmConfig(ProcessingFacility processingFacility, ProductionPlanner planner) {
		super(processingFacility, planner);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean connect() {
		// TODO Auto-generated method stub

		engineJobList = new HashMap<>();
		try {
			sync();
		} catch (Exception e) {
			// Exception already reported
			return false;
		}
		return true;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return true;
	}

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
		Map<?,?> m = mapper.readValue(result, Map.class);
		if (m.get("nodes") != null && m.get("nodes") instanceof List) {
			workerCnt = ((List<?>)(m.get("nodes"))).size();
		}
		// get current jobs
		result = "";
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
		String dbResult = "";
		for (int ii = 0; ii < ProseoUtil.DB_MAX_RETRY; ii++) {
			try {
				dbResult = getProductionPlanner().getServiceConnection().getFromSlurm(getProcessingFacility().getProcessingEngineUrl(), 
						CALL_SLURMDB_0045 + "/jobs", String.class, getProcessingFacility().getProcessingEngineToken());
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
		

		Map<String, JobStepState> slurmJobs = new HashMap<>();
		JobStepState jobState;
		String jobName;
		Integer slurmId;
		m = mapper.readValue(result, Map.class);
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
						// Ensure that the kube job is contained in the configuration's map of kube jobs
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
		// Step 4: Remove elements from KubeJob list without existing Kubernetes jobs
		List<EngineJob> sjList = new ArrayList<>();
		sjList.addAll(engineJobList.values());
		for (EngineJob sj : sjList) {
			if (!slurmJobs.containsKey(sj.getJobName())) {
				// TODO Did we not just add missing jobs to engineJobList in step 3? If this is about non-prosEO jobs, we could have
				// removed them before.
				engineJobList.remove(sj.getJobName());
			}
		}
		// Step 5: Check if any Kubernetes job has finished without triggering a message event to the Planner
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

		// These job steps have to be in the Kubernetes job list. If not, there was a problem. Set it to failed.
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

					Boolean success = Boolean.FALSE;
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
