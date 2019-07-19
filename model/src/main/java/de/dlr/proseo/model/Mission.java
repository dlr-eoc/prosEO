/**
 * Mission.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.OneToMany;

/**
 * An Earth Observation mission.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class Mission extends PersistentObject {

	/** The mission code (e. g. S5P) */
	private String code;
	/** The mission name (e. g. Sentinel-5 Precursor) */
	private String name;
	
	/** The spacecrafts this mission owns */
	@OneToMany
	private Set<Spacecraft> spacecrafts;
	/** The product classes this mission produces or uses */
	@OneToMany
	private Set<ProductClass> productClasses;
	
	/**
	 * Gets the mission code
	 * 
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * Sets the mission code
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}
	
	/**
	 * Gets the mission name
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the mission name
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the set of spacecrafts
	 * @return the spacecrafts
	 */
	public Set<Spacecraft> getSpacecrafts() {
		return spacecrafts;
	}

	/**
	 * Sets the set of spacecrafts
	 * @param spacecrafts the spacecrafts to set
	 */
	public void setSpacecrafts(Set<Spacecraft> spacecrafts) {
		this.spacecrafts = spacecrafts;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Mission))
			return false;
		Mission other = (Mission) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		return true;
	}
}
