/**
 * NotificationEndpoint.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * The endpoint to send order completion notifications to
 * 
 * @author Dr. Thomas Bassler
 */
@Embeddable
public class NotificationEndpoint {

	/**
	 * The URI used to notify the ProcessingOrder originator of the order completion
	 * (allowed protocols are "http:", "https:", "mailto:", "http:" only if no authentication is required)
	 */
	@Column(name = "endpoint_uri")
	private String uri;
	
	/** The username to authenticate with (using Basic Authentication) */
	@Column(name = "endpoint_username")
	private String username;
	
	/** The password to authenticate with (using Basic Authentication), mandatory if username is given */
	@Column(name = "endpoint_password")
	private String password;

	/**
	 * Gets the notification endpoint URI
	 * 
	 * @return the URI
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Sets the notification endpoint URI
	 * 
	 * @param uri the URI to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Gets the username
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username
	 * 
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the password (as stored, i. e. encrypted, if it was stored encrypted, clear text otherwise)
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password; the password is not further encrypted, so only a (two-way) encrypted password should be provided.
	 * 
	 * @param password the (encrypted) password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public int hashCode() {
		return Objects.hash(uri, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof NotificationEndpoint))
			return false;
		NotificationEndpoint other = (NotificationEndpoint) obj;
		return Objects.equals(uri, other.getUri()) && Objects.equals(username, other.getUsername());
	}

	@Override
	public String toString() {
		return "NotificationEndpoint [uri=" + uri + ", username=" + username + "]";
	}
	
}
