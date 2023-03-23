/**
 * FacilityState.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * For the user as well as for planning and product access purposes it is important to know, whether a processing facility is
 * operational or not.
 * <br>
 * Allowed state transitions are DISABLED &lt;--&gt; STOPPED --&gt; STARTING --&gt; RUNNING --&gt; STOPPING --&gt; STOPPED.
 * 
 * @author Dr. Thomas Bassler
 * 
 */
public enum FacilityState {
	/** 
	 * The processing facility must not be used. All requests concerning a facility in this state shall return the
	 * HTTP code 400 (Bad Request) 
	 */
	DISABLED,
	/**
	 * The processing facility is temporarily halted. The facility is available for order planning, but no job steps
     * shall be started, and requests for product ingestion or download shall return the HTTP code 503
     * (Temporarily Unavailable) immediately.
     */
	STOPPED,
	/**
	 * The processing facility is in the run-up process. The fa- cility is available for order planning, job step
     * starting may be initiated, but may time out, all other requests shall return the HTTP code 503
     * (Temporarily Unavailable) either immediately or after a reasonable time out period.
	 */
	STARTING,
	/**
	 * The processing facility is fully available. All requests are allowed.
	 */
	RUNNING,
	/**
	 * The processing facility is in the process of being stopped. No new job steps shall be started, but running
     * job steps are allowed to finish and can be queried. All other requests shall return the HTTP code 503
     * (Temporarily Unavailable) immediately.
	 */
	STOPPING;
	
	/**
	 * Check whether the transition to the other state is legal
	 * 
	 * @param other the state to switch to
	 * @return true, if the transition is legal, false otherwise
	 */
	public boolean isLegalTransition(FacilityState other) {
		switch (this) {
		case DISABLED:
			return other.equals(STOPPED);
		case STOPPED:
			return other.equals(STARTING) || other.equals(DISABLED);
		case STARTING:
			return other.equals(RUNNING);
		case RUNNING:
			return other.equals(STOPPING);
		case STOPPING:
			return other.equals(STOPPED);
		}
		return false;
	}
}
