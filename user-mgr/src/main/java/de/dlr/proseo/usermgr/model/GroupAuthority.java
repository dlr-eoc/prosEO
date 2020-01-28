/**
 * GroupAuthority.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.OneToOne;

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

}
