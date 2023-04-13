/**
 * OrderSlicingType.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Possible methods for partitioning the order time period into individual job time periods for product generation.
 * 
 * @author Dr. Thomas Bassler
 *
 */
public enum OrderSlicingType {
	/**
	 * Create jobs by orbit (preferably a list of orbits is then given for the order, if no such lists exists, generate
     * jobs orbit-wise so that the time interval is fully covered, i. e. with the first orbit starting no later
     * than the beginning of the time interval and the last orbit ending no earlier than the end of the time interval;
     * jobs will be linked to their respective orbits)
	 */
	ORBIT,
	/**
	 * Create jobs by calendar day (in such a way that the first job starts no later than the beginning of
     * the order time interval and the last job ends no earlier than the end of the time interval)
	 */
	CALENDAR_DAY, 
	/** Same as CALENDAR_DAY, but for calendar months */
	CALENDAR_MONTH, 
	/** Same as CALENDAR_DAY, but for calendar years */
	CALENDAR_YEAR,
	/**
	 * Create jobs in fixed time slices, starting with the start time of the order time interval and ending
     * no earlier than the end of the time interval
	 */
	TIME_SLICE, 
	/**
	 * Do not attempt to create slices, but create a single job spanning exactly the time interval from startTime to stopTime
	 */
	NONE
}
