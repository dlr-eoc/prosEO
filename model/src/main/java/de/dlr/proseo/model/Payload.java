/**
 * Payload.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;

import javax.persistence.Embeddable;

/**
 * The payload of a spacecraft, e. g. the TROPOMI instrument of Sentinel-5P. A spacecraft may have multiple payloads.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Embeddable
public class Payload {
	
	/** The payload name (instrument short name), e. g. "TROPOMI". */
	private String name;
	
	/** The payload description */
	private String description;

	/**
	 * Gets the payload name
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the payload name
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the payload description
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the payload description
	 * 
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Payload))
			return false;
		Payload other = (Payload) obj;
		return Objects.equals(name, other.getName());
	}

}
