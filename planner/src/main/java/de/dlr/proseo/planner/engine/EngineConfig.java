/**
 * EngineConfig.java
 *
 * © 2026 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.engine;

import java.util.HashMap;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.enums.ProcessingEngineType;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;


/**
 * Represents the connection to the Engine API
 *
 * @author Ernst Melchinger
 */
public abstract class EngineConfig {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(EngineConfig.class);

	/** The production planner instance */
	protected ProductionPlanner productionPlanner;
	
	/** Map containing the creating Kubernetes jobs */
	private HashMap<Long, Long> jobCreatingList = null;

	/** Map containing the created engine jobs */
	protected HashMap<String, EngineJob> engineJobList = null;
	
	/** Number of ready worker nodes */
	protected int workerCnt = 0;

	/**
	 * TODO If that is correct, maybe change parameter name to "name"
	 *
	 * The name of the facility
	 */
	private String id;

	/** The id of the facility */
	private long longId;

	/** The version of the facility */
	private long version;

	/** The facility description */
	private String description;

	/** The run state the facility currently is in */
	protected FacilityState facilityState;

	/** The maximum of jobs per node */
	private Integer maxJobsPerNode;

	/** The URL of the facility */
	protected String url;

	/** The storage manager URL */
	private String storageManagerUrl;

	/** The URL to access this facility's storage manager from an external client (via PRIP API) */
	private String externalStorageManagerUrl;

	/** The default storage type */
	private StorageType storageType;

	/** The processing engine type */
	private ProcessingEngineType processingEngineType;

	/** Authentication token for connecting to this facility's processing engine (Kubernetes instance) */
	protected String processingEngineToken;

	/**
	 * URL of the locally accessible Storage Manager instance on a specific processing node (to be used by the Processing Engine).
	 * This URL shall contain the string "%NODE_IP%", which will be replaced by the actual node IP of the Kubernetes worker node.
	 */
	private String localStorageManagerUrl;

	/** User name for connecting to the Storage Manager (locally and from external services) */
	private String storageManagerUser;

	/** Password for connecting to the Storage Manager (locally and from external services) */
	private String storageManagerPassword;

	/** The processing facility */
	protected ProcessingFacility processingFacility;

	/** TODO Please define. */
	private int nodesDelta = 0;

	/**
	 * @return the processingEngineType
	 */
	public ProcessingEngineType getProcessingEngineType() {
		return processingEngineType;
	}

	/**
	 * @return the jobCreatingList
	 */
	public HashMap<Long, Long> getJobCreatingList() {
		return jobCreatingList;
	}

	/**
	 * @return the engineJobList
	 */
	public HashMap<String, EngineJob> getEngineJobList() {
		return engineJobList;
	}

	/**
	 * @param engineJobList the engineJobList to set
	 */
	public void setEngineJobList(HashMap<String, EngineJob> engineJobList) {
		this.engineJobList = engineJobList;
	}

	/**
	 * @param jobCreatingList the jobCreatingList to set
	 */
	public void setJobCreatingList(HashMap<Long, Long> jobCreatingList) {
		this.jobCreatingList = jobCreatingList;
	}

	/**
	 * Returns the production planner.
	 *
	 * @return the production planner
	 */
	public ProductionPlanner getProductionPlanner() {
		return productionPlanner;
	}

	/**
	 * Returns the maximum number of jobs per node.
	 *
	 * @return the maximum number of jobs per node
	 */
	public Integer getMaxJobsPerNode() {
		return maxJobsPerNode;
	}

	/**
	 * Sets the maximum number of jobs per node.
	 *
	 * @param maxJobsPerNode the maximum number of jobs per node to set
	 */
	public void setMaxJobsPerNode(Integer maxJobsPerNode) {
		this.maxJobsPerNode = maxJobsPerNode;
	}

	/**
	 * Returns the storage manager password.
	 *
	 * @return the storage manager password
	 */
	public String getStorageManagerPassword() {
		return storageManagerPassword;
	}

	/**
	 * Sets the storage manager password.
	 *
	 * @param storageManagerPassword the storage manager password to set
	 */
	public void setStorageManagerPassword(String storageManagerPassword) {
		this.storageManagerPassword = storageManagerPassword;
	}

	/**
	 * Returns the processing engine token.
	 *
	 * @return the processing engine token
	 */
	public String getProcessingEngineToken() {
		return processingEngineToken;
	}

	/**
	 * Returns the local storage manager URL.
	 *
	 * @return the local storage manager URL
	 */
	public String getLocalStorageManagerUrl() {
		return localStorageManagerUrl;
	}

	/**
	 * Returns the storage manager user.
	 *
	 * @return the storage manager user
	 */
	public String getStorageManagerUser() {
		return storageManagerUser;
	}

	/**
	 * Sets the storage manager URL.
	 *
	 * @param storageManagerUrl the storage manager URL to set
	 */
	public void setStorageManagerUrl(String storageManagerUrl) {
		this.storageManagerUrl = storageManagerUrl;
	}

	/**
	 * Returns the external storage manager URL.
	 *
	 * @return the external storage manager URL
	 */
	public String getExternalStorageManagerUrl() {
		return externalStorageManagerUrl;
	}

	/**
	 * Sets the external storage manager URL.
	 *
	 * @param externalStorageManagerUrl the external storage manager URL to set
	 */
	public void setExternalStorageManagerUrl(String externalStorageManagerUrl) {
		this.externalStorageManagerUrl = externalStorageManagerUrl;
	}

	/**
	 * Sets the processing engine token.
	 *
	 * @param processingEngineToken the processing engine token to set
	 */
	public void setProcessingEngineToken(String processingEngineToken) {
		this.processingEngineToken = processingEngineToken;
	}

	/**
	 * Sets the local storage manager URL.
	 *
	 * @param localStorageManagerUrl the local storage manager URL to set
	 */
	public void setLocalStorageManagerUrl(String localStorageManagerUrl) {
		this.localStorageManagerUrl = localStorageManagerUrl;
	}

	/**
	 * Sets the storage manager user.
	 *
	 * @param storageManagerUser the storage manager user to set
	 */
	public void setStorageManagerUser(String storageManagerUser) {
		this.storageManagerUser = storageManagerUser;
	}

	/**
	 * Returns the storage type.
	 *
	 * @return the storage type
	 */
	public StorageType getStorageType() {
		return storageType;
	}

	/**
	 * Sets the storage type.
	 *
	 * @param storageType the storage type to set
	 */
	public void setStorageType(StorageType storageType) {
		this.storageType = storageType;
	}

	/**
	 * Returns the processing facility.
	 *
	 * @return the processing facility
	 */
	public ProcessingFacility getProcessingFacility() {
		return processingFacility;
	}

	/**
	 * Sets the processing facility.
	 *
	 * @param processingFacility the processing facility to set
	 */
	public void setProcessingFacility(ProcessingFacility processingFacility) {
		this.processingFacility = processingFacility;
	}

	/**
	 * Instantiates a new EngineConfig object with a processing facility.
	 *
	 * @param processingFacility the processing facility to set
	 * @param planner            the production planner
	 */
	public EngineConfig(ProcessingFacility processingFacility, ProductionPlanner planner) {
		setFacility(processingFacility);
		productionPlanner = planner;
		jobCreatingList = new HashMap<>();
	}

	/**
	 * Sets all locally stored variables based on the provided processing facility.
	 *
	 * @param processingFacility the processing facility
	 */
	public void setFacility(ProcessingFacility processingFacility) {
		id = processingFacility.getName();
		longId = processingFacility.getId();
		version = processingFacility.getVersion();
		description = processingFacility.getDescription();
		facilityState = processingFacility.getFacilityState();
		url = processingFacility.getProcessingEngineUrl();
		storageManagerUrl = processingFacility.getStorageManagerUrl();
		storageType = processingFacility.getDefaultStorageType();
		localStorageManagerUrl = processingFacility.getLocalStorageManagerUrl();
		externalStorageManagerUrl = processingFacility.getExternalStorageManagerUrl();
		processingEngineToken = processingFacility.getProcessingEngineToken();
		storageManagerUser = processingFacility.getStorageManagerUser();
		storageManagerPassword = processingFacility.getStorageManagerPassword();
		maxJobsPerNode = processingFacility.getMaxJobsPerNode();
		this.processingFacility = processingFacility;
		jobCreatingList = new HashMap<>();
	}

	/**
	 * Returns the storage manager URL.
	 *
	 * @return the storage manager URL
	 */
	public String getStorageManagerUrl() {
		return storageManagerUrl;
	}

	/**
	 * Returns the worker count.
	 *
	 * @return the worker count
	 */
	public int getWorkerCnt() {
		return workerCnt;
	}

	/**
	 * Connects to the Kubernetes cluster.
	 *
	 * @return true if connected, otherwise false
	 */
	public abstract boolean connect();

	/**
	 * Checks if the connection to the engine cluster is established.
	 *
	 * @return true if connected, otherwise false
	 */
	public abstract boolean isConnected();

	/**
	 * Checks whether a new Kubernetes job could run.
	 *
	 * @return true if a new job could run, otherwise false
	 */
	public boolean couldJobRun(Long jsId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> couldJobRun()");

		if (jsId != null) {
			if (jobCreatingList.containsKey(jsId)) {
				return false;
			}
		}
		// Check the facility state
		if (getFacilityState(null) == FacilityState.DISABLED || getFacilityState(null) == FacilityState.STOPPED
				|| getFacilityState(null) == FacilityState.STOPPING || getFacilityState(null) == FacilityState.STARTING) {
			// not available for jobs
			return false;
		}

		// Check the maximum number of jobs per node
		Integer maxJobsPerNode = 1;
		if (getMaxJobsPerNode() != null) {
			maxJobsPerNode = getMaxJobsPerNode();
		}

		return (engineJobList.size() < ((getWorkerCnt() * maxJobsPerNode) + nodesDelta));
	}
	

	/**
	 * Looks for the Kubernetes job with the specified name.
	 *
	 * @param name the name of the Kubernetes job
	 * @return the found Kubernetes job or null if not found
	 */
	public EngineJob getEngineJob(String name) {
		return engineJobList.get(name);
	}

	/** Synchronizes the Kubernetes jobs and the internal KubeJob list with the Planner. */
	public abstract void sync();

	/**
	 * Creates a new job on the cluster with the specified name and log levels.
	 *
	 * @param name           the name of the new job
	 * @param stdoutLogLevel the log level for stdout
	 * @param stderrLogLevel the log level for stderr
	 * @return the created job or null
	 */
	// @Transactional(isolation = Isolation.REPEATABLE_READ)
	public abstract EngineJob createJob(String name, String stdoutLogLevel, String stderrLogLevel);
	/**
	 * Creates a new job on the cluster with the specified ID and log levels.
	 *
	 * @param id             the ID of the job
	 * @param stdoutLogLevel the log level for stdout
	 * @param stderrLogLevel the log level for stderr
	 * @return the created job or null
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public abstract EngineJob createJob(long id, String stdoutLogLevel, String stderrLogLevel);

	/**
	 * Deletes a job from the cluster.
	 *
	 * @param jobToDelete the job to delete
	 * @return true if the job was successfully deleted, otherwise false
	 */
	public boolean deleteJob(EngineJob jobToDelete) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteJob({})", (null == jobToDelete ? "null" : jobToDelete.getJobId()));

		if (jobToDelete != null) {
			return (deleteJob(jobToDelete.getJobName()));
		} else {
			return false;
		}
	}

	/**
	 * Deletes a named job from the cluster.
	 *
	 * @param name the name of the job to delete
	 * @return true if the job was successfully deleted, otherwise false
	 */
	public abstract boolean deleteJob(String name);

	/**
	 * Retrieves the ID.
	 *
	 * @return the ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Retrieves the long ID.
	 *
	 * @return the long ID
	 */
	public long getLongId() {
		return longId;
	}

	/**
	 * Retrieves the version.
	 *
	 * @return the version
	 */
	public long getVersion() {
		return version;
	}

	/**
	 * Retrieves the URL.
	 *
	 * @return the URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Retrieves the processing engine URL.
	 *
	 * @return the processing engine URL
	 */
	public String getProcessingEngineUrl() {
		return url;
	}

	/**
	 * Retrieves the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
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

}
