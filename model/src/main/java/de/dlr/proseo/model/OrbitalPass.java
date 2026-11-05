/**
 * OrbitalPass.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

/**
 * An orbital pass (or simply pass) is the period in which a spacecraft is above the local horizon, and thus available for 
 * line-of-sight communication with a given ground station. (Wikipedia)
 * 
 * In its simplest form, an OrbitalPass just provides a link from a Downlink to a GroundStation.
 * 
 * @author Dr. Thomas Bassler
 */
@Embeddable
public class OrbitalPass {

	/** The ground station to which the downlink is effected */
	@ManyToOne(optional=false)
	private GroundStation groundStation;
	
	/** Start time of data acquisition (AOS, acquisition of signal), if known */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant acquisitionStartTime;
	
	/** End time of data acquisition (LOS, loss of signal), if known */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant acquisitionStopTime;
	
	/** Start time of data reception */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant dataStartTime;
	
	/** End time of data reception */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant dataStopTime;
	
	/** Timestamp for data availability on ground station pickup point */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant pickupAvailabilityTime;
	
	/**
	 * Gets the associated ground station
	 * 
	 * @return the associated ground station
	 */
	public GroundStation getGroundStation() {
		return groundStation;
	}

	/**
	 * Sets the associated ground station
	 * 
	 * @param groundStation the associated ground station to set
	 */
	public void setGroundStation(GroundStation groundStation) {
		this.groundStation = groundStation;
	}

	/**
	 * Gets the start time of data acquisition (AOS)
	 * 
	 * @return the acquisition start time
	 */
	public Instant getAcquisitionStartTime() {
		return acquisitionStartTime;
	}

	/**
	 * Sets the start time of data acquisition (AOS)
	 * 
	 * @param acquisitionStartTime the acquisition start time to set
	 */
	public void setAcquisitionStartTime(Instant acquisitionStartTime) {
		this.acquisitionStartTime = acquisitionStartTime;
	}

	/**
	 * Gets the end time of data acquisition (LOS)
	 * 
	 * @return the acquisition end time
	 */
	public Instant getAcquisitionStopTime() {
		return acquisitionStopTime;
	}

	/**
	 * Sets the end time of data acquisition (LOS)
	 * 
	 * @param acquisitionStopTime the acquisition end time to set
	 */
	public void setAcquisitionStopTime(Instant acquisitionStopTime) {
		this.acquisitionStopTime = acquisitionStopTime;
	}

	/**
	 * Gets the start time of data reception
	 * 
	 * @return the data reception start time
	 */
	public Instant getDataStartTime() {
		return dataStartTime;
	}

	/**
	 * Sets the start time of data reception
	 * 
	 * @param dataStartTime the data reception start time to set
	 */
	public void setDataStartTime(Instant dataStartTime) {
		this.dataStartTime = dataStartTime;
	}

	/**
	 * Gets the end time of data reception
	 * 
	 * @return the data reception stop time
	 */
	public Instant getDataStopTime() {
		return dataStopTime;
	}

	/**
	 * Sets the end time of data reception
	 * 
	 * @param dataStopTime the data reception stop time to set
	 */
	public void setDataStopTime(Instant dataStopTime) {
		this.dataStopTime = dataStopTime;
	}

	/**
	 * Gets the timestamp for data availability on ground station pickup point
	 * 
	 * @return the ground station pickup availability time
	 */
	public Instant getPickupAvailabilityTime() {
		return pickupAvailabilityTime;
	}

	/**
	 * Sets the timestamp for data availability on ground station pickup point
	 * 
	 * @param pickupAvailabilityTime the ground station pickup availability time to set
	 */
	public void setPickupAvailabilityTime(Instant pickupAvailabilityTime) {
		this.pickupAvailabilityTime = pickupAvailabilityTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(acquisitionStartTime, groundStation);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof OrbitalPass))
			return false;
		OrbitalPass other = (OrbitalPass) obj;
		return Objects.equals(acquisitionStartTime, other.acquisitionStartTime)
				&& Objects.equals(groundStation, other.groundStation);
	}

	@Override
	public String toString() {
		return "OrbitalPass [groundStation=" + groundStation + ", acquisitionStartTime=" + acquisitionStartTime
				+ ", acquisitionStopTime=" + acquisitionStopTime + ", dataStartTime=" + dataStartTime + ", dataStopTime="
				+ dataStopTime + ", pickupAvailabilityTime=" + pickupAvailabilityTime + "]";
	}

}
