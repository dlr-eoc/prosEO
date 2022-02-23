/**
 * Orbit.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * The description of an orbit identified by its start and stop times (e. g. the "spacecraft midnight" events of the
 * Sentinel-5P satellite). There must be no time gap between subsequent orbits of the same spacecraft.
 * <br>
 * Orbit times (and all derived times in prosEO) are given in UTC-STS (leap seconds spread evenly over the last 1000
 * seconds of the day) and to a microsecond precision. The public static variable orbitTimeFormatter gives a standard
 * format for parsing and formatting orbit times. 
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = { @Index(unique = true, columnList = "spacecraft_id, orbit_number"), @Index(unique = false, columnList = "start_time") })
public class Orbit extends PersistentObject {
	
	/** The spacecraft this orbit belongs to */
	@ManyToOne
	private Spacecraft spacecraft;
	
	/** The orbit number (usually starting at 1 at launch); unique for a spacecraft and usually gapless */
	@Column(name = "orbit_number")
	private Integer orbitNumber;
	
	/** The start time of the orbit (e. g. using a Spacecraft Midnight Crossing [SMX] event) */
	@Column(name = "start_time", columnDefinition = "TIMESTAMP(6)")
	private Instant startTime;
	
	/** The stop time of the orbit (e. g. using a Spacecraft Midnight Crossing [SMX] event) */
	@Column(name = "stop_time", columnDefinition = "TIMESTAMP(6)")
	private Instant stopTime;
	
	/**
	 * Gets the related spacecraft
	 * 
	 * @return the spacecraft
	 */
	public Spacecraft getSpacecraft() {
		return spacecraft;
	}
	
	/**
	 * Sets the related spacecraft
	 * 
	 * @param spacecraft the spacecraft to set
	 */
	public void setSpacecraft(Spacecraft spacecraft) {
		this.spacecraft = spacecraft;
	}
	
	/**
	 * Gets the orbit number
	 * 
	 * @return the orbitNumber
	 */
	public Integer getOrbitNumber() {
		return orbitNumber;
	}

	/**
	 * Sets the orbit number
	 * 
	 * @param orbitNumber the orbitNumber to set
	 */
	public void setOrbitNumber(Integer orbitNumber) {
		this.orbitNumber = orbitNumber;
	}

	/**
	 * Gets the orbit start time
	 * 
	 * @return the startTime
	 */
	public Instant getStartTime() {
		return startTime;
	}
	
	/**
	 * Sets the orbit start time
	 * 
	 * @param startTime the startTime to set
	 */
	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * Gets the orbit stop time
	 * 
	 * @return the stopTime
	 */
	public Instant getStopTime() {
		return stopTime;
	}
	
	/**
	 * Sets the orbit stop time
	 * 
	 * @param stopTime the stopTime to set
	 */
	public void setStopTime(Instant stopTime) {
		this.stopTime = stopTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((spacecraft == null) ? 0 : spacecraft.hashCode());
		result = prime * result + ((startTime == null) ? 0 : startTime.hashCode());
		result = prime * result + ((stopTime == null) ? 0 : stopTime.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Orbit))
			return false;
		Orbit other = (Orbit) obj;
		if (spacecraft == null) {
			if (other.spacecraft != null)
				return false;
		} else if (!spacecraft.equals(other.spacecraft))
			return false;
		if (startTime == null) {
			if (other.startTime != null)
				return false;
		} else if (!startTime.equals(other.startTime))
			return false;
		if (stopTime == null) {
			if (other.stopTime != null)
				return false;
		} else if (!stopTime.equals(other.stopTime))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Orbit [orbitNumber=" + orbitNumber + ", startTime=" + startTime + ", stopTime=" + stopTime + "]";
	}
	
	
}
