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
	private boolean isAuthenticated;

	@Override
	public String getName() {
		return principal.getUsername();
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
