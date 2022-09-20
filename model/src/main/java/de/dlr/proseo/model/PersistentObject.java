/**
 * PersistentObject.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Abstract superclass of all persistent classes
 * 
 * @author Thomas Bassler
 */
@MappedSuperclass
abstract public class PersistentObject {
	/** Next object id for assignment */
	private static long nextId = System.currentTimeMillis(); // Seeded by the current time

	/**
	 * The persistent id of this object (an "assigned identifier" according to JPA).
	 */
	@GeneratedValue
	@Id
	private long id;
	
	/**
	 * A version identifier to track updates to the object (especially to detect concurrent update attempts).
	 */
	private int version;
	
	/**
	 * Get the next available object id
	 * @return a unique object id
	 */
	private static synchronized long getNextId() {
		return ++nextId;
	}

	/**
	 * No-argument constructor that assigns the object id and initializes the version number
	 */
	public PersistentObject() {
		super();
		id = getNextId();
		version = 1;
	}

	/**
	 * Set the id of the persistent object.
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets the id of the persistent object
	 * @return the object id
	 */
	public long getId() {
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