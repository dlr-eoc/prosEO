/**
 * User.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * A prosEO user (actually the user's credentials).
 * 
 * @author Dr. Thomas Bassler
 */
@Entity(name = "users")
public class User implements Persistable<String> {

	/** 
	 * The (unique) user name, consisting of the mission code, a hyphen ("-") and the actual user name 
	 * (which is intended to be used across missions). 
	 */
	@Id
	private String username;
	
	/** The user's password (BCrypt encoded). */
	@Column(nullable = false)
	private String password;
	
	/** Flag indicating whether the user account is enabled. */
	@Column(nullable = false)
	private Boolean enabled = true;
	
	/** The expiration date for the user account (default [almost] never) */
	@Column(nullable = false)
	private Date expirationDate = Date.from(Instant.now().plus(36500, ChronoUnit.DAYS));
	
	/** The expiration date of the password (default [almost] never) */
	@Column(nullable = false)
	private Date passwordExpirationDate = Date.from(Instant.now().plus(36500, ChronoUnit.DAYS));
	
	/** Data download quota for this user (if not set, no quota applies) */
	private Quota quota;
	
	/** The authorities (privileges) granted to this user */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "authorities", joinColumns = {
		@JoinColumn(
			name = "username", 
			foreignKey = @ForeignKey(name = "fk_authorities_users")
	)})
	private Set<Authority> authorities = new HashSet<>();
	
	/** The user groups this user belongs to */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<GroupMember> groupMemberships = new HashSet<>();
	
	/** Flag indicating whether this instance has been loaded already */
	@Transient
	private boolean isNew = true; 
	
	/**
	 * Gets the user name
	 * 
	 * @return the user name
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the user name
	 * 
	 * @param username the user name to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the encrypted password
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the encrypted password
	 * 
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Indicates whether the user account is enabled; an account is always disabled, if its expiration date is in the past
	 * 
	 * @return true, if the user account is enabled, false otherwise
	 */
	public Boolean getEnabled() {
		if (enabled && (new Date()).after(expirationDate)) {
			enabled = false;
		}
		return enabled;
	}

	/**
	 * Sets the enabling status of the user account
	 * 
	 * @param enabled the status to set
	 */
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Gets the expiration date of the account
	 * 
	 * @return the expirationDate
	 */
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * Sets the account expiration date
	 * 
	 * @param expirationDate the expiration date to set
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	/**
	 * Gets the expiration date of the password
	 * 
	 * @return the password expiration date
	 */
	public Date getPasswordExpirationDate() {
		return passwordExpirationDate;
	}

	/**
	 * Sets the password expiration date
	 * 
	 * @param passwordExpirationDate the password expiration date to set
	 */
	public void setPasswordExpirationDate(Date passwordExpirationDate) {
		this.passwordExpirationDate = passwordExpirationDate;
	}

	/**
	 * Gets the data download quota
	 * 
	 * @return a Quota object
	 */
	public Quota getQuota() {
		return quota;
	}

	/**
	 * Gets the data download quota
	 * 
	 * @param quota the Quota object to set
	 */
	public void setQuota(Quota quota) {
		this.quota = quota;
	}

	/**
	 * Gets the user's set of authorities
	 * 
	 * @return a set of authorities
	 */
	public Set<Authority> getAuthorities() {
		return authorities;
	}

	/**
	 * Sets the user's set of authorities
	 * 
	 * @param authorities the authorities to set
	 */
	public void setAuthorities(Set<Authority> authorities) {
		this.authorities = authorities;
	}

	/**
	 * Gets the group memberships for this user
	 * 
	 * @return the groupMemberships
	 */
	public Set<GroupMember> getGroupMemberships() {
		return groupMemberships;
	}

	/**
	 * Sets the group memberships for this user
	 * 
	 * @param groupMemberships the groupMemberships to set
	 */
	public void setGroupMemberships(Set<GroupMember> groupMemberships) {
		this.groupMemberships = groupMemberships;
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof User))
			return false;
		User other = (User) obj;
		return Objects.equals(username, other.username);
	}

	@Override
	public String getId() {
		return username;
	}

	@Override
	public boolean isNew() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * Switch "isNew" flag to indicate an existing entity after a repository call to save(…) or an instance creation
	 * by the persistence provider. (cf. https://docs.spring.io/spring-data/jpa/docs/2.2.5.RELEASE/reference/html/#reference)
	 */
	@PrePersist 
	@PostLoad
	void markNotNew() {
		this.isNew = false;
	}
}
