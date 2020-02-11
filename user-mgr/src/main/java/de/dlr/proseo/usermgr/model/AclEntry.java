/**
 * AclEntry.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Stores the ACL permissions which apply to a specific object identity and security identity
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "unique_uk_4", columnNames = {"acl_object_identity", "ace_order"})})
public class AclEntry {

	/** Database generated unique ID for this ACL entry. */
	@Id
	@GeneratedValue
	@Column(columnDefinition = "BIGSERIAL")
	private long id;
	
	/** The ACL domain object referenced by this ACL entry */
	@ManyToOne
	@JoinColumn(name="acl_object_identity")
	private AclObjectIdentity aclObjectIdentity;
	
	/** An ordering sequence for ACL entries relative to TBD. (?) */
	@Column(name = "ace_order")
	private int aceOrder;
	
	/** The security identity referenced by this ACL entry */
	@ManyToOne
	@JoinColumn(name="sid")
	private AclSid sid;
	
	/** A permission mask, indicating permissions bitwise (e. g. Create, Read, Update, Delete). */
	private int mask;
	
	/** Flag indicating whether the ACL entry grants ("true") or revokes ("false") permissions. */
	private boolean granting;
	
	/** Flag indicating whether an audit trail entry shall be written upon successful authorization. */
	private boolean auditSuccess;
	
	/** Flag indicating whether an audit trail entry shall be written upon failed authorization. */
	private boolean auditFailure;

	/**
	 * Gets the database id of this ACL entry
	 * 
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the database id of this ACL entry
	 * 
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets the domain object referenced by this entry
	 * 
	 * @return the domain object
	 */
	public AclObjectIdentity getAclObjectIdentity() {
		return aclObjectIdentity;
	}

	/**
	 * Sets the domain object referenced by this entry
	 * 
	 * @param aclObjectIdentity the domain object to set
	 */
	public void setAclObjectIdentity(AclObjectIdentity aclObjectIdentity) {
		this.aclObjectIdentity = aclObjectIdentity;
	}

	/**
	 * Gets the ordering index of this entry
	 * 
	 * @return the ordering index
	 */
	public int getAceOrder() {
		return aceOrder;
	}

	/**
	 * Sets the ordering index of this entry
	 * 
	 * @param aceOrder the ordering index to set
	 */
	public void setAceOrder(int aceOrder) {
		this.aceOrder = aceOrder;
	}

	/**
	 * Gets the identifier of the security identity referenced by this entry
	 * 
	 * @return the security identity identifier
	 */
	public AclSid getSid() {
		return sid;
	}

	/**
	 * Sets the identifier of the security identity referenced by this entry
	 * 
	 * @param sid the security identity identifier to set
	 */
	public void setSid(AclSid sid) {
		this.sid = sid;
	}

	/**
	 * Gets the permissions mask
	 * 
	 * @return the mask
	 */
	public int getMask() {
		return mask;
	}

	/**
	 * Sets the permissions mask
	 * 
	 * @param mask the mask to set
	 */
	public void setMask(int mask) {
		this.mask = mask;
	}

	/**
	 * Indicates whether this entry grants or revokes permissions
	 * 
	 * @return true, if the entry grants permissions, false otherwise
	 */
	public boolean isGranting() {
		return granting;
	}

	/**
	 * Sets whether this entry grants (true) or revokes (false) permissions
	 * 
	 * @param granting the granting flag to set
	 */
	public void setGranting(boolean granting) {
		this.granting = granting;
	}

	/**
	 * Indicates whether successful authorization shall be audited
	 * 
	 * @return true, if audit on success is requested, false otherwise
	 */
	public boolean isAuditSuccess() {
		return auditSuccess;
	}

	/**
	 * Sets whether successful authorization shall be audited
	 * 
	 * @param auditSuccess the flag for audit on success to set
	 */
	public void setAuditSuccess(boolean auditSuccess) {
		this.auditSuccess = auditSuccess;
	}

	/**
	 * Indicates whether failed authorization shall be audited
	 * 
	 * @return true, if audit on failure is requested, false otherwise
	 */
	public boolean isAuditFailure() {
		return auditFailure;
	}

	/**
	 * Sets whether failed authorization shall be audited
	 * 
	 * @param auditFailure the flag for audit on failure to set
	 */
	public void setAuditFailure(boolean auditFailure) {
		this.auditFailure = auditFailure;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aceOrder, aclObjectIdentity);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AclEntry))
			return false;
		AclEntry other = (AclEntry) obj;
		return aceOrder == other.aceOrder && Objects.equals(aclObjectIdentity, other.aclObjectIdentity);
	}

}
