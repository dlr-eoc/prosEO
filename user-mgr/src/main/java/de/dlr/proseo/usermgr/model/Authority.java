/**
 * Authority.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.hibernate.annotations.Parent;

/**
 * An authority (also called a "privilege") is an atomic entitlement for a user to access a prosEO method or domain object.
 * 
 * @author Dr. Thomas Bassler
 */
@Embeddable
public class Authority {

	/** 
	 * The authority identifier (may be prefixed by "ROLE_" to indicate a user role, or by other common prefixes).
	 * An authority identifier may occur at most once for each user.
	 */
	@Column(nullable = false)
	private String authority;
	
	/** The user, to which the authority (privilege) was granted */
	@Parent
	private User user;

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
	 * Gets the user holding the authority
	 * 
	 * @return the user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Sets the user holding the authority
	 * 
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public int hashCode() {
		return Objects.hash(authority, user);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Authority))
			return false;
		Authority other = (Authority) obj;
		return Objects.equals(authority, other.authority) && Objects.equals(user, other.user);
	}
	
}
