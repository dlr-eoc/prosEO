/**
 * OrbitOrderTrigger.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Trigger for the generation of processing orders per spacecraft orbit
 */
@Entity
@Table(indexes = { 
		@Index(unique = true, columnList = "mission_id,name")})
public class OrbitOrderTrigger extends MissionPlanningOrderTrigger {
	
	/**
	 * The spacecraft whose orbit start times determine the trigger times
	 */
	@ManyToOne
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