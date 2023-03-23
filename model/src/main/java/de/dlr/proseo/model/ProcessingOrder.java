/**
 * ProcessingOrder.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderSource;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;

/**
 * A customer order to process a specific set of ProductClasses for a specific period of time using a specific set of
 * ConfiguredProcessors. An order may have properties like a product quality indicator (test vs operational), specific product
 * delivery endpoints, specific (potentially mission-dependent) product generation attributes (e. g. a Copernicus collection
 * number) etc.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = {
	@Index(unique = true, columnList = "mission_id, identifier"),
	@Index(unique = true, columnList = "uuid"),
	@Index(unique = false, columnList = "execution_time") 
})
public class ProcessingOrder extends PersistentObject {

	private static final String MSG_ILLEGAL_STATE_TRANSITION = "Illegal order state transition from %s to %s";
	private static final String MSG_SLICING_DURATION_NOT_ALLOWED = "Setting of slicing duration not allowed for slicing type ";
	private static final String MSG_SLICING_OVERLAP_NOT_ALLOWED = "Setting of slicing overlap not allowed for slicing type ";

	/** Mission, to which this order belongs */
	@ManyToOne
	private Mission mission;
	
	/** User-defined order identifier (unique within the mission) */
	@Column(nullable = false)
	private String identifier;
	
	/** The universally unique identifier (UUID) for this order */
	@Column(nullable = false)
	private UUID uuid;
	
	/**
	 * Priority of the ProcessingOrder (lower number means lower priority; value range 1..100 is defined for the ODIP,
	 * but other values are allowed outside On-Demand Production, including negative numbers). Default value is 50.
	 */
	private Integer priority = 50;
	
	/** State of the processing order */
	@Enumerated(EnumType.STRING)
	private OrderState orderState;
	
	/** 
	 * Explanatory message describing the reason for the latest state change ("StatusMessage" in ODPRIP ICD),
	 * mandatory for ProcessingOrders created via the ODIP
	 */
	private String stateMessage;
	
	/** Source application for the processing order */
	@Enumerated(EnumType.STRING)
	private OrderSource orderSource;
	
	/**
	 * Date and time at which the ProcessingOrder was received
	 * (mandatory for ProcessingOrders created via the ODIP; "SubmissionDate" in ODPRIP ICD)
	 */
	@Column(name = "submission_time", columnDefinition = "TIMESTAMP")
	private Instant submissionTime;
	
	/** Earliest execution time (optional, used for scheduling) */
	@Column(name = "execution_time", columnDefinition = "TIMESTAMP")
	private Instant executionTime;
	
	/**
	 * Date and time at which the releasing of the ProcessingOrder was completed
	 * (used during priority calculation by the Production Planner)
	 */
	@Column(name = "release_time", columnDefinition = "TIMESTAMP")
	private Instant releaseTime;
	
	/**
	 * Estimated date and time when the output product(s) will be available for download from the (OD)PRIP
	 * (mandatory for ProcessingOrders created via the ODIP; "EstimatedDate" in ODPRIP ICD)
	 */
	@Column(name = "estimated_completion_time", columnDefinition = "TIMESTAMP")
	private Instant estimatedCompletionTime;
	
	/**
	 * Date and time when the output product(s) was/were available for download from the (OD)PRIP
	 * (mandatory for ProcessingOrders created via the ODIP, once they are in state "COMPLETED"; "CompletedDate" in ODPRIP ICD)
	 */
	@Column(name = "actual_completion_time", columnDefinition = "TIMESTAMP")
	private Instant actualCompletionTime;
	
	/**
	 * Time for automatic order deletion, if an orderRetentionPeriod is set for the mission and
	 * the productionType is SYSTEMATIC_PRODUCTION.
	 */
	@Column(name = "eviction_time", columnDefinition = "TIMESTAMP")
	private Instant evictionTime;
	
	/**
	 * The start time of the time interval to process. If a range of orbit numbers is given, this time is set to the earliest
	 * start time of the selected orbits.
	 */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant startTime;
	
	/**
	 * The end time of the time interval to process. If a range of orbit numbers is given, this time is set to the latest
	 * stop time of the selected orbits.
	 */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant stopTime;
	
	/**
	 * Method for slicing the orbit time interval into jobs for product generation (default "ORBIT")
	 */
	@Enumerated(EnumType.STRING)
	private OrderSlicingType slicingType = OrderSlicingType.ORBIT;
	
	/**
	 * Duration of a time slice for slicing type TIME_SLICE
	 */
	private Duration sliceDuration = null;
	
	/**
	 * Overlap between adjacent time slices, half of the overlap is added at each end of the slice time interval
	 */
	private Duration sliceOverlap = Duration.ZERO;
	
	/** Identification of the input product to use for On-Demand Production */
	private InputProductReference inputProductReference;
	
	/**
	 * Filter conditions to apply to input products of a specific product class in addition to filter conditions contained
	 * in the applicable selection rule
	 */
	@ManyToMany
	private Map<ProductClass, InputFilter> inputFilters = new HashMap<>();
	
	/**
	 * Processing option settings (for on-demand processing called "WorkflowOptions" in the ICD); these options will be passed
	 * to the data processors in the "Dynamic Processing Parameter" section of the Job Order file.
	 * 
	 * If derived from a WorkflowOptions object, then the "Type" value given in that object will be mapped to parameterType
	 * as follows (this mapping will be reversed when creating the Job Order file):
	 * <ul>
	 *   <li>"string" --> "STRING"</li>
	 *   <li>"number" --> if parameterValue is a valid integer then "INTEGER" else "DOUBLE"</li>
	 *   <li>"datenumber" --> "INTEGER"</li>
	 * </ul>
	 */
	@ElementCollection
	private Map<String, Parameter> dynamicProcessingParameters = new HashMap<>();
	
	/**
	 * Set of parameters to apply to a generated product of the referenced product class replacing the general output parameters
	 */
	@ManyToMany
	private Map<ProductClass, ClassOutputParameter> classOutputParameters = new HashMap<>();
	
	/**
	 * Parameters to set for the generated products
	 */
	@ElementCollection
	private Map<String, Parameter> outputParameters = new HashMap<>();
	
	/** Set of requested product classes */
	@ManyToMany
	private Set<ProductClass> requestedProductClasses = new HashSet<>();
	
	/** Set of product classes provided as input data (processing job steps must not be generated) */
	@ManyToMany
	private Set<ProductClass> inputProductClasses = new HashSet<>();
	
	/** The file class of the generated output products (from the list of allowed file classes agreed for the mission) */
	private String outputFileClass;
	
	/** The processing mode to run the processor(s) in (one of the modes specified for the mission) */
	private String processingMode;
	
	/** Production type context, in which the order is running */
	@Enumerated(EnumType.STRING)
	private ProductionType productionType = ProductionType.ON_DEMAND_DEFAULT;
	
	/** The endpoint to send order completion notifications to */
	private NotificationEndpoint notificationEndpoint;
	
	/** 
	 * Period between product generation time and product eviction time for all products generated by job steps of this
	 * processing order (not just those of the output product classes). If the eviction period is not set, it will be taken
	 * from the mission default value; if that is not present either, no eviction time will be computed for the products.
	 */
	private Duration productRetentionPeriod;
	
	/** Indicates whether at least one of the job steps for this order is in state FAILED */
	private Boolean hasFailedJobSteps = false;
	
	/** The workflow applicable for this processing order (only for orders created through the On-Demand Interface Point API */
	@ManyToOne
	private Workflow workflow;
	
	/** The processor configurations for processing the products */
	@ManyToMany
	private Set<ConfiguredProcessor> requestedConfiguredProcessors = new HashSet<>();
	
	/** The orbits, for which products are to be generated */
	@ManyToMany
	private List<Orbit> requestedOrbits = new ArrayList<>();
	
	/** The processing jobs belonging to this order */	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "processingOrder")
	private Set<Job> jobs = new HashSet<>();

	/**
	 * The progress monitoring data for this order
	 */
	@ElementCollection
	private Set<MonOrderProgress> monOrderProgress = new HashSet<>();
	
	/**
	 * Gets the owning mission
	 * 
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Sets the owning mission
	 * 
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}

	/**
	 * Gets the user-defined identifier
	 * 
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the user-defined identifier
	 * 
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Gets the universally unique identifier (UUID)
	 * 
	 * @return the UUID
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Sets the universally unique identifier (UUID)
	 * 
	 * @param uuid the UUID to set
	 */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * Gets the priority value for scheduling
	 * 
	 * @return the priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * Sets the priority value for scheduling
	 * 
	 * @param priority the priority to set
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * Gets the state of the processing order
	 * 
	 * @return the orderState
	 */
	public OrderState getOrderState() {
		return orderState;
	}

	/**
	 * Sets the state of the processing order
	 * 
	 * @param orderState the orderState to set
	 * @throws IllegalStateException if the intended order state transition is illegal
	 */
	@Transactional
	public void setOrderState(OrderState orderState) throws IllegalStateException {
		if (null == this.orderState || this.orderState.equals(orderState) || this.orderState.isLegalTransition(orderState)) {
			this.orderState = orderState;
		} else {
			throw new IllegalStateException(String.format(MSG_ILLEGAL_STATE_TRANSITION,
					this.orderState.toString(), orderState.toString()));
		}
	}

	/**
	 * Check whether a processing order state change is required based on the current state of the contained jobs
	 */
	@Transactional
	public void checkStateChange() {
		hasFailedJobSteps = false;
		
		// Check whether the processing order has any jobs at all
		if (0 == jobs.size()) {
			if (OrderState.INITIAL.equals(orderState)
					|| OrderState.APPROVED.equals(orderState)
					|| OrderState.PLANNING.equals(orderState)
					|| OrderState.PLANNING_FAILED.equals(orderState)
					|| OrderState.COMPLETED.equals(orderState)
					|| OrderState.CLOSED.equals(orderState)) {
				// Do nothing, as in any of these states the order may have no jobs
				return;
			} else {
				// Something is wrong, so setting the order to FAILED to be able to restart/reset it
				orderState = OrderState.FAILED;
				return;
			}
		}
		
		// Prepare counter for job step states
		Map<JobState, Integer> jobStateMap = new HashMap<>();
		for (JobState jobStepState: JobState.values()) {
			jobStateMap.put(jobStepState, 0);
		}
		
		// Check status of job steps
		for (Job job: jobs) {
			// Update job step failure flag
			hasFailedJobSteps = hasFailedJobSteps || job.hasFailedJobSteps();
			
			// Count jobs  per state
			jobStateMap.put(job.getJobState(), jobStateMap.get(job.getJobState()) + 1);
		}
		
		// Update order state according to distribution of job states
		int jobCount = jobs.size();
		int terminatedJobCount = jobStateMap.get(JobState.COMPLETED)
				+ jobStateMap.get(JobState.FAILED) + jobStateMap.get(JobState.CLOSED);
		int activeJobCount = jobStateMap.get(JobState.STARTED) + jobStateMap.get(JobState.ON_HOLD);
		
		// First check, whether all jobs are finished
		if (jobCount == jobStateMap.get(JobState.CLOSED)) {
			// All jobs are CLOSED
			orderState = OrderState.CLOSED;
			Duration retPeriod = getMission().getOrderRetentionPeriod();
			if (retPeriod != null && getProductionType() == ProductionType.SYSTEMATIC) {
				setEvictionTime(Instant.now().plus(retPeriod));
				orderState = OrderState.CLOSED;
			}
		} else if (jobCount == terminatedJobCount) {
			// All jobs are terminated in some way
			if (0 < jobStateMap.get(JobState.FAILED)) {
				orderState = OrderState.FAILED;
			} else {
				orderState = OrderState.COMPLETED;
			}
		// We still have unfinished jobs   && 0 < terminatedJobCount
		} else if (0 == activeJobCount) {
			// At least one job has terminated, but none is currently active
			if (OrderState.SUSPENDING.equals(orderState)) {
				orderState = OrderState.PLANNED;
			} else if (OrderState.RELEASING.equals(orderState)) {
				// Keep status until all jobs are released
				if (0 == jobStateMap.get(JobState.PLANNED)) {
					orderState = OrderState.RUNNING;
				}
			} else {
				// keep state
				// orderState = OrderState.RUNNING;
			}
		} else if (0 < activeJobCount) {
			// At least one job is active
			if (OrderState.SUSPENDING.equals(orderState)) {
				// Do nothing, suspended order is waiting for active jobs to terminate
			} else if (OrderState.RELEASING.equals(orderState)) {
				// Keep status until all jobs are released
				if (0 == jobStateMap.get(JobState.PLANNED)) {
					orderState = OrderState.RUNNING;
				}
			} else if (orderState == OrderState.PLANNED) {
				// The complete order was suspended and some job steps finished later
				// keep the order state
				orderState = OrderState.PLANNED;
			} else {
				orderState = OrderState.RUNNING;
			}
		// No active and no terminated jobs
		} else if (0 < jobStateMap.get(JobState.RELEASED)) {
			// The order was released, but jobs are waiting to start
			if (OrderState.RELEASING.equals(orderState)) {
				if (jobCount == jobStateMap.get(JobState.RELEASED)) {
					// All jobs are released
					orderState = OrderState.RELEASED;
				} else {
					// Do nothing, there are still jobs to release
				}
			} else {
				orderState = OrderState.RELEASED;
			}
		// All jobs should be either in state INITIAL or in state PLANNED
		} else if (jobCount == jobStateMap.get(JobState.PLANNED)) {
			// All jobs are planned
			orderState = OrderState.PLANNED;
		} else {
			if (OrderState.PLANNING_FAILED.equals(orderState)) {
				// Do nothing, further way of action to be decided by operator
			} else {
				// We do have jobs, but they are not fully planned yet
				orderState = OrderState.PLANNING;
			}
		}
		
	}

	/**
	 * Gets a message explaining the latest state change
	 * 
	 * @return the state message
	 */
	public String getStateMessage() {
		return stateMessage;
	}

	/**
	 * Sets a message explaining the latest state change
	 * 
	 * @param stateMessage the state message to set
	 */
	public void setStateMessage(String stateMessage) {
		this.stateMessage = stateMessage;
	}

	/**
	 * Gets the source application for the order
	 * 
	 * @return the order source
	 */
	public OrderSource getOrderSource() {
		return orderSource;
	}

	/**
	 * Sets the source application for the order
	 * 
	 * @param orderSource the order source to set
	 */
	public void setOrderSource(OrderSource orderSource) {
		this.orderSource = orderSource;
	}

	/**
	 * Gets the date and time the order was received
	 * 
	 * @return the submission time
	 */
	public Instant getSubmissionTime() {
		return submissionTime;
	}

	/**
	 * Sets the date and time the order was received
	 * 
	 * @param submissionTime the submission time to set
	 */
	public void setSubmissionTime(Instant submissionTime) {
		this.submissionTime = submissionTime;
	}

	/**
	 * Gets the scheduled execution time (if any)
	 * 
	 * @return the executionTime (may be null)
	 */
	public Instant getExecutionTime() {
		return executionTime;
	}

	/**
	 * Sets the scheduled execution time
	 * 
	 * @param executionTime the executionTime to set (a null value removes an existing execution time)
	 */
	public void setExecutionTime(Instant executionTime) {
		this.executionTime = executionTime;
	}

	/**
	 * Gets the date and time the order was released for processing
	 * 
	 * @return the releaseTime
	 */
	public Instant getReleaseTime() {
		return releaseTime;
	}

	/**
	 * Sets the date and time the order was released for processing
	 * 
	 * @param releaseTime the releaseTime to set
	 */
	public void setReleaseTime(Instant releaseTime) {
		this.releaseTime = releaseTime;
	}

	/**
	 * Gets the date and time the order is expected to be completed
	 * 
	 * @return the estimated completion time
	 */
	public Instant getEstimatedCompletionTime() {
		return estimatedCompletionTime;
	}

	/**
	 * Sets the date and time the order is expected to be completed
	 * 
	 * @param estimatedCompletionTime the estimated completion time to set
	 */
	public void setEstimatedCompletionTime(Instant estimatedCompletionTime) {
		this.estimatedCompletionTime = estimatedCompletionTime;
	}

	/**
	 * Gets the date and time the order was actually completed
	 * 
	 * @return the actualCompletionTime
	 */
	public Instant getActualCompletionTime() {
		return actualCompletionTime;
	}

	/**
	 * Sets the date and time the order was actually completed
	 * 
	 * @param actualCompletionTime the actualCompletionTime to set
	 */
	public void setActualCompletionTime(Instant actualCompletionTime) {
		this.actualCompletionTime = actualCompletionTime;
	}

	/**
	 * Gets the order eviction time (if any)
	 * 
	 * @return the eviction time
	 */
	public Instant getEvictionTime() {
		return evictionTime;
	}

	/**
	 * Sets the order eviction time
	 * 
	 * @param evictionTime the eviction time to set (may be null)
	 */
	public void setEvictionTime(Instant evictionTime) {
		this.evictionTime = evictionTime;
	}

	/**
	 * Gets the (earliest) start time of the processing time interval
	 * 
	 * @return the startTime
	 */
	public Instant getStartTime() {
		return startTime;
	}

	/**
	 * Sets the (earliest) start time of the processing time interval
	 * 
	 * @param startTime the startTime to set
	 */
	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	/**
	 * Gets the (latest) stop time of the processing time interval
	 * 
	 * @return the stopTime
	 */
	public Instant getStopTime() {
		return stopTime;
	}

	/**
	 * Sets the (latest) stop time of the processing time interval
	 * 
	 * @param stopTime the stopTime to set
	 */
	public void setStopTime(Instant stopTime) {
		this.stopTime = stopTime;
	}

	/**
	 * Gets the method for partitioning the orbit time interval
	 * 
	 * @return the slicingType the order slicing type
	 */
	public OrderSlicingType getSlicingType() {
		return slicingType;
	}

	/**
	 * Sets the method for partitioning the orbit time interval.
	 * 
	 * If the slicing type is set to TIME_SLICE and the slice duration
	 * has not yet been set, then the slice duration will be set to a default value of one day; for other slicing types
	 * the current slice duration will be deleted.
	 * 
	 * If the slicing type is set to NONE, the slice overlap will be set to zero.
	 * 
	 * @param slicingType the slicing type to set
	 */
	public void setSlicingType(OrderSlicingType slicingType) {
		this.slicingType = slicingType;
		if (OrderSlicingType.TIME_SLICE.equals(slicingType)) {
			if (null == sliceDuration) {
				sliceDuration = Duration.of(1, ChronoUnit.DAYS);
			}
		} else {
			sliceDuration = null;
		}
		if (OrderSlicingType.NONE.equals(slicingType)) {
			sliceOverlap = Duration.ZERO;
		}
	}

	/**
	 * Gets the duration for a single slice
	 * 
	 * @return the slice duration for slicing type TIME_SLICE, null otherwise
	 */
	public Duration getSliceDuration() {
		return sliceDuration;
	}

	/**
	 * Sets the duration for a single slice (for slicing type TIME_SLICE only)
	 * 
	 * @param sliceDuration the sliceDuration to set
	 * @throws IllegalStateException if setting a slice duration for orders with slicing type other than TIME_SLICE is attempted
	 */
	public void setSliceDuration(Duration sliceDuration) throws IllegalStateException {
		if (OrderSlicingType.TIME_SLICE.equals(slicingType)) {
			this.sliceDuration = sliceDuration;
		} else {
			throw new IllegalStateException(MSG_SLICING_DURATION_NOT_ALLOWED + slicingType);
		}
	}

	/**
	 * Gets the overlap time between slices
	 * 
	 * @return the slice overlap
	 */
	public Duration getSliceOverlap() {
		return sliceOverlap;
	}

	/**
	 * Sets the overlap time between slices
	 * 
	 * @param sliceOverlap the slice overlap to set
	 * @throws IllegalStateException if setting an overlap other than 0 for orders with slicing type NONE is attempted
	 */
	public void setSliceOverlap(Duration sliceOverlap) throws IllegalStateException {
		if (OrderSlicingType.NONE.equals(slicingType) && !Duration.ZERO.equals(sliceOverlap)) {
			throw new IllegalStateException(MSG_SLICING_OVERLAP_NOT_ALLOWED + slicingType);
		} else {
			this.sliceOverlap = sliceOverlap;
		}
	}

	/**
	 * @return the inputProductReference
	 */
	public InputProductReference getInputProductReference() {
		return inputProductReference;
	}

	/**
	 * @param inputProductReference the inputProductReference to set
	 */
	public void setInputProductReference(InputProductReference inputProductReference) {
		this.inputProductReference = inputProductReference;
	}

	/**
	 * Gets the input filters
	 * 
	 * @return the input filters
	 */
	public Map<ProductClass, InputFilter> getInputFilters() {
		return inputFilters;
	}

	/**
	 * Sets the input filters
	 * 
	 * @param inputFilters the input filters to set
	 */
	public void setInputFilters(Map<ProductClass, InputFilter> inputFilters) {
		this.inputFilters = inputFilters;
	}

	/**
	 * Gets the dynamic processing parameters to be set for the Job Orders in this order
	 * 
	 * @return the dynamicProcessingParameters
	 */
	public Map<String, Parameter> getDynamicProcessingParameters() {
		return dynamicProcessingParameters;
	}

	/**
	 * Sets the dynamic processing parameters to be set for the Job Orders in this order
	 * 
	 * @param dynamicProcessingParameters the dynamicProcessingParameters to set
	 */
	public void setDynamicProcessingParameters(Map<String, Parameter> dynamicProcessingParameters) {
		this.dynamicProcessingParameters = dynamicProcessingParameters;
	}

	/**
	 * Gets the class-specific output parameters
	 * 
	 * @return the class-specific output parameters
	 */
	public Map<ProductClass, ClassOutputParameter> getClassOutputParameters() {
		return classOutputParameters;
	}

	/**
	 * Sets the class-specific output parameters
	 * 
	 * @param classOutputParameters the class-specific output parameters to set
	 */
	public void setClassOutputParameters(Map<ProductClass, ClassOutputParameter> classOutputParameters) {
		this.classOutputParameters = classOutputParameters;
	}

	/**
	 * Gets the output parameters
	 * 
	 * @return the outputParameters
	 */
	public Map<String, Parameter> getOutputParameters() {
		return outputParameters;
	}

	/**
	 * Gets the output parameters for a specific product class (class-specific, if available, general otherwise)
	 * 
	 * @param productClass the product class to get the parameters for
	 * @return the output parameters
	 */
	public Map<String, Parameter> getOutputParameters(ProductClass productClass) {
		if (null == productClass || null == classOutputParameters.get(productClass)) {
			return getOutputParameters();
		} else {
			return classOutputParameters.get(productClass).getOutputParameters();
		}
	}

	/**
	 * Sets the output parameters
	 * 
	 * @param outputParameters the output parameters to set
	 */
	public void setOutputParameters(Map<String, Parameter> outputParameters) {
		this.outputParameters = outputParameters;
	}

	/**
	 * Gets the requested product classes
	 * 
	 * @return the requestedProductClasses
	 */
	public Set<ProductClass> getRequestedProductClasses() {
		return requestedProductClasses;
	}

	/**
	 * Sets the requested product classes
	 * 
	 * @param requestedProductClasses the requestedProductClasses to set
	 */
	public void setRequestedProductClasses(Set<ProductClass> requestedProductClasses) {
		this.requestedProductClasses = requestedProductClasses;
	}

	/**
	 * Gets the input product classes provided to the order
	 * 
	 * @return the input product classes
	 */
	public Set<ProductClass> getInputProductClasses() {
		return inputProductClasses;
	}

	/**
	 * Sets the input product classes provided to the order
	 * 
	 * @param inputProductClasses the input product classes to set
	 */
	public void setInputProductClasses(Set<ProductClass> inputProductClasses) {
		this.inputProductClasses = inputProductClasses;
	}

	/**
	 * Gets the file class of the output products
	 * 
	 * @return the outputFileClass
	 */
	public String getOutputFileClass() {
		return outputFileClass;
	}

	/**
	 * Sets the file class for the output products
	 * 
	 * @param outputFileClass the outputFileClass to set
	 */
	public void setOutputFileClass(String outputFileClass) {
		this.outputFileClass = outputFileClass;
	}

	/**
	 * Gets the processing mode for the processors
	 * 
	 * @return the processingMode
	 */
	public String getProcessingMode() {
		return processingMode;
	}

	/**
	 * Sets the processing mode for the processors
	 * 
	 * @param processingMode the processingMode to set
	 */
	public void setProcessingMode(String processingMode) {
		this.processingMode = processingMode;
	}

	/**
	 * Gets the production type context
	 * 
	 * @return the production type
	 */
	public ProductionType getProductionType() {
		return productionType;
	}

	/**
	 * Sets the production type context
	 * 
	 * @param productionType the production type to set
	 */
	public void setProductionType(ProductionType productionType) {
		this.productionType = productionType;
	}

	/**
	 * @return the notificationEndpoint
	 */
	public NotificationEndpoint getNotificationEndpoint() {
		return notificationEndpoint;
	}

	/**
	 * @param notificationEndpoint the notificationEndpoint to set
	 */
	public void setNotificationEndpoint(NotificationEndpoint notificationEndpoint) {
		this.notificationEndpoint = notificationEndpoint;
	}

	/**
	 * Gets the retention period for products generated by this processing order
	 * 
	 * @return the product retention period
	 */
	public Duration getProductRetentionPeriod() {
		return productRetentionPeriod;
	}

	/**
	 * Sets the retention period for products generated by this processing order
	 * 
	 * @param productRetentionPeriod the product retention period to set
	 */
	public void setProductRetentionPeriod(Duration productRetentionPeriod) {
		this.productRetentionPeriod = productRetentionPeriod;
	}

	/**
	 * Checks whether the order has failed job steps
	 * 
	 * @return true, if at least one job step is in FAILED state, false otherwise
	 */
	public Boolean getHasFailedJobSteps() {
		return hasFailedJobSteps;
	}

	/**
	 * Checks whether the order has failed job steps (convenience method for getHasFailedJobSteps())
	 * 
	 * @return true, if at least one job step is in FAILED state, false otherwise
	 */
	public Boolean hasFailedJobSteps() {
		return getHasFailedJobSteps();
	}

	/**
	 * Sets whether the order has failed job steps
	 * 
	 * @param hasFailedJobSteps set to true, when a job step for this order fails
	 */
	public void setHasFailedJobSteps(Boolean hasFailedJobSteps) {
		this.hasFailedJobSteps = hasFailedJobSteps;
	}

	/** Get the workflow
	 * @return the workflow
	 */
	public Workflow getWorkflow() {
		return workflow;
	}

	/** Set the workflow
	 * @param workflow the workflow to set
	 */
	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	/**
	 * Gets the requested configured processors
	 * 
	 * @return the requestedConfiguredProcessors
	 */
	public Set<ConfiguredProcessor> getRequestedConfiguredProcessors() {
		return requestedConfiguredProcessors;
	}

	/**
	 * Sets the requested configured processors
	 * 
	 * @param requestedConfiguredProcessors the requestedConfiguredProcessors to set
	 */
	public void setRequestedConfiguredProcessors(Set<ConfiguredProcessor> requestedConfiguredProcessors) {
		this.requestedConfiguredProcessors = requestedConfiguredProcessors;
	}

	/**
	 * Gets the requested orbits
	 * 
	 * @return the requestedOrbits
	 */
	public List<Orbit> getRequestedOrbits() {
		return requestedOrbits;
	}

	/**
	 * Sets the requested orbits
	 * 
	 * @param requestedOrbits the requestedOrbits to set
	 */
	public void setRequestedOrbits(List<Orbit> requestedOrbits) {
		this.requestedOrbits = requestedOrbits;
	}

	/**
	 * Gets the processing jobs
	 * 
	 * @return the jobs
	 */
	public Set<Job> getJobs() {
		return jobs;
	}

	/**
	 * Sets the processing jobs
	 * 
	 * @param jobs the jobs to set
	 */
	public void setJobs(Set<Job> jobs) {
		this.jobs = jobs;
	}

	/**
	 * Gets the order progress monitoring data
	 * 
	 * @return the order progress monitoring data
	 */
	public Set<MonOrderProgress> getMonOrderProgress() {
		return monOrderProgress;
	}

	/**
	 * Sets the order progress monitoring data
	 * 
	 * @param monOrderProgress the order progress monitoring data to set
	 */
	public void setMonOrderProgress(Set<MonOrderProgress> monOrderProgress) {
		this.monOrderProgress = monOrderProgress;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier); // same identifier in different missions unlikely
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof ProcessingOrder))
			return false;
		ProcessingOrder other = (ProcessingOrder) obj;
		return Objects.equals(identifier, other.getIdentifier()) && Objects.equals(mission, other.getMission());
	}

	@Override
	public String toString() {
		return "ProcessingOrder [mission=" + (null == mission ? "null" : mission.getCode()) + ", identifier=" + identifier 
				+ ", orderState=" + orderState + ", stateMessage=" + stateMessage + ", orderSource=" + orderSource
				+ ", submissionTime=" + submissionTime + ", executionTime=" + executionTime + ", releaseTime=" + releaseTime
				+ ", requestedProductClasses=" + requestedProductClasses
				+ ", startTime=" + startTime + ", stopTime=" + stopTime + ", requestedOrbits=" + requestedOrbits
				+ ", slicingType=" + slicingType + ", sliceDuration=" + sliceDuration + ", sliceOverlap=" + sliceOverlap
				+ ", inputFilters=" + inputFilters + ", outputParameters=" + outputParameters
				+ ", classOutputParameters=" + classOutputParameters 
				+ ", outputFileClass=" + outputFileClass + ", processingMode=" + processingMode
				+ ", productionType=" + productionType + ", notificationEndpoint=" + notificationEndpoint
				+ ", productRetentionPeriod=" + productRetentionPeriod
				+ ", hasFailedJobSteps=" + hasFailedJobSteps + "]";
	}

}
