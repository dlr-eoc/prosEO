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

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * A collection of job steps required to fulfil an order for a specific period of time (e. g. one orbit).
 * Note: As a future extension a selection by geographical area in addition to or instead of a period of time is envisioned.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class Job extends PersistentObject {
	
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
	 * A set of additional conditions to apply to selected products.
	 * Note: For Sentnel-5P at least the parameters "copernicusCollection", "fileClass" and "revision" are required.
	 */
	@ElementCollection
	private Map<String, Parameter> filterConditions = new HashMap<>();
	
	/**
	 * A set of parameters to set for the generated products.
	 * Note: For Sentnel-5P at least the parameters "copernicusCollection", "fileClass" and "revision" are required.
	 */
	@ElementCollection
	private Map<String, Parameter> outputParameters = new HashMap<>();
	
	/**
	 * A processing priority (lower numbers indicate lower priority, higher numbers higher priority; the default value is 0).
	 */
	private Integer priority;
		
	/** The processing facility this job runs on */
	@ManyToOne
	private ProcessingFacility processingFacility;
	
	/** The job steps for this job */
	@OneToMany(mappedBy = "job")
	private Set<JobStep> jobSteps = new HashSet<>();
	
	/**
	 * Enumeration describing possible job states.
	 */
	public enum JobState { INITIAL, STARTED, ON_HOLD, COMPLETED, FAILED }

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
	 * @param ordeprocessingOrderr the order to set
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
	 * Sets the processing state of the job
	 * 
	 * @param jobState the jobState to set
	 */
	public void setJobState(JobState jobState) {
		this.jobState = jobState;
	}

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
	 * Gets the filtering conditions
	 * 
	 * @return the filterConditions
	 */
	public Map<String, Parameter> getFilterConditions() {
		return filterConditions;
	}

	/**
	 * Sets the filtering conditions
	 * 
	 * @param filterConditions the filterConditions to set
	 */
	public void setFilterConditions(Map<String, Parameter> filterConditions) {
		this.filterConditions = filterConditions;
	}

	/**
	 * Gets the parameters for output products
	 * 
	 * @return the outputParameters
	 */
	public Map<String, Parameter> getOutputParameters() {
		return outputParameters;
	}

	/**
	 * Sets the parameters for output products
	 * 
	 * @param outputParameters the outputParameters to set
	 */
	public void setOutputParameters(Map<String, Parameter> outputParameters) {
		this.outputParameters = outputParameters;
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
	};

}
