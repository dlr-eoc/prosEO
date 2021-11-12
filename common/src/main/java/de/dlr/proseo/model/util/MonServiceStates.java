package de.dlr.proseo.model.util;


/**
 * @author melchinger
 *
 * MonServiceStates class to hold definition of monitoring service states/ids
 */
public class MonServiceStates {
	public static final String RUNNING = "running";
	public static final String STOPPED = "stopped";
	public static final String STARTING = "starting";
	public static final String STOPPING = "stopping";
	public static final String DEGRADED = "degraded";
	public static final Long RUNNING_ID = (long) 1;
	public static final Long STOPPED_ID = (long) 2;
	public static final Long STARTING_ID = (long) 3;
	public static final Long STOPPING_ID = (long) 4;
	public static final Long DEGRADED_ID = (long) 5;
}
