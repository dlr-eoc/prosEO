/**
 * GroupAuthority.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Parent;

/**
 * A group authority (also called a "privilege") is an atomic entitlement for a group of users to access a prosEO method or domain object.
 * 
 * @author Dr. Thomas Bassler
 */
@Embeddable
public class GroupAuthority {

	/** The identifier of the authority. A group authority may occur at most once for each group. */
	@Column(nullable = false)
	private String authority;
	
	@Parent
	private Group group;

	/**
	 * Gets the authority identifier
	 * 
	 * @return the authority identifier
	 */
	public String getAuthority() {
		return authority;
	}

	/**
	 * Sets the authority identifier
	 * 
	 * @param authority the authority identifier to set
	 */
	public void setAuthority(String authority) {
		this.authority = authority;
	}

	/**
	 * Gets the user group holding this authority
	 * 
	 * @return the group
	 */
	public Group getGroup() {
		return group;
	}

	/**
	 * Sets the user group holding this authority
	 * 
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	@Override
	public int hashCode() {
		return Objects.hash(authority, group);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof GroupAuthority))
			return false;
		GroupAuthority other = (GroupAuthority) obj;
		return Objects.equals(authority, other.authority) && Objects.equals(group, other.group);
	}

}
