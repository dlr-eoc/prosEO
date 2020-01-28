/**
 * AclSid.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Security identities recognised by the ACL system. These can be unique principals (users or groups [?]) or authorities,
 * which may apply to multiple principals
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "unique_uk_1", columnNames = { "sid", "principal"})})
public class AclSid {
	
	/** Database generated unique identifier of the security identity. */
	@Id
	@GeneratedValue
	private long id;
	
	/** Flag indicating whether the security identity is a user (principal) or an authority. */
	@Column(nullable = false)
	private Boolean principal;
	
	/** The name of the principal (user, group) or authority this security identity represents. */
	@Column(nullable = false)
	private String sid;

	/**
	 * Gets the database id
	 * 
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the database id
	 * 
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Indicates whether this security identity represents a principal (true) or an authority (false)
	 * 
	 * @return the principal flag
	 */
	public Boolean getPrincipal() {
		return principal;
	}

	/**
	 * Sets whether this security identity shall represent a principal (true) or an authority (false)
	 * 
	 * @param principal the principal to set
	 */
	public void setPrincipal(Boolean principal) {
		this.principal = principal;
	}

	/**
	 * Gets the identifier of the represented entity (user/group name, authority identifier)
	 * 
	 * @return the identifier of the principal/authority
	 */
	public String getSid() {
		return sid;
	}

	/**
	 * Sets the identifier of the represented entity (user/group name, authority identifier)
	 * 
	 * @param sid the identifier of the principal/authority to set
	 */
	public void setSid(String sid) {
		this.sid = sid;
	}

}
