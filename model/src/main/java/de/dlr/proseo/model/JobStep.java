/**
 * JobStep.java
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
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.springframework.transaction.annotation.Transactional;

/**
 * A single processor execution to produce a defined output product based on a defined set of required input product
 * (modelled as ProductQuery objects). A JobStep can be executed as soon as all its ProductQuerys are satisfied.
 * 
 * @author Dr. Thomas Bassler
 *
 */

@Entity
@Table(indexes = { @Index(unique = false, columnList = "jobStepState") })
public class JobStep extends PersistentObject {

	private static final String MSG_ILLEGAL_STATE_TRANSITION = "Illegal job step state transition from %s to %s";

	/** The job this job step belongs to */
	@ManyToOne
	private Job job;
	
	/**
	 * The currenet status of the job step; job steps in status PLANNED need to be released to advance to WAITING_INPUT status,
	 * job steps in status WAITING_INPUT and READY can be returned to PLANNED status. All other status transitions are automatic
	 * depending on processing progress.
	 */
	@Enumerated(EnumType.STRING)
	private JobStepState jobStepState;
	
	/** Additional parameter to set in the output products */
	@ElementCollection
	private Map<String, Parameter> outputParameters = new HashMap<>();
	
	/** The processing mode to run the processor(s) in (one of the modes specified for the mission) */
	private String processingMode;
	
	/** Query objects for input products */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "jobStep")
	private Set<ProductQuery> inputProductQueries = new HashSet<>();
	
	/** The output product of this job step */
	@OneToOne(mappedBy = "jobStep")
	private Product outputProduct;
	
	/** The start time of the processing job */
	private Instant processingStartTime;
	
	/** The completion time of the processing job */
	private Instant processingCompletionTime;
	
	/** The standard output of the processing job */
	@org.hibernate.annotations.Type(type = "materialized_clob")
	private String processingStdOut;
	
	/** The standard error output of the processing job */
	@org.hibernate.annotations.Type(type = "materialized_clob")
	private String processingStdErr;
	
	/** The filename of the Job Order file in the associated processing facility */
	private String jobOrderFilename;

	/**
	 * The log level of stdout
	 */
	private StdLogLevel stdoutLogLevel = StdLogLevel.INFO;

	/**
	 * The log level of stderr
	 */
	private StdLogLevel stderrLogLevel = StdLogLevel.INFO;

	/**
	 * The failed state of job step
	 */
	private Boolean isFailed = false;

	/**
	 * The possible processing states for a job step
	 */
	public enum JobStepState {
		PLANNED, WAITING_INPUT, READY, RUNNING, COMPLETED, FAILED, CLOSED;
		
		public boolean isLegalTransition(JobStepState other) {
			switch(this) {
			case PLANNED:
				return other.equals(WAITING_INPUT) || other.equals(READY) || other.equals(FAILED);
			case COMPLETED:
				return other.equals(CLOSED);
			case FAILED:
				return other.equals(PLANNED) || other.equals(CLOSED);
			case READY:
				return other.equals(PLANNED) || other.equals(RUNNING);
			case RUNNING:
				return other.equals(PLANNED) || other.equals(COMPLETED) || other.equals(FAILED);
			case WAITING_INPUT:
				return other.equals(PLANNED) || other.equals(READY);
			case CLOSED:
				return false; // End state
			default:
				return false;
			}
		}
	}

	/**
	 * The possible log levels for stdout and stderr
	 */
	public enum StdLogLevel { DEBUG, INFO, PROGRESS, WARNING, ERROR }

	/**
	 * @return the stdoutLogLevel
	 */
	public StdLogLevel getStdoutLogLevel() {
		return stdoutLogLevel;
	}

	/**
	 * @return the stderrLogLevel
	 */
	public StdLogLevel getStderrLogLevel() {
		return stderrLogLevel;
	}

	/**
	 * @param stdoutLogLevel the stdoutLogLevel to set
	 */
	public void setStdoutLogLevel(StdLogLevel stdoutLogLevel) {
		this.stdoutLogLevel = stdoutLogLevel;
	}

	/**
	 * @param stderrLogLevel the stderrLogLevel to set
	 */
	public void setStderrLogLevel(StdLogLevel stderrLogLevel) {
		this.stderrLogLevel = stderrLogLevel;
	}

	/**
	 * @return the isFailed
	 */
	public Boolean getIsFailed() {
		return isFailed;
	}

	/**
	 * @return the isFailed
	 */
	public Boolean isFailed() {
		return getIsFailed();
	}

	/**
	 * @param isFailed the isFailed to set
	 */
	public void setIsFailed(Boolean isFailed) {
		this.isFailed = isFailed;
	}

	/**
	 * Gets the enclosing job
	 * 
	 * @return the job
	 */
	public Job getJob() {
		return job;
	}

	/**
	 * Sets the enclosing job
	 * 
	 * @param job the job to set
	 */
	public void setJob(Job job) {
		this.job = job;
	}

	/**
	 * Gets the state of the job step
	 * 
	 * @return the jobStepState
	 */
	public JobStepState getJobStepState() {
		return jobStepState;
	}

	/**
	 * Sets the state of the job step (and optionally its failure flag) and propagates this state to the job
	 * 
	 * @param jobStepState the jobStepState to set
	 * @throws IllegalStateException if the intended job step state transition is illegal
	 */
	@Transactional
	public void setJobStepState(JobStepState jobStepState) throws IllegalStateException {
		if (null == this.jobStepState) {
			this.jobStepState = jobStepState;
		} else if (this.jobStepState.equals(jobStepState)) {
			// Do nothing
		} else if (this.jobStepState.isLegalTransition(jobStepState)) {
			// Update job step state
			this.jobStepState = jobStepState;
			
			// Set failure flag
			if (JobStepState.FAILED.equals(jobStepState)) {
				this.isFailed = true;
			} else if (!JobStepState.CLOSED.equals(jobStepState)) {
				this.isFailed = false;
			}
			
			// Propagate state change
			if (job != null) {
				job.checkStateChange();
			}
		} else {
			throw new IllegalStateException(String.format(MSG_ILLEGAL_STATE_TRANSITION,
					this.jobStepState.toString(), jobStepState.toString()));
		}
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
	 * Sets the output parameters
	 * 
	 * @param outputParameters the outputParameters to set
	 */
	public void setOutputParameters(Map<String, Parameter> outputParameters) {
		this.outputParameters = outputParameters;
	}

	/**
	 * Gets the processing mode to run the processor in
	 * 
	 * @return the processingMode
	 */
	public String getProcessingMode() {
		return processingMode;
	}

	/**
	 * Sets the processing mode to run the processor in
	 * 
	 * @param processingMode the processingMode to set
	 */
	public void setProcessingMode(String processingMode) {
		this.processingMode = processingMode;
	}

	/**
	 * Gets the product queries for the input products
	 * 
	 * @return the inputProductQueries
	 */
	public Set<ProductQuery> getInputProductQueries() {
		return inputProductQueries;
	}

	/**
	 * Sets the product queries for the input products
	 * 
	 * @param inputProductQueries the inputProductQueries to set
	 */
	public void setInputProductQueries(Set<ProductQuery> inputProductQueries) {
		this.inputProductQueries = inputProductQueries;
	}

	/**
	 * Gets the output product
	 * 
	 * @return the outputProduct
	 */
	public Product getOutputProduct() {
		return outputProduct;
	}

	/**
	 * Sets the output product
	 * 
	 * @param outputProduct the outputProduct to set
	 */
	public void setOutputProduct(Product outputProduct) {
		this.outputProduct = outputProduct;
	}

	/**
	 * Gets the start time of the processing job
	 * 
	 * @return the processing start time
	 */
	public Instant getProcessingStartTime() {
		return processingStartTime;
	}

	/**
	 * Sets the start time of the processing job
	 * 
	 * @param processingStartTime the processing start time to set
	 */
	public void setProcessingStartTime(Instant processingStartTime) {
		this.processingStartTime = processingStartTime;
	}

	/**
	 * Gets the completion time of the processing job
	 * 
	 * @return the processing completion time
	 */
	public Instant getProcessingCompletionTime() {
		return processingCompletionTime;
	}

	/**
	 * Sets the completion time of the processing job
	 * 
	 * @param processingCompletionTime the processing completion time to set
	 */
	public void setProcessingCompletionTime(Instant processingCompletionTime) {
		this.processingCompletionTime = processingCompletionTime;
	}

	/**
	 * Gets the standard output of the processing job
	 * 
	 * @return the processing standard output
	 */
	public String getProcessingStdOut() {
		return processingStdOut;
	}

	/**
	 * Sets the standard output of the processing job
	 * 
	 * @param processingStdOut the processing standard output to set
	 */
	public void setProcessingStdOut(String processingStdOut) {
		this.processingStdOut = processingStdOut;
	}

	/**
	 * Gets the standard error output of the processing job
	 * 
	 * @return the processing standard error output
	 */
	public String getProcessingStdErr() {
		return processingStdErr;
	}

	/**
	 * Sets the standard error output of the processing job
	 * 
	 * @param processingStdErr the processing standard error output to set
	 */
	public void setProcessingStdErr(String processingStdErr) {
		this.processingStdErr = processingStdErr;
	}

	/**
	 * Gets the Job Order filename
	 * 
	 * @return the Job Order filename
	 */
	public String getJobOrderFilename() {
		return jobOrderFilename;
	}

	/**
	 * Sets the Job Order filename
	 * 
	 * @param jobOrderFilename the Job Order filename to set
	 */
	public void setJobOrderFilename(String jobOrderFilename) {
		this.jobOrderFilename = jobOrderFilename;
	}

	@Override
	public int hashCode() {
		return Objects.hash(job, outputProduct);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (super.equals(obj))
			return true;
		if (!(obj instanceof JobStep))
			return false;
		JobStep other = (JobStep) obj;
		return Objects.equals(job, other.job) && Objects.equals(outputProduct, other.outputProduct);
	}

	@Override
	public String toString() {
		return "JobStep [jobStepState=" + jobStepState + ", outputParameters=" + outputParameters
				+ ", processingMode=" + processingMode + ", processingStartTime=" + processingStartTime + ", processingCompletionTime="
				+ processingCompletionTime + ", processingStdOut=" + processingStdOut + ", processingStdErr=" + processingStdErr
				 + ", isFailed=" + isFailed
				+ "]";
	}

}
