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
	public static final String INITIAL = "INITIAL";
	public static final String APPROVED = "APPROVED";
	public static final String PLANNED = "PLANNED";
	public static final String RELEASED = "RELEASED";
	public static final String RUNNING = "RUNNING";
	public static final String SUSPENDING = "SUSPENDING";
	public static final String COMPLETED = "COMPLETED";
	public static final String FAILED = "FAILED";
	public static final String CLOSED = "CLOSED";

	public static final Long INITIAL_ID = (long) 0;
	public static final Long APPROVED_ID = (long) 1;
	public static final Long PLANNED_ID = (long) 2;
	public static final Long RELEASED_ID = (long) 3;
	public static final Long RUNNING_ID = (long) 4;
	public static final Long SUSPENDING_ID = (long) 5;
	public static final Long COMPLETED_ID = (long) 6;
	public static final Long FAILED_ID = (long) 7;
	public static final Long CLOSED_ID = (long) 8;
}