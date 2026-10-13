/**
 * GroundStation.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A ground station for satellite data uplink or downlink
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = {
		@Index(unique = true, columnList = "station_id") 
})
public class GroundStation extends PersistentObject {

	/** The globally unique ID of the ground station */
	@Column(nullable = false)
	private String stationId;
	
	/** The name of the ground station */
	private String stationName;

	/**
	 * Gets the ground station ID
	 * 
	 * @return the station ID
	 */
	public String getStationId() {
		return stationId;
	}

	/**
	 * Sets the ground station ID
	 * 
	 * @param stationId the stationId to set
	 */
	public void setStationId(String stationId) {
		this.stationId = stationId;
	}

	/**
	 * Gets the ground station name
	 * 
	 * @return the station name
	 */
	public String getStationName() {
		return stationName;
	}

	/**
	 * Sets the ground station name
	 * 
	 * @param stationName the station name to set
	 */
	public void setStationName(String stationName) {
		this.stationName = stationName;
	}
	
}
