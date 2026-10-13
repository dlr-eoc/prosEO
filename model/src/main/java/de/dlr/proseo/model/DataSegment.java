/**
 * DataSegment.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A single segment of a datatake (unit of downlinking from spacecraft)
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = {
		@Index(unique = false, columnList = "datatake_id,segment_number") 
})
public class DataSegment extends PersistentObject {

	/** The sequence number of this segment within the datatake */
	@Column(nullable = false)
	private Integer segmentNumber;
	
	/** The datatake this segment belongs to */
	@ManyToOne
	private Datatake datatake;
	
	/** The downlink providing this data segment */
	@ManyToOne
	private Downlink downlink;
	
	/** The ground station(s) which actually received this data segment (in order of reception time) */
	@ManyToMany
	private List<GroundStation> groundStations = new ArrayList<>();
	
	/** Sensing start time for this segment */
	@Column(nullable = false, columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStartTime;
	
	/** The sensing duration for this segment */
	@Column(nullable = false)
	private Duration segmentDuration;

	/** Actual start of data downlink */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant downlinkStart;
	
	/** Actual end of data downlink */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant downlinkStop;
	
	/** Actual downlink channel (expected to be in the range of the channels for the associated downlink) */
	private String channel;
		
	/**
	 * Gets the sequence number of this segment within the datatake
	 * 
	 * @return the segment number
	 */
	public Integer getSegmentNumber() {
		return segmentNumber;
	}

	/**
	 * Sets the sequence number of this segment within the datatake
	 * 
	 * @param segmentNumber the segment number to set
	 */
	public void setSegmentNumber(Integer segmentNumber) {
		this.segmentNumber = segmentNumber;
	}

	/**
	 * Gets the datatake this segment belongs to
	 * 
	 * @return the owning datatake
	 */
	public Datatake getDatatake() {
		return datatake;
	}

	/**
	 * Sets the datatake this segment belongs to
	 * 
	 * @param datatake the owning datatake to set
	 */
	public void setDatatake(Datatake datatake) {
		this.datatake = datatake;
	}

	/**
	 * Gets the downlink providing this data segment
	 * 
	 * @return the associated downlink
	 */
	public Downlink getDownlink() {
		return downlink;
	}

	/**
	 * Sets the downlink providing this data segment
	 * 
	 * @param downlink the associated downlink to set
	 */
	public void setDownlink(Downlink downlink) {
		this.downlink = downlink;
	}

	/**
	 * Gets the list of ground stations receiving this data segment, ordered by reception time
	 * 
	 * @return the receiving ground stations
	 */
	public List<GroundStation> getGroundStations() {
		return groundStations;
	}

	/**
	 * Sets the list of ground stations receiving this data segment
	 * 
	 * @param groundStations the receiving ground stations to set, ordered by reception time
	 */
	public void setGroundStations(List<GroundStation> groundStations) {
		this.groundStations = groundStations;
	}

	/**
	 * Gets the sensing start time for this segment
	 * 
	 * @return the sensing start time
	 */
	public Instant getSensingStartTime() {
		return sensingStartTime;
	}

	/**
	 * Sets the sensing start time for this segment
	 * 
	 * @param sensingStartTime the sensing start time to set
	 */
	public void setSensingStartTime(Instant sensingStartTime) {
		this.sensingStartTime = sensingStartTime;
	}

	/**
	 * Gets the sensing duration for this segment
	 * 
	 * @return the segment duration
	 */
	public Duration getSegmentDuration() {
		return segmentDuration;
	}

	/**
	 * Sets the sensing duration for this segment
	 * 
	 * @param segmentDuration the segment duration to set
	 */
	public void setSegmentDuration(Duration segmentDuration) {
		this.segmentDuration = segmentDuration;
	}

	/**
	 * Gets the actual start time of the data downlink
	 * 
	 * @return the downlink start time
	 */
	public Instant getDownlinkStart() {
		return downlinkStart;
	}

	/**
	 * Sets the actual start time of the data downlink
	 * 
	 * @param downlinkStart the downlink start time to set
	 */
	public void setDownlinkStart(Instant downlinkStart) {
		this.downlinkStart = downlinkStart;
	}

	/**
	 * Gets the actual end time of the data downlink
	 * 
	 * @return the downlink stop time
	 */
	public Instant getDownlinkStop() {
		return downlinkStop;
	}

	/**
	 * Sets the actual end time of the data downlink
	 * 
	 * @param downlinkStop the downlink stop time to set
	 */
	public void setDownlinkStop(Instant downlinkStop) {
		this.downlinkStop = downlinkStop;
	}

	/**
	 * Gets the sctual downlink channel
	 * 
	 * @return the downlink channel
	 */
	public String getChannel() {
		return channel;
	}

	/**
	 * Sets the sctual downlink channel
	 * 
	 * @param channel the downlink channel to set
	 */
	public void setChannel(String channel) {
		this.channel = channel;
	}

}
