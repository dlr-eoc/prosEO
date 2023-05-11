/**
 * SecurityService.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import de.dlr.proseo.model.enums.UserRole;

/**
 * Utility method for accessing Spring Security user data
 * 
 * @author Dr. Thomas Bassler
 */
@Service
public class SecurityService {

	/**
	 * Check, whether the logged in user has the given role
	 * 
	 * @param role the user role to check
	 * @return true, if the role is in the user's list of roles, false otherwise
	 */
	public boolean hasRole(UserRole role) {
		// Since successful authentication is required for accessing any method, we trust that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();  // Includes group authorities
		
		// Check role in authorities
		String roleString = role.asRoleString();
		for (GrantedAuthority authority: authorities) {
			if (roleString.equals(authority.getAuthority())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check, whether the logged in user is authorized for the given mission
	 * 
	 * @param missionCode the mission code to check against
	 * @return true, if the user is authorized for this mission, false otherwise
	 */
	public boolean isAuthorizedForMission(String missionCode) {
		// Since successful authentication is required for accessing any method, we trust that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		if (null == auth || null == auth.getName()) {
			return false;
		}
		
		return auth.getName().split("-")[0].equals(missionCode);
	}
	
	/**
	 * Gets the name of the logged in user
	 * 
	 * @return the user name
	 */
	public String getUser() {
		System.out.println("Current security context = " + SecurityContextHolder.getContext());
		// Since successful authentication is required for accessing any method, we trust that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth.getName();
	}
	
	/**
	 * Gets the code of the logged in mission
	 * 
	 * @return the mission code
	 */
	public String getMission() {
		// Since successful authentication is required for accessing any method, we trust that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		if (null == auth || null == auth.getName()) {
			return null;
		}
		
		return auth.getName().split("-")[0];
	}
}
