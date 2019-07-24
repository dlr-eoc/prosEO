/**
 * ProcessingFacility.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * A processing facility for running prosEO jobs and storing prosEO product files,  e. g. at a cloud service provider.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class ProcessingFacility extends PersistentObject {

	/** The facility name (unique key) */
	private String name;
	
	/** A short description of the processing facility */
	private String description;

	/**
	 * Gets the name of the processing facility
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the processing facility
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the description of the processing facility
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of the processing facility
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(name);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProcessingFacility))
			return false;
		ProcessingFacility other = (ProcessingFacility) obj;
		return Objects.equals(name, other.name);
	}
	
}