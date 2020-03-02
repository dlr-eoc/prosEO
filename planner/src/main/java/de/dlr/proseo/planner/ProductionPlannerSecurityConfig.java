/**
 * ProductionPlannerSecurityConfig.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for prosEO Planner module
 * 
 * @author Ernst Melchinger
 */
@Configuration
@EnableWebSecurity
public class ProductionPlannerSecurityConfig extends WebSecurityConfigurerAdapter {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductionPlannerSecurityConfig.class);
	
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
			// .regexMatchers("(?i).*/processingfacilities/[^/]+/finish/.*").permitAll()
			.anyRequest().authenticated()
			.and()
			.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the ProductClassManager from the prosEO database
	 * 
	 * @param builder to manage authentications
	 * @param dataSource the data source configured for the ProductClassManager
	 * @throws Exception if anything goes wrong with JDBC authentication
	 */
	@Autowired
	public void initialize(AuthenticationManagerBuilder builder, DataSource dataSource) throws Exception {
		logger.info(Messages.PLANNER_AUTH_DATASOURCE.formatWithPrefix(dataSource.toString()));

		builder.jdbcAuthentication()
			.dataSource(dataSource);
		
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}

}
