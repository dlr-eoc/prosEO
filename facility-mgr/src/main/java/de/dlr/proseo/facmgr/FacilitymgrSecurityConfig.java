/**
 * FacilitymgrSecurityConfig.java
 * 
 * (c) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.facmgr;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for prosEO Facility Manager module
 * 
 * @author Ranjitha Vignesh
 */
@Configuration
@EnableWebSecurity
public class FacilitymgrSecurityConfig extends WebSecurityConfigurerAdapter {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FacilitymgrSecurityConfig.class);
	
	/**
	 * Set the Facility Manager security options
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
	 * Initialize the users, passwords and roles for the Facility Manager from the prosEO database
	 * 
	 * @param builder to manage authentications
	 * @param dataSource the data source configured for the Facility Manager
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

	/**
	 * Provides the default user details service for prosEO (based on the standard data model for users and groups)
	 * 
	 * @return a JdbcDaoImpl object
	 */
	@Bean
	public UserDetailsService userDetailsService() {
		logger.info("Initializing user details service from datasource " + dataSource);

		JdbcDaoImpl jdbcDaoImpl = new JdbcDaoImpl();
		jdbcDaoImpl.setDataSource(dataSource);
		jdbcDaoImpl.setEnableGroups(true);
		return jdbcDaoImpl;
	}
}
