/**
 * Downlink.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * A continuous transmission of satellite data (both housekeeping and payload data) from the satellite to a specific ground station.
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "downlink_id") 
})
public class Downlink extends PersistentObject {

	/** Downlink ID (unique within spacecraft; usually from Mission Planning) */
	@Column(nullable = false)
	private String downlinkId;
	
	/** The spacecraft performing the downlink */
	@ManyToOne(optional = false)
	private Spacecraft spacecraft;
	
	/** The ground station to which the downlink is effected */
	@ManyToOne
	private GroundStation groundStation;
	
	/** The list of data segments contained in this downlink, ordered by reception time */
	@OneToMany(mappedBy = "downlink")
	private List<DataSegment> dataSegments = new ArrayList<>();
	
	/** Downlink channels expected (should be at least one) */
	private List<String> channels = new ArrayList<>();
	
	/** Start time of data acquisition (AOS, acquisition of signal), if known */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant acquisitionStartTime;
	
	/** Start time of data reception */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant dataStartTime;
	
	/** End time of data reception */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant dataStopTime;
	
	/** Timestamp for data availability on ground station pickup point */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant pickupAvailabilityTime;
	
	/** Indicates reception of downlink data by prosEO */
	@Column(nullable = false)
	private Boolean isReceived = false;
	
	/** Size of the received data */
	private Long receivedSize;
	
	/** Time of download completion from ground station pickup point */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant receptionTime;

	/**
	 * Gets the downlink ID
	 * 
	 * @return the downlink ID
	 */
	public String getDownlinkId() {
		return downlinkId;
	}

	/**
	 * SGets the downlink ID
	 * 
	 * @param downlinkId the downlink ID to set
	 */
	public void setDownlinkId(String downlinkId) {
		this.downlinkId = downlinkId;
	}

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
	 * Gets the list of data segments contained in this downlink, ordered by reception time
	 * 
	 * @return the data segments in the downlink
	 */
	public List<DataSegment> getDataSegments() {
		return dataSegments;
	}

	/**
	 * Sets the list of data segments contained in this downlink
	 * 
	 * @param dataSegments the data segments in the downlink to set, ordered by reception time
	 */
	public void setDataSegments(List<DataSegment> dataSegments) {
		this.dataSegments = dataSegments;
	}

	/**
	 * Gets the expected downlink channels
	 * 
	 * @return the downlink channels
	 */
	public List<String> getChannels() {
		return channels;
	}

	/**
	 * Sets the expected downlink channels
	 * 
	 * @param channels the downlink channels to set
	 */
	public void setChannels(List<String> channels) {
		this.channels = channels;
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

	/**
	 * Indicates reception of downlink data by prosEO
	 * 
	 * @return true, if the downlink has been fully received, and false otherwise
	 */
	public Boolean getIsReceived() {
		return isReceived;
	}

	/**
	 * Indicates reception of downlink data by prosEO (convenience method for better readability)
	 * 
	 * @return true, if the downlink has been fully received, and false otherwise
	 */
	public Boolean isReceived() {
		return getIsReceived();
	}

	/**
	 * Sets whether the downlink has been fully received
	 * 
	 * @param isReceived true, if the downlink has been fully received, and false otherwise
	 */
	public void setIsReceived(Boolean isReceived) {
		this.isReceived = isReceived;
	}

	/**
	 * Gets the size of the received data
	 * 
	 * @return the received data size
	 */
	public Long getReceivedSize() {
		return receivedSize;
	}

	/**
	 * Sets the size of the received data
	 * 
	 * @param receivedSize the received data size to set
	 */
	public void setReceivedSize(Long receivedSize) {
		this.receivedSize = receivedSize;
	}

	/**
	 * Gets the time of download completion from ground station pickup point
	 * 
	 * @return the reception time
	 */
	public Instant getReceptionTime() {
		return receptionTime;
	}

	/**
	 * Sets the time of download completion from ground station pickup point
	 * 
	 * @param receptionTime the reception time to set
	 */
	public void setReceptionTime(Instant receptionTime) {
		this.receptionTime = receptionTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(downlinkId, spacecraft);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Downlink))
			return false;
		Downlink other = (Downlink) obj;
		return Objects.equals(downlinkId, other.downlinkId) && Objects.equals(spacecraft, other.spacecraft);
	}

	@Override
	public String toString() {
		return "Downlink [id=" + this.getId() + ", downlinkId=" + downlinkId + ", spacecraft=" + spacecraft.getCode() 
				+ ", groundStation=" + (null == groundStation ? "null" : groundStation.getStationId()) 
				+ ", dataSegments=" + dataSegments + ", channels=" + channels 
				+ ", acquisitionStartTime=" + acquisitionStartTime + ", dataStartTime=" + dataStartTime 
				+ ", dataStopTime=" + dataStopTime + ", pickupAvailabilityTime=" + pickupAvailabilityTime 
				+ ", isReceived=" + isReceived + ", receivedSize=" + receivedSize + ", receptionTime=" + receptionTime + "]";
	}
	
}
