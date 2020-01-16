/**
 * ProductionInterfaceSecurityConfig.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Security configuration for prosEO Production Interface Delivery Point (PRIP) API
 * 
 * @author Dr. Thomas Bassler
 */
@Configuration
@EnableWebSecurity
public class ProductionInterfaceSecurityConfig extends WebSecurityConfigurerAdapter {

	/**
	 * Set the PRIP API security options (actually the API is open, the security checks are done by the called services)
	 * 
	 * @param http the HTTP security object
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic()
				.and()
			.authorizeRequests()
				.anyRequest().permitAll()
				.and()
			.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

}
