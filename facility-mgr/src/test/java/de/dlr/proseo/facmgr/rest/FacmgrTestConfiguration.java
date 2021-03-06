package de.dlr.proseo.facmgr.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import de.dlr.proseo.facmgr.FacilitymgrConfiguration;

@Configuration
@Import(FacilitymgrConfiguration.class)
@ConfigurationProperties(prefix="proseo")

public class FacmgrTestConfiguration {

	
	@Value("${proseo.user.name}")
	private String userName;
	
	@Value("${proseo.user.password}")
	private String userPassword;
	
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	
	/**
	 * @return the userPassword
	 */
	public String getUserPassword() {
		return userPassword;
	}
	
	/**
	 * @param userPassword the userPassword to set
	 */
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}



}
