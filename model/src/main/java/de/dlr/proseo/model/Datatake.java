/**
 * Datatake.java
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * A contiguous time-bound slice of instrument data, usually separated from adjacent slices (datatakes)
 * by a gap in the sensing data.
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = {
		@Index(unique = true, columnList = "datatake_id"),
		@Index(unique = false, columnList = "sensing_start_time") 
})
public class Datatake extends PersistentObject {
	
	/** The (external) datatake ID (usually from Mission Planning) */
	@Column(nullable = false)
	private String datatakeId;
	
	/** The satellite payload the datatake belongs to */
	@ManyToOne
	private Payload payload;
	
	/** The orbit the datatake belongs to (if known) */
	@ManyToOne
	private Orbit orbit;
	
	/** The datatake group the datatake belongs to (if any) */
	@ManyToOne
	private DatatakeGroup datatakeGroup;
	
	/** The list of data segments contained making up this datatake, ordered by sequence number */
	@OneToMany(mappedBy = "datatake")
	private List<DataSegment> dataSegments = new ArrayList<>();
	
	/** Instrument sensor mode (abbreviated; one of the values defined in the associated Payload object) */
	private String instrumentMode;
	
	/** Sensing start time (start of the instrument observation) */
	@Column(nullable = false, columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStartTime;
	
	/** Observation duration */
	@Column(nullable = false)
	private Duration datatakeDuration;

	/** Completeness of downlink segments for datatake (true, if all data segments for the datatake have been received) */
	@Column(nullable = false)
	private Boolean isComplete = false;
	
	/** Availability time for last instrument data (e.g. CCSDS Channel Access Data Units [CADUs]) of this datatake. */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant deliveryStopTime;
	
	/** Processing time for L0 product from this datatake (only first processing is recorded) */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant processingTime;

	/**
	 * Gets the (external) datatake ID
	 * 
	 * @return the datatake ID
	 */
	public String getDatatakeId() {
		return datatakeId;
	}

	/**
	 * Sets the (external) datatake ID
	 * 
	 * @param datatakeId the datatake ID to set
	 */
	public void setDatatakeId(String datatakeId) {
		this.datatakeId = datatakeId;
	}

	/**
	 * Gets the payload this datatake belongs to
	 * 
	 * @return the associated payload
	 */
	public Payload getPayload() {
		return payload;
	}

	/**
	 * Sets the payload this datatake belongs to
	 * 
	 * @param payload the payload to set
	 */
	public void setPayload(Payload payload) {
		this.payload = payload;
	}

	/**
	 * Gets the satellite orbit this datatake belongs to
	 * 
	 * @return the orbit or null, if no orbit is associated
	 */
	public Orbit getOrbit() {
		return orbit;
	}

	/**
	 * Sets the satellite orbit this datatake belongs to
	 * 
	 * @param orbit the orbit to set
	 */
	public void setOrbit(Orbit orbit) {
		this.orbit = orbit;
	}

	/**
	 * Gets the datatake group this datatake belongs to (if any)
	 * 
	 * @return the overarching datatake group or null, if no such group exists
	 */
	public DatatakeGroup getDatatakeGroup() {
		return datatakeGroup;
	}

	/**
	 * Sets the datatake group this datatake belongs to (if any)
	 * 
	 * @param datatakeGroup the datatake group to set
	 */
	public void setDatatakeGroup(DatatakeGroup datatakeGroup) {
		this.datatakeGroup = datatakeGroup;
	}

	/**
	 * Gets the list of data segments contained making up this datatake, ordered by sequence number
	 * 
	 * @return the data segment list
	 */
	public List<DataSegment> getDataSegments() {
		return dataSegments;
	}

	/**
	 * Sets the list of data segments contained making up this datatake
	 * 
	 * @param dataSegments the data segment list to set, ordered by sequence number
	 */
	public void setDataSegments(List<DataSegment> dataSegments) {
		this.dataSegments = dataSegments;
	}

	/**
	 * Gets the instrument sensor mode
	 * 
	 * @return the instrument sensor mode
	 */
	public String getInstrumentMode() {
		return instrumentMode;
	}

	/**
	 * Sets the instrument sensor mode
	 * 
	 * @param instrumentMode the instrument sensor mode to set
	 */
	public void setInstrumentMode(String instrumentMode) {
		this.instrumentMode = instrumentMode;
	}

	/**
	 * Gets the instrument observation (sensig) start time
	 * 
	 * @return the sensing start time
	 */
	public Instant getSensingStartTime() {
		return sensingStartTime;
	}

	/**
	 * Sets the instrument observation (sensig) start time
	 * 
	 * @param sensingStartTime the sensing start time to set
	 */
	public void setSensingStartTime(Instant sensingStartTime) {
		this.sensingStartTime = sensingStartTime;
	}

	/**
	 * Gets the duration of the instrument observation
	 * 
	 * @return the datatake duration
	 */
	public Duration getDatatakeDuration() {
		return datatakeDuration;
	}

	/**
	 * Sets the duration of the instrument observation
	 * 
	 * @param datatakeDuration the datatake duration to set
	 */
	public void setDatatakeDuration(Duration datatakeDuration) {
		this.datatakeDuration = datatakeDuration;
	}

	/**
	 * Tests whether the datatake reception is complete
	 * 
	 * @return true, if all segments of the datatake have been received, false otherwise
	 */
	public Boolean getIsComplete() {
		return isComplete;
	}

	/**
	 * Sets a flag indicatin whether the datatake reception is complete
	 * 
	 * @param isComplete true, if all segments of the datatake have been received, false otherwise
	 */
	public void setIsComplete(Boolean isComplete) {
		this.isComplete = isComplete;
	}

	/**
	 * Gets the availability time for the last instrument data package at the ground station
	 * 
	 * @return the delivery stop time
	 */
	public Instant getDeliveryStopTime() {
		return deliveryStopTime;
	}

	/**
	 * Sets the availability time for the last instrument data package at the ground station
	 * 
	 * @param deliveryStopTime the delivery stop time to set
	 */
	public void setDeliveryStopTime(Instant deliveryStopTime) {
		this.deliveryStopTime = deliveryStopTime;
	}

	/**
	 * Gets the first processing time for an L0 product from this datatake
	 * 
	 * @return the L0 processing time
	 */
	public Instant getProcessingTime() {
		return processingTime;
	}

	/**
	 * Sets the first processing time for an L0 product from this datatake
	 * 
	 * @param processingTime the processingTime to set
	 */
	public void setProcessingTime(Instant processingTime) {
		this.processingTime = processingTime;
	}
	
	
}
