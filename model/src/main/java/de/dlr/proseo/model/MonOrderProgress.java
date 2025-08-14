package de.dlr.proseo.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Embeddable
@Table(indexes = {
	@Index(unique = false, columnList = "datetime")
})
public class MonOrderProgress {
	
	/**
	 * The current order state
	 */
	@ManyToOne
	private MonOrderState monOrderState;
	
	/**
	 * count of job steps 
	 */
	private int allJobSteps;
	
	/**
	 * count of failed job steps 
	 */
	private int failedJobSteps;
	
	/**
	 * count of finished job steps (failed + completed)
	 */
	private int finishedJobSteps;
	
	/**
	 * count of completed job steps 
	 */
	private int completedJobSteps;
	
	/**
	 * count of running job steps 
	 */
	private int runningJobSteps;
	
	/**
	 * The time of service state
	 */
	@Column(name = "datetime", columnDefinition = "TIMESTAMP")
	private Instant datetime;

	/**
	 * @return the monOrderState
	 */
	public MonOrderState getMonOrderState() {
		return monOrderState;
	}

	/**
	 * @return the allJobSteps
	 */
	public int getAllJobSteps() {
		return allJobSteps;
	}

	/**
	 * @return the failedJobSteps
	 */
	public int getFailedJobSteps() {
		return failedJobSteps;
	}

	/**
	 * @return the finishedJobSteps
	 */
	public int getFinishedJobSteps() {
		return finishedJobSteps;
	}

	/**
	 * @return the completedJobSteps
	 */
	public int getCompletedJobSteps() {
		return completedJobSteps;
	}

	/**
	 * @return the runningJobSteps
	 */
	public int getRunningJobSteps() {
		return runningJobSteps;
	}

	/**
	 * @return the datetime
	 */
	public Instant getDatetime() {
		return datetime;
	}

	/**
	 * @param monOrderState the monOrderState to set
	 */
	public void setMonOrderState(MonOrderState monOrderState) {
		this.monOrderState = monOrderState;
	}

	/**
	 * @param allJobSteps the allJobSteps to set
	 */
	public void setAllJobSteps(int allJobSteps) {
		this.allJobSteps = allJobSteps;
	}

	/**
	 * @param failedJobSteps the failedJobSteps to set
	 */
	public void setFailedJobSteps(int failedJobSteps) {
		this.failedJobSteps = failedJobSteps;
	}

	/**
	 * @param finishedJobSteps the finishedJobSteps to set
	 */
	public void setFinishedJobSteps(int finishedJobSteps) {
		this.finishedJobSteps = finishedJobSteps;
	}

	/**
	 * @param completedJobSteps the completedJobSteps to set
	 */
	public void setCompletedJobSteps(int completedJobSteps) {
		this.completedJobSteps = completedJobSteps;
	}

	/**
	 * @param runningJobSteps the runningJobSteps to set
	 */
	public void setRunningJobSteps(int runningJobSteps) {
		this.runningJobSteps = runningJobSteps;
	}

	/**
	 * @param datetime the datetime to set
	 */
	public void setDatetime(Instant datetime) {
		this.datetime = datetime;
	}
	
}
