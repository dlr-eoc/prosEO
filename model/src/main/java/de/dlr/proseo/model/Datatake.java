/**
 * Datatake.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

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
	
	/** The satellite the datatake belongs to (somewhat redundant, but there is no navigation path from payload to spacecraft */
	@ManyToOne(optional = false)
	private Spacecraft spacecraft;
	
	/** The satellite payload the datatake belongs to */
	@Embedded
	@NotNull
	private Payload payload;
	
	/** The orbit the datatake belongs to (if known) */
	@ManyToOne
	private Orbit orbit;
	
	/** The datatake group the datatake belongs to (if any) */
	@ManyToOne
	private DatatakeGroup datatakeGroup;
	
	/** The list of data segments contained making up this datatake, ordered by sequence number */
	@OneToMany(mappedBy = "datatake")
	@OrderBy("sequence_number")
	private Set<DataSegment> dataSegments = 
		new TreeSet<>((ds1, ds2) -> { return ds1.getSegmentNumber().compareTo(ds2.getSegmentNumber()); });;
	
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
	
	/** Additional mission-specific parameters for the datatake */
	@ElementCollection
	private Map<String, Parameter> parameters = new HashMap<>();
	
	/** L0 products derived from this datatake (not yet implemented from Product side) */
	@ManyToMany
	private Set<Product> l0Products = new HashSet<>();

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
	 * Gets the spacecraft owning this datatake
	 * 
	 * @return the owning spacecraft
	 */
	public Spacecraft getSpacecraft() {
		return spacecraft;
	}

	/**
	 * Sets the spacecraft owning this datatake
	 * 
	 * @param spacecraft the owning spacecraft to set
	 */
	public void setSpacecraft(Spacecraft spacecraft) {
		this.spacecraft = spacecraft;
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
	 * @return a set of data segments ordered by segment number
	 */
	public Set<DataSegment> getDataSegments() {
		return dataSegments;
	}

	/**
	 * Sets the list of data segments contained making up this datatake
	 * 
	 * @param dataSegments the data segment set to set, ordered by sequence number
	 */
	public void setDataSegments(TreeSet<DataSegment> dataSegments) {
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

	/**
	 * Gets the mission-specific datatake parameters
	 * 
	 * @return a list of datatake parameters
	 */
	public Map<String, Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Sets the mission-specific datatake parameters
	 * 
	 * @param parameters the list of datatake parameters to set
	 */
	public void setParameters(Map<String, Parameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Gets the L0 products derived from this datatake
	 * 
	 * @return the associated L0 products
	 */
	public Set<Product> getL0Products() {
		return l0Products;
	}

	/**
	 * Sets the L0 products derived from this datatake
	 * 
	 * @param l0Products the associated L0 products to set
	 */
	public void setL0Products(Set<Product> l0Products) {
		this.l0Products = l0Products;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(spacecraft, payload, datatakeId);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Datatake))
			return false;
		Datatake other = (Datatake) obj;
		return Objects.equals(spacecraft, other.spacecraft) && Objects.equals(payload, other.payload) 
				&& Objects.equals(datatakeId, other.datatakeId);
	}

	@Override
	public String toString() {
		return "Datatake [id = " + this.getId() + ", datatakeId=" + datatakeId + ", spacecraft=" + spacecraft.getCode() 
				+ ", payload=" + payload.getName() 
				+ ", orbit=" + (null == orbit ? "null" : orbit.getOrbitNumber())
				+ ", datatakeGroup=" + datatakeGroup 
				+ ", dataSegments=" + dataSegments + ", instrumentMode=" + instrumentMode 
				+ ", sensingStartTime=" + sensingStartTime + ", datatakeDuration=" + datatakeDuration 
				+ ", isComplete=" + isComplete + ", deliveryStopTime=" + deliveryStopTime 
				+ ", processingTime=" + processingTime + ", parameters=" + parameters + "]";
	}

}
