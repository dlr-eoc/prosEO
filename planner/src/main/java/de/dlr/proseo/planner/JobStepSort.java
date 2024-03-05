package de.dlr.proseo.planner;
/**
 * Enumeration defining sort for ready job steps to run
 * 
 * @author Ernst Melchinger
 */
public enum JobStepSort {
	// Sort by sensing start time
	SENSING_TIME("sensingTime"),
	// Sort by submission time of order
	SUBMISSION_TIME("submissionTime");

	public String sort;

	JobStepSort(String string) {
		this.sort = string;
	}
}
