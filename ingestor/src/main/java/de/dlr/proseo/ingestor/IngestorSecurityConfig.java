/**
 * IngestorSecurityConfig.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import de.dlr.proseo.ingestor.rest.ProductControllerImpl;

/**
 * Security configuration for prosEO Ingestor module
 * 
 * @author Dr. Thomas Bassler
 */
@Configuration
@EnableWebSecurity
public class IngestorSecurityConfig extends WebSecurityConfigurerAdapter {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(IngestorSecurityConfig.class);
	
	/**
	 * Set the Ingestor security options
	 * 
	 * @param http the HTTP security object
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic()
				.and()
			.authorizeRequests()
				.anyRequest().authenticated()
				.and()
			.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the Ingestor from the prosEO database
	 * 
	 * @param builder to manage authentications
	 * @param dataSource the data source configured for the Ingestor
	 * @throws Exception if anything goes wrong with JDBC authentication
	 */
	@Autowired
	public void initialize(AuthenticationManagerBuilder builder, DataSource dataSource) throws Exception {
		logger.info("Initializing authentication from datasource " + dataSource);

		builder.jdbcAuthentication()
			.dataSource(dataSource);
		
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
	}

}
