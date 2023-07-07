/**
 * GUIAuthenticationToken.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Represents a token for an authentication within a Graphical User Interface. It holds the credentials, principal details and other
 * related data like roles, mission, etc. This class is used for authenticating a user within a GUI-based application.
 *
 * @author David Mazo
 */
public class GUIAuthenticationToken implements Authentication {

	/** Serialization ID. */
	private static final long serialVersionUID = 8032965813126496854L;

	/** The credentials of the user. */
	private Object credentials;

	/** The user's details. */
	private UserDetails principal;

	/** Mission assigned to the user. */
	private String mission;

	/** Flag to indicate if this is a new login. */
	private boolean isNewLogin = true;

	/** Flag to indicate if the user is authenticated. */
	private boolean isAuthenticated;

	/** List of roles assigned to the user. */
	private List<String> userRoles;

	/** Cache to hold data during the session. */
	private DataCache dataCache = new DataCache();

	/**
	 * Returns the data cache.
	 *
	 * @return the dataCache
	 */
	public DataCache getDataCache() {
		return dataCache;
	}

	/**
	 * Sets the data cache.
	 *
	 * @param dataCache the dataCache to set
	 */
	public void setDataCache(DataCache dataCache) {
		this.dataCache = dataCache;
	}

	/**
	 * Returns the list of user roles.
	 *
	 * @return the userRoles
	 */
	public List<String> getUserRoles() {
		return userRoles;
	}

	/**
	 * Sets the list of user roles.
	 *
	 * @param userRoles the userRoles to set
	 */
	public void setUserRoles(List<String> userRoles) {
		this.userRoles = userRoles;
	}

	/**
	 * Returns whether it's a new login or not.
	 *
	 * @return the isNewLogin
	 */
	public boolean isNewLogin() {
		return isNewLogin;
	}

	/**
	 * Sets the new login flag.
	 *
	 * @param isNewLogin true if the login is new
	 */
	public void setNewLogin(boolean isNewLogin) {
		this.isNewLogin = isNewLogin;
	}

	/**
	 * Returns the username of the principal.
	 */
	@Override
	public String getName() {
		return principal.getUsername();
	}

	/**
	 * Return the password of the principal
	 *
	 * @return The password of the principal
	 */
	public String getPassword() {
		return principal.getPassword();
	}

	/**
	 * Returns the combined prosEO user name, which is a concatenation of mission name and username.
	 *
	 * @return The prosEO user name
	 */
	public String getProseoName() {
		return this.getMission() + "-" + this.getName();
	}

	/**
	 * Returns the granted authorities, which are roles or permissions for the user.
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		// must not be null
		return new ArrayList<>();
	}

	/**
	 * Returns the credentials, usually the password.
	 */
	@Override
	public Object getCredentials() {
		return credentials;
	}

	/**
	 * Sets the credentials.
	 *
	 * @param credentials the user credentials
	 */
	public void setCredentials(Object credentials) {
		this.credentials = credentials;
	}

	/**
	 * Returns additional details, in this case, the mission.
	 */
	@Override
	public Object getDetails() {
		return mission;
	}

	/**
	 * Sets the mission details.
	 *
	 * @param mission the mission to which the user is logged in
	 */
	public void setDetails(String mission) {
		this.mission = mission;
	}

	/**
	 * Return the mission of the user
	 *
	 * @return The mission to which the user is logged in
	 */
	public String getMission() {
		return mission;
	}

	/**
	 * Returns the principal, which represents the user's details.
	 */
	@Override
	public Object getPrincipal() {
		return principal;
	}

	/**
	 * Sets the principal, which represents the user's details.
	 *
	 * @param principal the user details
	 */
	public void setPrincipal(UserDetails principal) {
		this.principal = principal;
	}

	/**
	 * Returns whether the user is authenticated.
	 */
	@Override
	public boolean isAuthenticated() {
		return isAuthenticated;
	}

	/**
	 * Sets the authenticated flag.
	 *
	 * @param isAuthenticated true if the user is authenticated
	 */
	@Override
	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
		this.isAuthenticated = isAuthenticated;
	}

}