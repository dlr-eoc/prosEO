/**
 * OrbitOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

/**
 * Trigger for the generation of processing orders per spacecraft orbit
 */
public class OrbitOrderTrigger extends MissionPlanningOrderTrigger {
	
	/**
	 * The spacecraft whose orbit start times determine the trigger times
	 */
	private Spacecraft spacecraft;

	/**
	 * Gets the associated spacecraft
	 * 
	 * @return the associated spacecraft
	 */
	public Spacecraft getSpacecraft() {
		return spacecraft;
	}

	/**
	 * Sets the associated spacecraft
	 * 
	 * @param spacecraft the associated spacecraft to set
	 */
	public void setSpacecraft(Spacecraft spacecraft) {
		this.spacecraft = spacecraft;
	}
	
}