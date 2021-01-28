/**
 * 
 */
package de.dlr.proseo.ui.gui;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author david
 *
 */
public class GUIAuthenticationToken implements Authentication {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8032965813126496854L;
	
	private Object credentials;
	private UserDetails principal;
	private String mission;
	private boolean isNewLogin = true;
	private boolean isAuthenticated;


	/**
	 * @return the isNewLogin
	 */
	public boolean isNewLogin() {
		return isNewLogin;
	}

	/**
	 * @param isNewLogin the isNewLogin to set
	 */
	public void setNewLogin(boolean isNewLogin) {
		this.isNewLogin = isNewLogin;
	}
	
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
	 * Return the complete prosEO user name (concatenate mission name and user name 
	 * 
	 * @return The prosEO user name
	 */
	public String getProseoName() {
		return this.getMission() + "-" + this.getName();
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getCredentials() {
		return credentials;
	}
	public void setCredentials(Object credentials) {
		this.credentials = credentials;
	}

	@Override
	public Object getDetails() {
		return mission;
	}
	
	public void setDetails(String mission) {
		this.mission = mission;
	}
	
	/**
	 * Return the mission of the user
	 * 
	 * @return The mission
	 */
	public String getMission() {
		return mission;
	}

	@Override
	public Object getPrincipal() {
		return principal;
	}
	public void setPrincipal(UserDetails principal) {
		this.principal = principal;
	}

	@Override
	public boolean isAuthenticated() {
		return isAuthenticated;
	}

	@Override
	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
		this.isAuthenticated = isAuthenticated;

	}

}
