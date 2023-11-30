/**
 * MonOrderStates.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

/**
 * MonOrderStates: A class to hold the state definitions for monitoring orders,
 * and the respective state IDs.
 * 
 * @author Ernst Melchinger
 */
public class MonOrderStates {
	/** Text string for order state INITIAL */
	public static final String INITIAL = "INITIAL";
	/** Text string for order state APPROVED */
	public static final String APPROVED = "APPROVED";
	/** Text string for order state PLANNED */
	public static final String PLANNED = "PLANNED";
	/** Text string for order state RELEASED */
	public static final String RELEASED = "RELEASED";
	/** Text string for order state RUNNING */
	public static final String RUNNING = "RUNNING";
	/** Text string for order state SUSPENDING */
	public static final String SUSPENDING = "SUSPENDING";
	/** Text string for order state COMPLETED */
	public static final String COMPLETED = "COMPLETED";
	/** Text string for order state FAILED */
	public static final String FAILED = "FAILED";
	/** Text string for order state CLOSED */
	public static final String CLOSED = "CLOSED";

	/** Numeric code for order state INITIAL */
	public static final Long INITIAL_ID = (long) 0;
	/** Numeric code for order state APPROVED */
	public static final Long APPROVED_ID = (long) 1;
	/** Numeric code for order state PLANNED */
	public static final Long PLANNED_ID = (long) 2;
	/** Numeric code for order state RELEASED */
	public static final Long RELEASED_ID = (long) 3;
	/** Numeric code for order state RUNNING */
	public static final Long RUNNING_ID = (long) 4;
	/** Numeric code for order state SUSPENDING */
	public static final Long SUSPENDING_ID = (long) 5;
	/** Numeric code for order state COMPLETED */
	public static final Long COMPLETED_ID = (long) 6;
	/** Numeric code for order state FAILED */
	public static final Long FAILED_ID = (long) 7;
	/** Numeric code for order state CLOSED */
	public static final Long CLOSED_ID = (long) 8;
}