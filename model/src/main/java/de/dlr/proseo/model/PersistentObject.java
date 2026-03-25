/**
 * PersistentObject.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Abstract superclass of all persistent classes
 * 
 * Updated for Spring Data 3.5 / Hibernate 6.6 as per https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html
 * 
 * @author Thomas Bassler
 */
@MappedSuperclass
abstract public class PersistentObject {

	/**
	 * The persistent id of this object
	 */
	@GeneratedValue
	@Id
	private Long id;
	
	/**
	 * A version identifier to track updates to the object (especially to detect concurrent update attempts), default 1.
	 */
	private Integer version = 1;
	
	/**
	 * Set the id of the persistent object.
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Gets the id of the persistent object
	 * @return the object id
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Gets the version of the persistent object
	 * @return the object version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Increments the version of the persistent object
	 */
	public void incrementVersion() {
		this.version++;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	/**
	 * Test equality of persistent objects based on their unique ID.
	 * 
	 * @param obj the object to compare this object to
	 * @return true, if obj is a persistent object and has the same ID, false otherwise
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof PersistentObject))
			return false;
		PersistentObject other = (PersistentObject) obj;
		return Objects.equals(id, other.getId());
	}

}