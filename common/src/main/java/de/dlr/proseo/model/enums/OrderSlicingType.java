/**
 * OrderSlicingType.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Possible methods for partitioning the order time period into individual job time periods for product generation:
 * <ul>
 * <li>ORBIT: Create jobs by orbit (preferably a list of orbits is then given for the order, if no such lists exists, generate
 *            jobs orbit-wise so that the time interval is fully covered, i. e. with the first orbit starting no later
 *            than the beginning of the time interval and the last orbit ending no earlier than the end of the time interval;
 *            jobs will be linked to their respective orbits)</li>
 * <li>CALENDAR_DAY: Create jobs by calendar day (in such a way that the first job starts no later than the beginning of
 *            the order time interval and the last job ends no earlier than the end of the time interval)</li>
 * <li>TIME_SLICE: Create jobs in fixed time slices, starting with the start time of the order time interval and ending
 *            no earlier than the end of the time interval</li>
 * </ul>
 * 
 * @author Dr. Thomas Bassler
 *
 */
public enum OrderSlicingType {
	ORBIT, CALENDAR_DAY, CALENDAR_MONTH, CALENDAR_YEAR, TIME_SLICE
}
