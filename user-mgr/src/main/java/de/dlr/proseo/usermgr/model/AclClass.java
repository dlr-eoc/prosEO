/**
 * AclClass.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Domain object types to which ACLs apply. The class column stores the Java class name of the object.
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "unique_uk_2", columnNames = {"class"})})
public class AclClass {

	/** Database generated ID for this ACL class. */
	@Id
	@GeneratedValue
	private long id;
	
	/** The Java class name. */
	@Column(name = "class", nullable = false, unique = true)
	private String className;

	/**
	 * Gets the database id of this ACL class
	 * 
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the database id of this ACL class
	 * 
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets the fully qualified name of the Java class represented by this ACL class
	 * 
	 * @return the class name
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Sets the fully qualified name of the Java class represented by this ACL class
	 * 
	 * @param className the class name to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	public int hashCode() {
		return Objects.hash(className);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AclClass))
			return false;
		AclClass other = (AclClass) obj;
		return Objects.equals(className, other.className);
	}
}
