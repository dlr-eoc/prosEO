/**
 * AclObjectIdentity.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Object identity definitions of specific domain objects
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "unique_uk_3", columnNames = {"object_id_class", "object_id_identity"})})
public class AclObjectIdentity {

	/** Database generated unique ID for this ACL object identity. */
	@Id
	@GeneratedValue
	@Column(columnDefinition = "BIGSERIAL")
	private long id;
	
	/** The class this domain object belongs to */
	@ManyToOne
	@JoinColumn(name = "object_id_class")
	private AclClass objectIdClass;
	
	/** An identifier for the domain object represented by this object ID. */
	@Column(name = "object_id_identity", nullable = false)
	private String objectIdIdentity;
	
	/** The parent object of this domain object */
	@ManyToOne
	@JoinColumn(name = "parent_object")
	private AclObjectIdentity parentObject;
	
	/** The owner of this domain object (must be a principal [?]) */
	@ManyToOne
	@JoinColumn(name = "owner_sid")
	private AclSid ownerSid;
	
	/** Flag indicating whether this domain object inherits ACL entries from its parent object. (? TBC) */
	private boolean entriesInheriting;

	/** The ACL entries associated with this object identity */
	@OneToMany(mappedBy = "aclObjectIdentity", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<AclEntry> aclEntries;

	/**
	 * Gets the database id of this domain object
	 * 
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the database id of this domain object
	 * 
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets the class of this domain object
	 * 
	 * @return the object class
	 */
	public AclClass getObjectIdClass() {
		return objectIdClass;
	}

	/**
	 * Sets the class of this domain object
	 * 
	 * @param objectIdClass the object class to set
	 */
	public void setObjectIdClass(AclClass objectIdClass) {
		this.objectIdClass = objectIdClass;
	}

	/**
	 * Gets the identifier of this domain object
	 * 
	 * @return the object identifier
	 */
	public String getObjectIdIdentity() {
		return objectIdIdentity;
	}

	/**
	 * Sets the identifier of this domain object
	 * 
	 * @param objectIdIdentity the object identifier to set
	 */
	public void setObjectIdIdentity(String objectIdIdentity) {
		this.objectIdIdentity = objectIdIdentity;
	}

	/**
	 * Gets the parent object of this domain object
	 * 
	 * @return the parent object
	 */
	public AclObjectIdentity getParentObject() {
		return parentObject;
	}

	/**
	 * Sets the parent object of this domain object
	 * 
	 * @param parentObject the parent object to set
	 */
	public void setParentObject(AclObjectIdentity parentObject) {
		this.parentObject = parentObject;
	}

	/**
	 * Gets the security identity of the object owner
	 * 
	 * @return the owner security identity
	 */
	public AclSid getOwnerSid() {
		return ownerSid;
	}

	/**
	 * Sets the security identity of the object owner
	 * 
	 * @param ownerSid the owner security identity to set
	 */
	public void setOwnerSid(AclSid ownerSid) {
		this.ownerSid = ownerSid;
	}

	/**
	 * Indicates whether this object inherits ACL entries from its parent
	 * 
	 * @return true, if ACL entries are inherited, false otherwise
	 */
	public boolean isEntriesInheriting() {
		return entriesInheriting;
	}

	/**
	 * Sets whether this object inherits ACL entries from its parent
	 * 
	 * @param entriesInheriting the inheritance flag to set
	 */
	public void setEntriesInheriting(boolean entriesInheriting) {
		this.entriesInheriting = entriesInheriting;
	}

	/**
	 * Gets the ACL entries associated with this object identity
	 * 
	 * @return the aclEntries
	 */
	public Set<AclEntry> getAclEntries() {
		return aclEntries;
	}

	/**
	 * Sets the ACL entries associated with this object identity
	 * 
	 * @param aclEntries the aclEntries to set
	 */
	public void setAclEntries(Set<AclEntry> aclEntries) {
		this.aclEntries = aclEntries;
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectIdClass, objectIdIdentity);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AclObjectIdentity))
			return false;
		AclObjectIdentity other = (AclObjectIdentity) obj;
		return Objects.equals(objectIdClass, other.objectIdClass) && Objects.equals(objectIdIdentity, other.objectIdIdentity);
	}
	
	
}
