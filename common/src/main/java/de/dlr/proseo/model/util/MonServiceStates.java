/**
 * MonServiceStates.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

/**
 * MonServiceStates: A class to hold the state definitions for monitoring
 * services, and the respective state IDs.
 * 
 * @author Ernst Melchinger
 */
public class MonServiceStates {
	/** Text string for service state RUNNING */
	public static final String RUNNING = "running";
	/** Text string for service state STOPPED */
	public static final String STOPPED = "stopped";
	/** Text string for service state STARTING */
	public static final String STARTING = "starting";
	/** Text string for service state STOPPING */
	public static final String STOPPING = "stopping";
	/** Text string for service state DEGRADED */
	public static final String DEGRADED = "degraded";

	/** Numeric code for service state RUNNING */
	public static final Long RUNNING_ID = (long) 1;
	/** Numeric code for service state STOPPED */
	public static final Long STOPPED_ID = (long) 2;
	/** Numeric code for service state STARTING */
	public static final Long STARTING_ID = (long) 3;
	/** Numeric code for service state STOPPING */
	public static final Long STOPPING_ID = (long) 4;
	/** Numeric code for service state DEGRADED */
	public static final Long DEGRADED_ID = (long) 5;
}