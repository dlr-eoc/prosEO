/**
 * ProductionPlanner.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner;

/**
 * Enumeration defining sorting criteria for ready job steps to run.
 *
 * @author Ernst Melchinger
 */
public enum JobStepSort {

	SENSING_TIME("sensingTime"), // Sort by sensing start time
	SUBMISSION_TIME("submissionTime"); // Sort by submission time of order

	/** The string representation of the sorting criterion. */
	public String sort;

	/**
	 * Constructs a new JobStepSort enumeration with the specified sorting criterion.
	 *
	 * @param string The string representation of the sorting criterion.
	 */
	JobStepSort(String string) {
		this.sort = string;
	}

}