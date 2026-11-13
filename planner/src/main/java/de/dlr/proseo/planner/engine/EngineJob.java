/**
 * EngineJob.java
 *
 * © 2026 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.enums.JobOrderVersion;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.util.UtilService;
import io.kubernetes.client.openapi.models.V1Job;

/**
 * An EngineJob describes the complete information to run a job.
 *
 * @author Ernst Melchinger
 *
 */
public abstract class EngineJob {


	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(EngineJob.class);

	/** The job's database id */
	protected long jobId;

	/** The generated job name (job prefix + job ID) */
	protected String jobName;

	/** The generated container name (container prefix + job ID) */
	protected String containerName;

	/** The processor image name */
	protected String imageName;

	/** The job order file (JOF) */
	protected String jobOrderFileName;

	/** Additional commands for the kube job (command arguments) */
	protected ArrayList<String> args = new ArrayList<>();

	/** The processing order to which the job step is associated */
	protected JobOrder jobOrder;

	/** The processing facility running the job step */
	protected EngineConfig engineConfig;

	/**
	 * Internal class to store data for Job Order creation
	 */
	private static class JobOrderData {
		public JobOrder jobOrder;
		public JobOrderVersion jobOrderVersion;

		public JobOrderData(JobOrder jobOrder, JobOrderVersion jobOrderVersion) {
			this.jobOrder = jobOrder;
			this.jobOrderVersion = jobOrderVersion;
		}
	}

	/**
	 * Returns the job ID.
	 *
	 * @return the jobId
	 */
	public long getJobId() {
		return jobId;
	}

	/**
	 * Returns the job name.
	 *
	 * @return the jobName
	 */
	public String getJobName() {
		return jobName;
	}

	/**
	 * Returns the name of the container associated with the job.
	 *
	 * @return the containerName
	 */
	public String getContainerName() {
		return containerName;
	}

	/**
	 * Returns the name of the job order file (JOF).
	 *
	 * @return the jobOrderFileName
	 */
	public String getJobOrderFileName() {
		return jobOrderFileName;
	}

	/**
	 * Sets the name of the job order file (JOF).
	 *
	 * @param jobOrderFileName the jobOrderFileName to set
	 */
	public void setJobOrderFileName(String jobOrderFileName) {
		this.jobOrderFileName = jobOrderFileName;
	}

	/**
	 * Returns the processing order to which the job is associated.
	 *
	 * @return the jobOrder
	 */
	public JobOrder getJobOrder() {
		return jobOrder;
	}

	/**
	 * Sets the job order for the job.
	 *
	 * @param jobOrder the jobOrder to set
	 */
	public void setJobOrder(JobOrder jobOrder) {
		this.jobOrder = jobOrder;
	}

	/**
	 * Add an argument to the list of command arguments.
	 *
	 * @param arg The command argument to add
	 */
	public void addArg(String arg) {
		if (arg != null) {
			args.add(arg);
		}
	}
	
	public EngineJob() {
		
	}

	/**
	 * Instantiate a Kubernetes job with ID and Job Order file name
	 *
	 * @param jobId            the job ID
	 * @param jobOrderFileName the Job Order file name
	 */
	public EngineJob(Long jobId, String jobOrderFileName) {

		this.jobOrderFileName = jobOrderFileName;

		this.jobId = jobId;
		this.jobName = ProductionPlanner.jobNamePrefix + jobId;
		this.containerName = ProductionPlanner.jobContainerPrefix + jobId;

	}

	public abstract String getJobStepLog(EngineConfig engineConfig);

	/**
	 * Creates a Kubernetes job on the processing facility based on the provided parameters.
	 *
	 * TODO Add retry of database update after job has been sent to Kubernetes
	 engineConfig
	 * @param kubeConfig     The processing facility's kube configuration
	 * @param stdoutLogLevel The log level for stdout
	 * @param stderrLogLevel The log level for stderr
	 * @return The kube job
	 * @throws Exception if an error occurs during job creation
	 */
	//@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	public abstract EngineJob createJob(EngineConfig engineConfig, String stdoutLogLevel, String stderrLogLevel) throws Exception;

	/**
	 * Gets the kube configuration
	 *
	 * @return the engineConfig
	 */
	public EngineConfig getEngineConfig() {
		return engineConfig;
	}

	/**
	 * If the kube configuration is available, search for pods associated with the job, retrieve the job information, and update the
	 * job step accordingly. Then, start a KubeJobFinish thread to monitor the completion of the kube job.
	 *
	 * @param engineConfig The kube configuration associated with the processing facility
	 * @param jobName    The name of the kube job
	 */
	public abstract void finish(EngineConfig engineConfig, String jobName);

	/*
	 * TODO Analyze pod. The phase of a Pod is a simple, high-level summary of where the Pod is in its lifecycle. The conditions
	 * array, the reason and message fields, and the individual container status arrays contain more detail about the pod's status.
	 * There are five possible phase values:
	 *
	 * Pending: The pod has been accepted by the Kubernetes system, but one or more of the container images has not been created.
	 * This includes time before being scheduled as well as time spent downloading images over the network, which could take a
	 * while.
	 *
	 * Running: The pod has been bound to a node, and all of the containers have been created. At least one container is still
	 * running, or is in the process of starting or restarting.
	 *
	 * Succeeded: All containers in the pod have terminated in success, and will not be restarted.
	 *
	 * Failed: All containers in the pod have terminated, and at least one container has terminated in failure. The container either
	 * exited with non-zero status or was terminated by the system. *
	 *
	 * Unknown: For some reason the state of the pod could not be obtained, typically due to an error in communicating with the host
	 * of the pod. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#pod-phase
	 *
	 * Conditions: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-conditions
	 */

	/**
	 * Update all the information of a Kubernetes job which is stored in the job step.
	 *
	 * This method retrieves the information of a Kubernetes job with the provided name and updates the corresponding job step
	 * accordingly. It also performs additional checks and updates based on the job and pod status. The method returns the success
	 * status of the update operation.
	 *
	 * @param jobName The name of the Kubernetes job
	 * @return The result of the update operation: TRUE if the update was successful, FALSE if an error occurred, and CHANGED if the
	 *         job step was modified by others during the update process
	 */
	public abstract Boolean updateInfo(String jobName);

	/**
	 * Retrieves and store the completion information of the Kubernetes job specified by the given job name. Additionally, perform
	 * cleanup operations and delete the Kubernetes job.
	 *
	 * @param jobName The name of the Kubernetes job
	 * @return True if the update and deletion were successful, false otherwise
	 */
	public abstract boolean updateFinishInfoAndDelete(String jobName);

	/**
	 * Sets the generation time of the given product to the provided time, and recursively sets the generation time for all its
	 * component products. If a retention period is specified, it also sets the eviction time of the product. The updated product is
	 * then saved in the repository.
	 *
	 * @param product         The product to set the generation time for
	 * @param genTime         The generation time to set
	 * @param retentionPeriod The retention period for the product
	 */
	protected void setGenerationTime(Product product, Instant genTime, Duration retentionPeriod) {
		if (logger.isTraceEnabled())
			logger.trace(">>> setGenerationTime({}, {}, {})", (null == product ? "null" : product.getId()), genTime,
					retentionPeriod);

		if (product != null && genTime != null) {
			product.setGenerationTime(genTime);

			if (retentionPeriod != null) {
				product.setEvictionTime(genTime.plus(retentionPeriod));
			}

			// Recursively set the generation time for component products
			for (Product p : product.getComponentProducts()) {
				setGenerationTime(p, genTime, retentionPeriod);
			}

			// Save the updated product in the repository
			RepositoryService.getProductRepository().save(product);
		}
	}

	/**
	 * Retrieves the maximum value of CPUs required by the processor tasks.
	 *
	 * @param processor The Processor object
	 * @param cpus      The default value of CPUs
	 * @return The maximum value of CPUs required
	 */
	protected Integer getCPUs(Processor processor, Integer cpus) {
		if (processor != null) {
			// Iterate over the tasks of the processor
			for (Task t : processor.getTasks()) {
				// Update 'cpus' if the task requires more CPUs than the current maximum
				if (t.getNumberOfCpus() != null && t.getNumberOfCpus() > cpus) {
					cpus = t.getNumberOfCpus();
				}
			}
		}

		return cpus;
	}

	/**
	 * Gets the minimum memory requirement among the tasks of a processor.
	 *
	 * @param processor        The Processor
	 * @param defaultMemoryMin The default minimum memory value
	 * @return The minimum memory requirement among the tasks
	 */
	protected Integer getMinMemory(Processor processor, Integer defaultMemoryMin) {
		if (processor != null) {
			for (Task task : processor.getTasks()) {
				if (task.getMinMemory() != null && task.getMinMemory() > defaultMemoryMin) {
					defaultMemoryMin = task.getMinMemory();
				}
			}
		}

		return defaultMemoryMin;
	}

	/**
	 * Updates the job step log based on the latest pod's log.
	 *
	 * @param jobStep The JobStep to update the log for
	 */
	protected abstract void updateJobLog(JobStep jobStep);

	protected void reexecuteProductQueries() {

		TransactionTemplate transactionTemplate = new TransactionTemplate(this.engineConfig.getProductionPlanner().getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.setReadOnly(false);
		transactionTemplate.execute(status -> {

			// Find the job step in the database
			Optional<JobStep> jobStepOptional = RepositoryService.getJobStepRepository().findById(this.getJobId());
			if (jobStepOptional.isEmpty()) {
				logger.log(PlannerMessage.JOB_STEP_NOT_FOUND, this.getJobId());
				return null;
			}
			JobStep js = jobStepOptional.get();

			// Iterate over the input product queries of the job step
			for (ProductQuery productQuery : js.getInputProductQueries()) {
				// Execute the product query always
				ProductQueryService productQueryService = UtilService.getJobStepUtil().getProductQueryService();
				if (productQueryService.executeQuery(productQuery, false)) {
					// If the query is successfully executed, update its state and save
					RepositoryService.getProductQueryRepository().save(productQuery);
				}
			}
			return null;
		});
	}

}
