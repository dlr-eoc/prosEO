/**
 * Job.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.OrderState;

/**
 * A collection of job steps required to fulfil an order for a specific period of time (e. g. one orbit).
 * Note: As a future extension a selection by geographical area in addition to or instead of a period of time is envisioned.
 * 
 * @author Dr. Thomas Bassler
 *
 */

@Entity
@Table(indexes = { @Index(unique = false, columnList = "jobState") })
public class Job extends PersistentObject {
	
	private static final String MSG_ILLEGAL_STATE_TRANSITION = "Illegal job state transition from %s to %s";
	
	/** The processing order this job belongs to */
	@ManyToOne
	private ProcessingOrder processingOrder;
	
	/** 
	 * Status of the whole job; jobs in status INITIAL or ON_HOLD need to be released to reach status STARTED, jobs in status 
	 * STARTED can be set to ON_HOLD, meaning that all qualifying dependent job steps are returned to status INITIAL (i. e. except 
	 * those in status RUNNING, COMPLETED and FAILED).
	 */
	@Enumerated(EnumType.STRING)
	private JobState jobState;
	
	/** The orbit this job relates to (if any) */
	@ManyToOne
	private Orbit orbit;
	
	/** 
	 * The start time of the time interval, for which products shall be generated. 
	 * If the job is orbit-related, this time is copied from the start time of the orbit.
	 */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant startTime;
	
	/**
	 * The end time of the time interval, for which products shall be generated.
	 * If the job is orbit-related, this time is copied from the stop time of the orbit.
	 */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant stopTime;
	
	/**
	 * A processing priority (lower numbers indicate lower priority, higher numbers higher priority; the default value is 0).
	 */
	private Integer priority;
	
	/** Indicates whether at least one of the job steps for this job is in state FAILED */
	private Boolean hasFailedJobSteps = false;
	
	/** The processing facility this job runs on */
	@ManyToOne
	private ProcessingFacility processingFacility;
	
	/** The job steps for this job */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "job")
	private Set<JobStep> jobSteps = new HashSet<>();
	
	/**
	 * Enumeration describing possible job states.
	 */
	public enum JobState {
		INITIAL, PLANNED, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED, CLOSED;
		
		public boolean isLegalTransition(JobState other) {
			switch(this) {
			case COMPLETED:
				return other.equals(CLOSED);
			case INITIAL:
				return other.equals(PLANNED);
			case FAILED:
				return other.equals(PLANNED) || other.equals(CLOSED);
			case PLANNED:
				return other.equals(RELEASED) || other.equals(FAILED);
			case ON_HOLD:
				return other.equals(PLANNED) || other.equals(COMPLETED) || other.equals(FAILED);
			case RELEASED:
				return other.equals(PLANNED) || other.equals(STARTED);
			case STARTED:
				return other.equals(ON_HOLD) || other.equals(COMPLETED) || other.equals(FAILED);
			case CLOSED:
				return false; // End state
			default:
				return false;
			}
		}
	}

	/**
	 * Gets the order this job belongs to
	 * 
	 * @return the order
	 */
	public ProcessingOrder getProcessingOrder() {
		return processingOrder;
	}

	/**
	 * Sets the order this job belongs to
	 * 
	 * @param processingOrder the order to set
	 */
	public void setProcessingOrder(ProcessingOrder processingOrder) {
		this.processingOrder = processingOrder;
	}

	/**
	 * Gets the processing state of the job
	 * 
	 * @return the jobState
	 */
	public JobState getJobState() {
		return jobState;
	}

	/**
	 * Sets the processing state of the job and propagates it to the processing order
	 * 
	 * @param jobState the jobState to set
	 * @throws IllegalStateException if the intended job state transition is illegal
	 */
	public void setJobState(JobState jobState) throws IllegalStateException {
		if (this.jobState.equals(jobState)) {
			// Do nothing
		} else if (null == this.jobState || this.jobState.isLegalTransition(jobState)) {
			this.jobState = jobState;
			processingOrder.checkStateChange();
		} else {
			throw new IllegalStateException(String.format(MSG_ILLEGAL_STATE_TRANSITION,
					this.jobState.toString(), jobState.toString()));
		}
	}

	/**
	 * Check whether a job state change is required based on the current state of the contained job steps,
	 * and if so, propagate the state change to the processing order
	 */
	public void checkStateChange() {
		hasFailedJobSteps = false;
		
		// Check whether the job has any job steps at all
		OrderState orderState = processingOrder.getOrderState();
		if (0 == jobSteps.size()) {
			if (OrderState.INITIAL.equals(orderState)
					|| OrderState.APPROVED.equals(orderState)
					|| OrderState.PLANNING.equals(orderState)
					|| OrderState.PLANNING_FAILED.equals(orderState)) {
				// Order planning is not yet completed (order states INITIAL and APPROVED should not happen here, but we account
				// for possible data inconsistencies)
				jobState = JobState.INITIAL;
			} else if (OrderState.CLOSED.equals(orderState)) {
				// Order is closed
				jobState = JobState.CLOSED;
			} else {
				// Order planning is completed, the order is still active, but no job steps were generated
				jobState = JobState.COMPLETED;
			}
			return;
		}
		
		// Prepare counter for job step states
		Map<JobStepState, Integer> jobStepStateMap = new HashMap<>();
		for (JobStepState jobStepState: JobStepState.values()) {
			jobStepStateMap.put(jobStepState, 0);
		}
		
		// Check status of job steps
		for (JobStep jobStep: jobSteps) {
			// Update job step failure flag
			hasFailedJobSteps = hasFailedJobSteps || jobStep.isFailed();
			
			// Count job steps per state
			jobStepStateMap.put(jobStep.getJobStepState(), jobStepStateMap.get(jobStep.getJobStepState()) + 1);
		}
		
		// Update job state according to distribution of job step states
		int jobStepCount = jobSteps.size();
		int terminatedJobStepCount = (jobStepStateMap.get(JobStepState.COMPLETED) 
				+ jobStepStateMap.get(JobStepState.FAILED)
				+ jobStepStateMap.get(JobStepState.CLOSED));
		
		// First check, whether all job steps are finished
		if (jobStepCount == jobStepStateMap.get(JobStepState.CLOSED)) {
			// All job steps are CLOSED
			jobState = JobState.CLOSED;
		} else if (jobStepCount == terminatedJobStepCount) {
			// All job steps are terminated in some way
			if (0 < jobStepStateMap.get(JobStepState.FAILED)) {
				jobState = JobState.FAILED;
			} else {
				jobState = JobState.COMPLETED;
			}
		// We still have unfinished job steps
		} else if (0 == jobStepStateMap.get(JobStepState.RUNNING) && 0 < terminatedJobStepCount) {
			// At least one job step has terminated, but none is running
			if (JobState.ON_HOLD.equals(jobState)) {
				jobState = JobState.PLANNED;
			} else {
				jobState = JobState.STARTED;
			}
		} else if (0 < jobStepStateMap.get(JobStepState.RUNNING)) {
			// At least one job step is running
			if (JobState.ON_HOLD.equals(jobState)) {
				// Do nothing, suspended job is waiting for the running jobs steps to terminate
			} else {
				jobState = JobState.STARTED; // This may already be the case
			}
		// No running and no terminated job steps
		} else if (0 < jobStepStateMap.get(JobStepState.READY) || 0 < jobStepStateMap.get(JobStepState.WAITING_INPUT)) {
			// The job was released, but job steps are waiting to be run
			jobState = JobState.RELEASED;
		// All existing job steps should be in state PLANNED
		} else {
			if (jobStepCount != jobStepStateMap.get(JobStepState.PLANNED)) {
				// Hmm, we missed a case in the selections above
				throw new RuntimeException("Unexpected job step state combination " + jobStepStateMap.toString());
			}
			// Now the state depends on whether planning was already completed
			if (OrderState.INITIAL.equals(orderState)
					|| OrderState.APPROVED.equals(orderState)
					|| OrderState.PLANNING.equals(orderState)
					|| OrderState.PLANNING_FAILED.equals(orderState)) {
				// Order planning is not yet completed (order states INITIAL and APPROVED should not happen here, but we account
				// for possible data inconsistencies)
				jobState = JobState.INITIAL;
			} else {
				jobState = JobState.PLANNED;
			}
		}
		
		
		processingOrder.checkStateChange();
	};

	/**
	 * Gets the related orbit (if any)
	 * 
	 * @return the orbit (null if there is no direct relationship with an orbit)
	 */
	public Orbit getOrbit() {
		return orbit;
	}

	/**
	 * Sets the related orbit
	 * 
	 * @param orbit the orbit to set
	 */
	public void setOrbit(Orbit orbit) {
		this.orbit = orbit;
	}

	/**
	 * Gets the start time of the processing time interval
	 * 
	 * @return the startTime
	 */
	public Instant getStartTime() {
		return startTime;
	}

	/**
	 * Sets the start time of the processing time interval
	 * 
	 * @param startTime the startTime to set
	 */
	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	/**
	 * Gets the end time of the processing time interval
	 * 
	 * @return the stopTime
	 */
	public Instant getStopTime() {
		return stopTime;
	}

	/**
	 * Sets the end time of the processing time interval
	 * 
	 * @param stopTime the stopTime to set
	 */
	public void setStopTime(Instant stopTime) {
		this.stopTime = stopTime;
	}

	/**
	 * Gets the processing priority
	 * 
	 * @return the priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * Sets the processing priority
	 * 
	 * @param priority the priority to set
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * Checks whether the job has failed steps
	 * 
	 * @return true, if at least one job step is in FAILED state, false otherwise
	 */
	public Boolean getHasFailedJobSteps() {
		return hasFailedJobSteps;
	}

	/**
	 * Checks whether the job has failed steps (convenience method for getHasFailedJobSteps())
	 * 
	 * @return true, if at least one job step is in FAILED state, false otherwise
	 */
	public Boolean hasFailedJobSteps() {
		return hasFailedJobSteps;
	}

	/**
	 * Sets whether the job has failed steps
	 * 
	 * @param hasFailedJobSteps set to true, when a step of this job fails
	 */
	public void setHasFailedJobSteps(Boolean hasFailedJobSteps) {
		this.hasFailedJobSteps = hasFailedJobSteps;
	}

	/**
	 * Gets the processing facility for this job
	 * 
	 * @return the processingFacility
	 */
	public ProcessingFacility getProcessingFacility() {
		return processingFacility;
	}

	/**
	 * Sets the processing facility for this job
	 * 
	 * @param processingFacility the processingFacility to set
	 */
	public void setProcessingFacility(ProcessingFacility processingFacility) {
		this.processingFacility = processingFacility;
	}

	/**
	 * Gets the job steps
	 * 
	 * @return the jobSteps
	 */
	public Set<JobStep> getJobSteps() {
		return jobSteps;
	}

	/**
	 * Sets the job steps
	 * 
	 * @param jobSteps the jobSteps to set
	 */
	public void setJobSteps(Set<JobStep> jobSteps) {
		this.jobSteps = jobSteps;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(processingOrder, startTime, stopTime);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Job))
			return false;
		Job other = (Job) obj;
		return Objects.equals(processingOrder, other.processingOrder) && Objects.equals(startTime, other.startTime)
				&& Objects.equals(stopTime, other.stopTime);
	}

	@Override
	public String toString() {
		return "Job [jobState=" + jobState + ", orbit=" + orbit + ", startTime="
				+ startTime + ", stopTime=" + stopTime + ", priority=" + priority + ", hasFailedJobSteps=" + hasFailedJobSteps
				+ ", processingFacility=" + processingFacility + ", jobSteps=" + jobSteps + "]";
	}

}
