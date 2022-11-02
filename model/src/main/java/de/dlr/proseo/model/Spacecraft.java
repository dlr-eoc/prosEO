/**
 * Spacecraft.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

/**
 * The abstraction of a spacecraft used for a specific Mission. A Mission may operate more than one spacecraft.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = @Index(unique = true, columnList = "mission_id, code"))
public class Spacecraft extends PersistentObject {

	/** The mission this spacecraft belongs to */
	@ManyToOne
	private Mission mission;
	
	/** The spacecraft code (e. g. S5P), unique within a mission */
	private String code;
	
	/** The spacecraft name (e. g. Sentinel-5 Precursor) */
	private String name;
	
	/** The orbits this spacecraft performs */
	@OneToMany(mappedBy = "spacecraft", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orbitNumber")
	private List<Orbit> orbits = new ArrayList<>();
	
	/** The payloads flying on this spacecraft */
	@ElementCollection
	private List<Payload> payloads = new ArrayList<>();
	
	/**
	 * Gets the mission of this spacecraft
	 * 
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Sets the mission of this spacecraft
	 * 
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}

	/**
	 * Gets the spacecraft code
	 * 
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * Sets the spacecraft code
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}
	
	/**
	 * Gets the spacecraft name
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the spacecraft name
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the orbits of the spacecraft
	 * @return the orbits
	 */
	public List<Orbit> getOrbits() {
		return orbits;
	}

	/**
	 * Set the orbits of the spacecraft
	 * @param orbits the orbits to set
	 */
	public void setOrbits(List<Orbit> orbits) {
		this.orbits = orbits;
	}

	/**
	 * Gets the list of payloads
	 * 
	 * @return the payloads
	 */
	public List<Payload> getPayloads() {
		return payloads;
	}

	/**
	 * Sets the list of payloads
	 * 
	 * @param payloads the payloads to set
	 */
	public void setPayloads(List<Payload> payloads) {
		this.payloads = payloads;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((mission == null) ? 0 : mission.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof Spacecraft))
			return false;
		Spacecraft other = (Spacecraft) obj;
		return Objects.equals(code, other.getCode()) && Objects.equals(mission, other.getMission());
	}

	@Override
	public String toString() {
		return "Spacecraft [code=" + code + "]";
	}
}
