/**
 * Spacecraft.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
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
@Table(indexes = @Index(unique = true, columnList = "code"))
public class Spacecraft extends PersistentObject {

	/** The mission this spacecraft belongs to */
	@ManyToOne
	private Mission mission;
	
	/** The spacecraft code (e. g. S5P) */
	private String code;
	
	/** The spacecraft name (e. g. Sentinel-5 Precursor) */
	private String name;
	
	/** The orbits this spacecraft performs */
	@OneToMany(mappedBy = "spacecraft", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orbitNumber")
	private List<Orbit> orbits = new ArrayList<>();
	
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
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Spacecraft))
			return false;
		Spacecraft other = (Spacecraft) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (mission == null) {
			if (other.mission != null)
				return false;
		} else if (!mission.equals(other.mission))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Spacecraft [code=" + code + "]";
	}
}
