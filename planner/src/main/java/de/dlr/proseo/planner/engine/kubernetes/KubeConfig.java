/**
 * KubeConfig.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.engine.kubernetes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.engine.EngineConfig;
import de.dlr.proseo.planner.engine.EngineJob;
import de.dlr.proseo.planner.rest.model.PodKube;
import de.dlr.proseo.planner.util.UtilService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Taint;
import io.kubernetes.client.util.Config;

/**
 * Represents the connection to the Kubernetes API
 *
 * @author Ernst Melchinger
 */
public class KubeConfig extends EngineConfig{

	/** Dummy Job Order file name (only used in development environment) **/
	private static final String DUMMY_JOF_FILENAME = "/testdata/test1.pl";

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(KubeConfig.class);

	/** List of Kubernetes nodes */
	private V1NodeList kubeNodes = null;

	/** The Kubernetes API client */
	private ApiClient client;

	/** The Kubernetes core API V1 */
	private CoreV1Api apiV1;

	/** The Kubernetes batch API V1 */
	private BatchV1Api batchApiV1;

	// No need to create own namespace, as there is only one "user" (prosEO)
	/** The namespace */
	private String namespace = "default";

	/**
	 * Returns the API client.
	 *
	 * @return the API client
	 */
	public ApiClient getClient() {
		return client;
	}

	/**
	 * Returns the CoreV1Api.
	 *
	 * @return the CoreV1Api
	 */
	public CoreV1Api getApiV1() {
		return apiV1;
	}

	/**
	 * Returns the BatchV1Api.
	 *
	 * @return the BatchV1Api
	 */
	public BatchV1Api getBatchApiV1() {
		return batchApiV1;
	}

	public KubeConfig(ProcessingFacility processingFacility, ProductionPlanner planner) {
		super(processingFacility, planner);
	}
	
	/**
	 * Connects to the Kubernetes cluster.
	 *
	 * @return true if connected, otherwise false
	 */
	public boolean connect() {
		if (logger.isTraceEnabled())
			logger.trace(">>> connect()");

		// If the facility is disabled or stopped, no need to connect.
		if (getFacilityState(null) == FacilityState.DISABLED || getFacilityState(null) == FacilityState.STOPPED)
			return false;

		// Check if already connected
		if (isConnected()) {
			return true;
		} else {
			// Initialize the kubeJobList
			engineJobList = new HashMap<>();

			// Try to connect using processingEngineToken if available
			if (StringUtils.hasText(processingEngineToken) && StringUtils.hasText(url)) {
				try {
					client = null;
					client = Config.fromToken(url, processingEngineToken, false);
				} catch (Exception e) {
					logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
				}
			}

			// If connection using processingEngineToken failed, try other methods
			if (client == null) {
				try {
					// Describes Kubernetes in Docker
					String kubeConfig = ProductionPlanner.config.getProductionPlannerKubeConfig();
					if (kubeConfig == null || kubeConfig.isEmpty()) {
						kubeConfig = "kube_config";
					}
					client = Config.fromConfig(kubeConfig);
				} catch (IOException e) {
					logger.log(PlannerMessage.CONFIGURATION_ACCESS_FAILED, e.getClass() + " - " + e.getMessage());

					if (logger.isDebugEnabled()) {
						logger.debug("IO exception encountered: ", e);
					}
				}

				// If connection using kube_config failed, try connecting directly using the URL
				if (client == null && StringUtils.hasText(url)) {
					client = Config.fromUrl(url, false);
				}
			}

			// If all connection attempts failed, log the failure
			if (client == null) {
				logger.log(PlannerMessage.FACILITY_CONNECTION_FAILED, url);
				return false;
			}

			// Enable debugging if trace level logging is enabled
			if (logger.isTraceEnabled()) {
				client.setDebugging(true);
			}

			// Set the default API client configuration
			Configuration.setDefaultApiClient(client);

			// Initialize CoreV1Api and BatchV1Api
			apiV1 = new CoreV1Api();
			batchApiV1 = new BatchV1Api();
			if (apiV1 == null || batchApiV1 == null) {
				// TODO Is that even possible?
				apiV1 = null;
				batchApiV1 = null;
				return false;
			} else {
				// Allow more response time and enhance time out
				client.setReadTimeout(100000);
				client.setWriteTimeout(100000);
				client.setConnectTimeout(100000);

				// Synchronize the Kubernetes cluster and the planner
				sync();
			}

			return true;
		}
	}

	/**
	 * Checks if the connection to the Kubernetes cluster is established.
	 *
	 * @return true if connected, otherwise false
	 */
	public boolean isConnected() {
		if (getFacilityState(null) == FacilityState.DISABLED || apiV1 == null) {
			return false;
		} else {
			return true;
		}
	}

	/** Synchronizes the Kubernetes jobs and the internal KubeJob list with the Planner. */
	public void sync() {
		if (logger.isTraceEnabled())
			logger.trace(">>> sync()");

		// Step 1: Check facility state
		if (getFacilityState(null) == FacilityState.DISABLED || getFacilityState(null) == FacilityState.STOPPED
				|| getFacilityState(null) == FacilityState.STARTING) {
			// Nothing to do if facility state is disabled, stopped, or starting
			return;
		}

		// Step 2: Rebuild runtime data by retrieving all jobs from all namespaces
		V1JobList k8sJobList = null;
		try {
			getNodeInfo(); // TODO Should this info be processed?
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			return;
		}
		try {
			k8sJobList = batchApiV1.listJobForAllNamespaces().execute();
		} catch (ApiException e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			return;
		}

		// Step 3: Update kubeJob list
		Map<String, V1Job> kubeJobs = new HashMap<>();
		for (V1Job kubeJob : k8sJobList.getItems()) {
			String kubeJobName = kubeJob.getMetadata().getName();
			kubeJobs.put(kubeJobName, kubeJob);

			// Ensure that the kube job is contained in the configuration's map of kube jobs
			if (getEngineJob(kubeJobName) == null) {
				if (!getEngineJobList().containsKey(kubeJob.getMetadata().getName())) {
					KubeJob newKubeJob = new KubeJob();
					newKubeJob = newKubeJob.rebuild(this, kubeJob);
					if (newKubeJob != null) {
						getEngineJobList().put(newKubeJob.getJobName(), newKubeJob);
					}
				}
			}
		}

		// Step 4: Remove elements from KubeJob list without existing Kubernetes jobs
		List<EngineJob> kjList = new ArrayList<>();
		kjList.addAll(engineJobList.values());
		for (EngineJob kj : kjList) {
			if (!kubeJobs.containsKey(kj.getJobName())) {
				// TODO Did we not just add missing jobs to kubeJobList in step 3? If this is about non-prosEO jobs, we could have
				// removed them before.
				engineJobList.remove(kj.getJobName());
			}
		}

		// Step 5: Check if any Kubernetes job has finished without triggering a message event to the Planner
		for (V1Job aJob : k8sJobList.getItems()) {
			String kubeJobName = aJob.getMetadata().getName();
			EngineJob kubeJob = engineJobList.get(kubeJobName);
			if (kubeJob != null && !getProductionPlanner().getFinishThreads().containsKey(kubeJobName)) {
				kubeJob.updateFinishInfoAndDelete(kubeJobName);
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
						String jobName = ProductionPlanner.jobNamePrefix + jobStepId;
						Optional<JobStep> jobStepOpt = RepositoryService.getJobStepRepository().findById(jobStepId);
						JobStep jobStep = null;
						if (jobStepOpt.isPresent()) {
							jobStep = jobStepOpt.get();
						} else {
							return null;
						}
						// If no job with this name is running, change state to FAILED and set info in log
						if (!kubeJobs.containsKey(jobName)) {
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
	 * Retrieves the list of pods in the cluster.
	 *
	 * @return the list of pods
	 */
	public V1PodList getPodList() {
		V1PodList podList = null;

		try {
			podList = apiV1.listPodForAllNamespaces().execute();
		} catch (ApiException e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
		}

		return podList;
	}

	/**
	 * Retrieves the list of jobs in the cluster.
	 *
	 * @return the list of jobs
	 */
	public V1JobList getJobList() {
		V1JobList jobList = null;

		try {
			jobList = batchApiV1.listJobForAllNamespaces().execute();
		} catch (ApiException e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
		}

		return jobList;
	}

	/**
	 * Creates a new job on the cluster with the specified name and log levels.
	 *
	 * @param name           the name of the new job
	 * @param stdoutLogLevel the log level for stdout
	 * @param stderrLogLevel the log level for stderr
	 * @return the created job or null
	 */
	// @Transactional(isolation = Isolation.REPEATABLE_READ)
	@Override
	public KubeJob createJob(String name, String stdoutLogLevel, String stderrLogLevel) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJob({}, {}, {})", name, stdoutLogLevel, stderrLogLevel);

		KubeJob newJob = new KubeJob(Long.parseLong(name), DUMMY_JOF_FILENAME);

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
	// @Transactional(isolation = Isolation.REPEATABLE_READ)
	@Override
	public KubeJob createJob(long id, String stdoutLogLevel, String stderrLogLevel) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJob({}, {}, {})", id, stdoutLogLevel, stderrLogLevel);

		KubeJob newJob = new KubeJob(id, DUMMY_JOF_FILENAME);

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
	public boolean deleteJob(String name) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteJob({})", name);

		// Configure deletion options
		V1DeleteOptions opt = new V1DeleteOptions();
		opt.setApiVersion("batchV1");
		opt.setPropagationPolicy("Foreground");
		opt.setGracePeriodSeconds((long) 0);

		// Attempt deletion
		try {
			batchApiV1.deleteNamespacedJob(name, namespace).propagationPolicy("Foreground").execute();
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());

			if ((e instanceof ApiException && ((ApiException) e).getCode() == HttpStatus.NOT_FOUND.value())
					|| e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException) {
				// Nothing to do, because there is a bug in Kubernetes API (IllegalState) or the job is already gone (Api)
				return true;
			}
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			return false;
		} finally {
			engineJobList.remove(name);
		}

		return true;
	}

	/**
	 * Retrieves a Kubernetes job with the specified name.
	 *
	 * @param name the name of the job
	 * @return the job found or null
	 */
	public V1Job getV1Job(String name) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getV1Job({})", name);

		V1Job foundJob = null;
		int retryNumber = 0;

		while (retryNumber < 10 && foundJob == null) {
			retryNumber++;

			try {
				foundJob = batchApiV1.readNamespacedJob(name, namespace).execute();
			} catch (ApiException e) {
				if (e.getCode() == 404) {
					logger.log(PlannerMessage.KUBECONFIG_JOB_NOT_FOUND, name);
					retryNumber = 10;
				} else {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
				}
			} catch (Exception e) {
				if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException) {
					// Nothing to do, as there is a bug in the Kubernetes API
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
				}

				logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());

				if (logger.isDebugEnabled())  logger.debug("... exception stack trace: ", e);
			}

			if ((retryNumber < 10 && foundJob == null)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
				}
			}
		}

		return foundJob;
	}

	/**
	 * Retrieves a Kubernetes pod with the specified name.
	 *
	 * @param name the name of the pod
	 * @return the pod found or null
	 */
	public V1Pod getV1Pod(String name) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getV1Pod({})", name);

		V1Pod retrievedPod = null;
		int retryNumber = 0;

		while (retryNumber < ProseoUtil.K8S_MAX_RETRY && retrievedPod == null) {
			retryNumber++;
			
			try {
				retrievedPod = apiV1.readNamespacedPod(name, namespace).execute();
			} catch (Exception e) {
				if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException) {
					// Nothing to do, as there is a bug in the Kubernetes API
				} else if (e instanceof ApiException && ((ApiException) e).getCode() == 404) {
					// Pod not found
					retryNumber = ProseoUtil.K8S_MAX_RETRY;
				} else {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
				}
			}
			
			if ((retryNumber < ProseoUtil.K8S_MAX_RETRY && retrievedPod == null)) {
				ProseoUtil.kubeWait(retryNumber);
			}
		}

		return retrievedPod;
	}

	/**
	 * Retrieves the namespace.
	 *
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Retrieves the facility state.
	 *
	 * @return the facility state
	 */
	public FacilityState getFacilityState(ProcessingFacility facility) {
		if (facility != null) {
			facilityState = facility.getFacilityState();
		} else {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			// Check the status of the requested processing facility
			final FacilityState response = transactionTemplate.execute((status) -> {
				if (getProcessingFacility() != null) {
					ProcessingFacility pf = RepositoryService.getFacilityRepository().findByName(getProcessingFacility().getName());
					facilityState = pf.getFacilityState();	
					return facilityState;
				}
				return FacilityState.DISABLED;
			});
			facilityState = response;
		}
		return facilityState;
	}

	/**
	 * Sets the facility state.
	 *
	 * @param facilityState the facility state to set
	 */
	public void setFacilityState(FacilityState facilityState) {
		this.facilityState = facilityState;
	}

	/**
	 * Deletes a job.
	 *
	 * @param name the name of the pod/job
	 * @return true if the job was successfully deleted, otherwise false
	 */
	public boolean deletePodNamed(String name) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deletePodNamed({})", name);

		if (this.isConnected()) {
			return this.deleteJob(name);
		}
		return false;
	}

	/**
	 * Deletes all pods with the specified status.
	 *
	 * @param status the status of the pods to delete
	 * @return true if the pods were successfully deleted, otherwise false
	 */
	public boolean deletePodsStatus(String status) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deletePodsStatus({})", status);

		// Check if the Kubernetes cluster is connected
		if (this.isConnected()) {
			// Get the list of jobs in the cluster
			V1JobList jobList = this.getJobList();

			if (jobList != null) {
				for (V1Job job : jobList.getItems()) {
					// Create a PodKube object from the job
					PodKube podKube = new PodKube(job);

					// Check if the PodKube object is not null and has the specified status
					if (podKube != null && podKube.hasStatus(status)) {
						// Delete the job (which corresponds to the pod)
						this.deleteJob(podKube.getName());
					}
				}
				// Pods with the specified status have been successfully deleted
				return true;
			}
		}

		return false;
	}

	/**
	 * Searches for ready worker nodes on the Kubernetes cluster.
	 *
	 * @return true if at least one worker node is ready, otherwise false
	 */
	public boolean getNodeInfo() {
		if (logger.isTraceEnabled())
			logger.trace(">>> getNodeInfo()");

		kubeNodes = null;
		workerCnt = 0;

		try {
			// Retrieve the list of nodes from the Kubernetes API
			kubeNodes = apiV1.listNode().execute();

			if (kubeNodes != null) {
				for (V1Node node : kubeNodes.getItems()) {
					if (node.getSpec() != null) {
						if (node.getSpec().getUnschedulable() == null || !node.getSpec().getUnschedulable()) {
							if (node.getSpec().getTaints() != null) {
								// Check the taints of the node and exclude nodes with specific taint effects
								if (CollectionUtils.isEmpty(node.getSpec().getTaints())) {
									// Increment the worker count as the node is ready
									workerCnt++;
								} else {
									for (V1Taint taint : node.getSpec().getTaints()) {
										if (taint.getEffect() != null) {
											if (!taint.getEffect().equalsIgnoreCase("NoSchedule")
													&& !taint.getEffect().equalsIgnoreCase("NoExecute")) {
												// Increment the worker count as the node is ready
												workerCnt++;
											}
										}
									}
								}
							}
						} else {
							// Increment the worker count as the node is ready
							workerCnt++;
						}
					} else {
						// Increment the worker count as the node is ready
						workerCnt++;
					}
				}
			}
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
			return false;
		}

		// Return true if at least one worker node is ready
		return workerCnt > 0;
	}

}
